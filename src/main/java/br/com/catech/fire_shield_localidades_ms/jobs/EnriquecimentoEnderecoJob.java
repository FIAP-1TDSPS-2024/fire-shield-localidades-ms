package br.com.catech.fire_shield_localidades_ms.jobs;

import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.StatusEnriquecimento;
import br.com.catech.fire_shield_localidades_ms.repository.OcorrenciaRepository;
import br.com.catech.fire_shield_localidades_ms.service.OcorrenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job agendado que tenta enriquecer o endereço das ocorrências pendentes.
 *
 * Estratégia:
 *  - Roda a cada 5 minutos.
 *  - Busca todas as ocorrências com status PENDENTE (até 2 falhas anteriores).
 *  - Para cada uma, realiza UMA chamada à API de localidade via OcorrenciaService.tentarEnriquecer().
 *  - O próprio método do serviço aplica as regras de HTTP:
 *      · 2xx → ENRIQUECIDA (encerra tentativas)
 *      · 4xx → COORDENADAS_INVALIDAS (encerra tentativas)
 *      · 5xx / rede → incrementa contador; na 3ª falha → SERVICO_INDISPONIVEL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnriquecimentoEnderecoJob {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final OcorrenciaService ocorrenciaService;

    @Scheduled(fixedDelay = 60 * 1000) // 5 minutos em ms
    public void executar() {
        List<Ocorrencia> pendentes = ocorrenciaRepository
                .findByStatusEnriquecimento(StatusEnriquecimento.PENDENTE);

        if (pendentes.isEmpty()) {
            log.debug("[JOB] Nenhuma ocorrencia pendente de enriquecimento.");
            return;
        }

        log.info("[JOB] Iniciando enriquecimento de {} ocorrencia(s) pendente(s).", pendentes.size());

        for (Ocorrencia ocorrencia : pendentes) {
            try {
                ocorrenciaService.tentarEnriquecer(ocorrencia);
            } catch (Exception e) {
                // Exceções (ex.: CoordenadasForaDoBrasilException) já foram logadas e persistidas no service.
                // Continuamos para a próxima ocorrência sem interromper o job.
                log.debug("[JOB] Ocorrencia uuid={} encerrada sem enriquecimento: {}",
                        ocorrencia.getUuid(), e.getMessage());
            }
        }

        log.info("[JOB] Ciclo de enriquecimento concluido.");
    }
}
