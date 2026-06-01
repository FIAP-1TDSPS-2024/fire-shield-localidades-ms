package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.dto.EnderecoDto;
import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaOut;
import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import br.com.catech.fire_shield_localidades_ms.exception.CoordenadasForaDoBrasilException;
import br.com.catech.fire_shield_localidades_ms.external_interface.feign.LocalidadeAppClient;
import br.com.catech.fire_shield_localidades_ms.repository.OcorrenciaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcorrenciaServiceImpl implements OcorrenciaService {

    private static final String OUTBOX_TYPE = "OCORRENCIA";

    @Value("${RABBITMQ_QUEUE_DESTINY}")
    private String queue;

    private final OcorrenciaRepository ocorrenciaRepository;
    private final LocalidadeAppClient localidadeAppClient;
    private final OutBoxService outBoxService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Ocorrencia registrar(OcorrenciaRequest request) {
        Ocorrencia ocorrencia = Ocorrencia.builder()
                .latitude(request.latitude())
                .longitude(request.longitude())
                .severidade(request.severidade())
                .horarioDeteccao(request.horarioDeteccao())
                .build();
        return ocorrenciaRepository.save(ocorrencia);
    }

    @Override
    public Ocorrencia buscarPorId(UUID id) {
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Ocorrencia nao encontrada: " + id));
    }

    @Override
    public List<Ocorrencia> listarTodas() {
        return ocorrenciaRepository.findAll();
    }

    @Override
    @Transactional
    public void tentarEnriquecer(Ocorrencia ocorrencia) {
        log.info("Tentando enriquecer ocorrencia uuid={} (tentativa {}/3)",
                ocorrencia.getUuid(), ocorrencia.getTentativasEnriquecimento() + 1);
        try {
            EnderecoDto dto = localidadeAppClient.buscarEnderecoPorCoordenadas(
                    ocorrencia.getLatitude(), ocorrencia.getLongitude(), "json");

            EnderecoDto.AddressDto addr = dto.address();
            ocorrencia.aplicarEndereco(
                    dto.placeId(),
                    dto.name(),
                    dto.displayName(),
                    addr != null ? addr.suburb() : null,
                    addr != null ? addr.city() : null,
                    addr != null ? addr.state() : null,
                    addr != null ? addr.region() : null,
                    addr != null ? addr.postcode() : null,
                    addr != null ? addr.country() : null,
                    addr != null ? addr.countryCode() : null
            );
            ocorrenciaRepository.save(ocorrencia);
            log.info("Ocorrencia uuid={} enriquecida com sucesso.", ocorrencia.getUuid());
            criarOutboxEvent(ocorrencia); // terminal: ENRIQUECIDA

        } catch (FeignException.FeignClientException ex) {
            int status = ex.status();
            if (status >= 400 && status < 500) {
                log.warn("Coordenadas invalidas ou fora do Brasil (HTTP {}) para uuid={}.",
                        status, ocorrencia.getUuid());
                ocorrencia.marcarCoordenadasInvalidas();
                ocorrenciaRepository.save(ocorrencia);
                throw new CoordenadasForaDoBrasilException(
                        "uuid=" + ocorrencia.getUuid() + " HTTP " + status);
            }
            log.warn("Erro 5xx (HTTP {}) ao enriquecer uuid={}", status, ocorrencia.getUuid());
            ocorrencia.registrarFalhaEnriquecimento();
            ocorrenciaRepository.save(ocorrencia);

        } catch (FeignException ex) {
            log.warn("Erro de comunicacao ao enriquecer uuid={}: {}", ocorrencia.getUuid(), ex.getMessage());
            ocorrencia.registrarFalhaEnriquecimento();
            ocorrenciaRepository.save(ocorrencia);
        }
    }

    private void criarOutboxEvent(Ocorrencia ocorrencia) {
        try {
            String payload = objectMapper.writeValueAsString(toOut(ocorrencia));
            outBoxService.save(new OutboxEvent(
                    ocorrencia.getUuid().toString(),
                    OUTBOX_TYPE,
                    queue,
                    payload
            ));
            log.info("[OUTBOX] Evento registrado para uuid={}, status={}",
                    ocorrencia.getUuid(), ocorrencia.getStatusEnriquecimento());
        } catch (JsonProcessingException e) {
            log.error("[OUTBOX] Falha ao serializar ocorrencia uuid={}: {}",
                    ocorrencia.getUuid(), e.getMessage(), e);
        }
    }

    private OcorrenciaOut toOut(Ocorrencia ocorrencia) {
        return new OcorrenciaOut(
                ocorrencia.getUuid(),
                ocorrencia.getLatitude(),
                ocorrencia.getLongitude(),
                ocorrencia.getSeveridade(),
                ocorrencia.getHorarioDeteccao(),
                ocorrencia.getNome(),
                ocorrencia.getBairro(),
                ocorrencia.getCidade(),
                ocorrencia.getEstado(),
                ocorrencia.getCep()
        );
    }
}
