package br.com.catech.fire_shield_localidades_ms.integration;

import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.enums.SeveridadeOcorrenciaEnum;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.StatusEnriquecimento;
import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import br.com.catech.fire_shield_localidades_ms.repository.OcorrenciaRepository;
import br.com.catech.fire_shield_localidades_ms.repository.OutboxEventRepository;
import br.com.catech.fire_shield_localidades_ms.service.OcorrenciaService;
import br.com.catech.fire_shield_localidades_ms.service.OutBoxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração usando H2 em memória (application.properties de test).
 * Valida o ciclo completo: persistência, leitura, e outbox com repositórios reais.
 */
@SpringBootTest
@Transactional
class OcorrenciaIntegrationTest {

    @Autowired
    private OcorrenciaService ocorrenciaService;

    @Autowired
    private OutBoxService outBoxService;

    @Autowired
    private OcorrenciaRepository ocorrenciaRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private OcorrenciaRequest requestValido() {
        return new OcorrenciaRequest(
                -23.5505, -46.6333,
                SeveridadeOcorrenciaEnum.ALTA,
                Instant.now()
        );
    }

    // -------------------------------------------------------------------------
    // Persistência de Ocorrência
    // -------------------------------------------------------------------------

    @Test
    void deveRegistrarEPersistirOcorrenciaNoBanco() {
        Ocorrencia salva = ocorrenciaService.registrar(requestValido());

        assertThat(salva.getUuid()).isNotNull();
        assertThat(salva.getHorarioSalvamento()).isNotNull();
        assertThat(salva.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);

        Ocorrencia encontrada = ocorrenciaService.buscarPorId(salva.getUuid());
        assertThat(encontrada.getUuid()).isEqualTo(salva.getUuid());
    }

    @Test
    void deveListarTodasAsOcorrencias() {
        ocorrenciaService.registrar(requestValido());
        ocorrenciaService.registrar(new OcorrenciaRequest(
                -3.717, -38.543, SeveridadeOcorrenciaEnum.CRITICO, Instant.now()));

        List<Ocorrencia> todas = ocorrenciaService.listarTodas();

        assertThat(todas).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deveLancarExcecaoAoBuscarIdInexistente() {
        assertThatThrownBy(() -> ocorrenciaService.buscarPorId(java.util.UUID.randomUUID()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // OcorrenciaRepository — query de pendentes
    // -------------------------------------------------------------------------

    @Test
    void deveBuscarApenasOcorrenciasPendentes() {
        ocorrenciaService.registrar(requestValido()); // PENDENTE

        List<Ocorrencia> pendentes = ocorrenciaRepository
                .findByStatusEnriquecimento(StatusEnriquecimento.PENDENTE);

        assertThat(pendentes).isNotEmpty();
        assertThat(pendentes).allMatch(
                o -> o.getStatusEnriquecimento() == StatusEnriquecimento.PENDENTE);
    }

    // -------------------------------------------------------------------------
    // OutboxEvent — persistência e leitura de pendentes
    // -------------------------------------------------------------------------

    @Test
    void deveSalvarERecuperarOutboxEventPendente() {
        OutboxEvent evento = new OutboxEvent(
                "ref-uuid-001",
                "OCORRENCIA",
                "loc.ocorrencia.queue",
                "{\"id\":\"ref-uuid-001\"}"
        );

        outBoxService.save(evento);

        List<OutboxEvent> pendentes = outBoxService.findPedentesToProcess();
        assertThat(pendentes).isNotEmpty();
        assertThat(pendentes).anyMatch(e -> "ref-uuid-001".equals(e.getReferenceId()));
    }

    @Test
    void deveMarcarOutboxEventComoEnviado() {
        OutboxEvent evento = new OutboxEvent(
                "ref-uuid-002",
                "OCORRENCIA",
                "loc.ocorrencia.queue",
                "{\"id\":\"ref-uuid-002\"}"
        );
        outBoxService.save(evento);

        evento.marcarComoEnviado();
        outBoxService.save(evento);

        assertThat(evento.getStatus()).isEqualTo(OutboxEvent.Status.ENVIADO);
        assertThat(evento.getEnviadoEm()).isNotNull();
    }
}

