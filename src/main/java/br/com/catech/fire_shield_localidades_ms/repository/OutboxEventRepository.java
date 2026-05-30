package br.com.catech.fire_shield_localidades_ms.repository;

import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query(nativeQuery = true, value = "select distinct o.* from outbox_event o where o.status = 'PENDENTE' FETCH FIRST 10 ROWS ONLY")
    List<OutboxEvent> findDistinctPendentes();
}
