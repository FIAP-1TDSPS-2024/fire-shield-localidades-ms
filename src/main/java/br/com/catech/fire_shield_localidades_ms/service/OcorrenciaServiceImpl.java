package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.dto.EnderecoDto;
import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.exception.CoordenadasForaDoBrasilException;
import br.com.catech.fire_shield_localidades_ms.external_interface.feign.LocalidadeAppClient;
import br.com.catech.fire_shield_localidades_ms.repository.OcorrenciaRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcorrenciaServiceImpl implements OcorrenciaService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final LocalidadeAppClient localidadeAppClient;

    @Override
    public Ocorrencia registrar(OcorrenciaRequest request) {
        Ocorrencia ocorrencia = Ocorrencia.builder()
                .latitude(request.latitude())
                .longitude(request.longitude())
                .severidade(request.severidade())
                .horarioDeteccao(request.horarioDeteccao())
                .build();

        Ocorrencia salva = ocorrenciaRepository.save(ocorrencia);
        publicar(salva);
        return salva;
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

    /**
     * Realiza UMA chamada à API de localidade para enriquecer o endereço da ocorrência.
     * Regras de HTTP:
     *  - 2xx ? aplica endereço e marca ENRIQUECIDA
     *  - 4xx ? coordenadas inválidas/fora do Brasil, marca COORDENADAS_INVALIDAS (sem novas tentativas)
     *  - 5xx / erro de rede ? incrementa contador; após 3 falhas marca SERVICO_INDISPONIVEL
     *
     * O job é responsável por chamar este método repetidamente a cada 5 minutos,
     * apenas para ocorrências com status PENDENTE.
     */
    @Override
    public void tentarEnriquecer(Ocorrencia ocorrencia) {
        log.info("Tentando enriquecer ocorrencia uuid={} (tentativa {}/3)",
                ocorrencia.getUuid(), ocorrencia.getTentativasEnriquecimento() + 1);
        try {
            EnderecoDto dto = localidadeAppClient.buscarEnderecoPorCoordenadas(
                    ocorrencia.getLatitude(),
                    ocorrencia.getLongitude(),
                    "json");

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

        } catch (FeignException.FeignClientException ex) {
            int status = ex.status();
            if (status >= 400 && status < 500) {
                log.warn("Coordenadas invalidas ou fora do Brasil (HTTP {}) para uuid={}. Descartando enriquecimento.",
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


    /** Publica a ocorrencia. Implementacao pendente (padrao Outbox). */
    private void publicar(Ocorrencia ocorrencia) {
        // TODO: implementar padrao Outbox
        log.info("[PUBLICACAO PENDENTE] uuid={}", ocorrencia.getUuid());
    }
}
