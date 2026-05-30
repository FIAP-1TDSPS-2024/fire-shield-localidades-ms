package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;

import java.util.List;

public interface OutBoxService {
    List<OutboxEvent> findPedentesToProcess();

    OutboxEvent save(OutboxEvent outboxEvent);
}
