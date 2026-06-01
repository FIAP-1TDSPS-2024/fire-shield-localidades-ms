package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import br.com.catech.fire_shield_localidades_ms.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutBoxServiceImplTest {

    @Mock
    private OutboxEventRepository repository;

    @InjectMocks
    private OutBoxServiceImpl service;

    private OutboxEvent eventoNovo() {
        return new OutboxEvent("ref-123", "OCORRENCIA", "loc.ocorrencia.queue", "{\"id\":\"ref-123\"}");
    }

    @Test
    void deveSalvarEventoERetornarPersistido() {
        OutboxEvent evento = eventoNovo();
        when(repository.save(evento)).thenReturn(evento);

        OutboxEvent result = service.save(evento);

        assertThat(result).isSameAs(evento);
        verify(repository).save(evento);
    }

    @Test
    void deveRetornarListaDePendentes() {
        OutboxEvent evento = eventoNovo();
        when(repository.findDistinctPendentes()).thenReturn(List.of(evento));

        List<OutboxEvent> result = service.findPedentesToProcess();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OutboxEvent.Status.PENDENTE);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaPendentes() {
        when(repository.findDistinctPendentes()).thenReturn(List.of());

        List<OutboxEvent> result = service.findPedentesToProcess();

        assertThat(result).isEmpty();
    }
}

