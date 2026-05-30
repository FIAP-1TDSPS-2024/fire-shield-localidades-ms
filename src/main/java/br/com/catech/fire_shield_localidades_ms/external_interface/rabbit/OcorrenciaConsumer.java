package br.com.catech.fire_shield_localidades_ms.external_interface.rabbit;

import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaPayload;
import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.SeveridadeOcorrencia;
import br.com.catech.fire_shield_localidades_ms.service.OcorrenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcorrenciaConsumer {

    private final OcorrenciaService ocorrenciaService;
    private final ObjectMapper objectMapper;

    /**
     * Consome mensagens da fila de ocorrências.
     *
     * O Spring AMQP entrega a mensagem como String quando o content-type é text/plain ou
     * application/json. Usar String evita a necessidade de recodificar bytes manualmente,
     * e o ObjectMapper trata os escapes Unicode do JSON (ex.: \u00E3 → ã) corretamente.
     */
    @RabbitListener(queues = "${RABBITMQ_QUEUE_ORIGIN}")
    public void consumir(String mensagem) {
        log.info("[RABBITMQ] Mensagem recebida: {}", mensagem);
        try {
            OcorrenciaPayload payload = objectMapper.readValue(mensagem, OcorrenciaPayload.class);
            log.info("[RABBITMQ] Payload desserializado: id={}, tipo={}, reportadoPor={}",
                    payload.id(), payload.tipo(), payload.reportadoPor());

            OcorrenciaRequest request = new OcorrenciaRequest(
                    payload.latitude(),
                    payload.longitude(),
                    mapearUrgencia(payload.urgencia()),
                    payload.dataUtc()
            );

            ocorrenciaService.registrar(request);
            log.info("[RABBITMQ] Ocorrencia registrada com sucesso. id_origem={}", payload.id());

        } catch (Exception e) {
            log.error("[RABBITMQ] Erro ao processar mensagem: {}", e.getMessage(), e);
        }
    }

    /**
     * Mapeia o campo "Urgencia" do producer externo para o enum interno SeveridadeOcorrencia.
     */
    private SeveridadeOcorrencia mapearUrgencia(String urgencia) {
        if (urgencia == null) return SeveridadeOcorrencia.MEDIA;
        return switch (urgencia.toLowerCase()) {
            case "critical", "critico", "critica" -> SeveridadeOcorrencia.CRITICO;
            case "alert", "alta", "high"          -> SeveridadeOcorrencia.ALTA;
            case "low", "baixa"                   -> SeveridadeOcorrencia.BAIXA;
            default                               -> SeveridadeOcorrencia.MEDIA;
        };
    }
}

