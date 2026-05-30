package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import br.com.catech.fire_shield_localidades_ms.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutBoxServiceImpl implements OutBoxService {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public List<OutboxEvent> findPedentesToProcess() {
        List<OutboxEvent> pendentes = outboxEventRepository.findDistinctPendentes();
        log.debug("[OUTBOX] {} evento(s) pendente(s) encontrado(s).", pendentes.size());
        return pendentes;
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        OutboxEvent salvo = outboxEventRepository.save(outboxEvent);
        log.info("[OUTBOX] Evento salvo: id={}, type={}, destination={}, referenceId={}",
                salvo.getId(), salvo.getType(), salvo.getDestination(), salvo.getReferenceId());
        return salvo;
    }
}

