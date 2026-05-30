package br.com.catech.fire_shield_localidades_ms.jobs;

import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import br.com.catech.fire_shield_localidades_ms.service.OutBoxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.NestedRuntimeException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class OutBoxJob {

    private final OutBoxService outBoxService;
    private final RabbitTemplate rabbitTemplate;

    public OutBoxJob(OutBoxService outBoxService, RabbitTemplate rabbitTemplate) {
        this.outBoxService = outBoxService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedRate = 10000)
    public void pedidosPendentes() {
        final List<OutboxEvent> pedentesToProcess = this.outBoxService.findPedentesToProcess();
        for (OutboxEvent outboxEvent : pedentesToProcess) {
            try {
                publish(outboxEvent);
                log.info("Evento {} pendente processado com Sucesso", outboxEvent.getId());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    @Transactional
    void publish(OutboxEvent outboxEvent) throws Exception {

        try {
            outboxEvent.marcarComoEnviado();
            outBoxService.save(outboxEvent);

            rabbitTemplate.convertAndSend(
                    outboxEvent.getDestination(),
                    outboxEvent.getPayload()
            );

        } catch (NestedRuntimeException e) {
            throw new Exception("Falha ao processar o pedido: " + outboxEvent.getId(), e);
        }
    }
}
