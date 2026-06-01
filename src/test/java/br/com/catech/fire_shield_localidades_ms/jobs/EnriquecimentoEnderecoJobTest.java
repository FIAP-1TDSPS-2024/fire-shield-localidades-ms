package br.com.catech.fire_shield_localidades_ms.jobs;

import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.SeveridadeOcorrencia;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.StatusEnriquecimento;
import br.com.catech.fire_shield_localidades_ms.repository.OcorrenciaRepository;
import br.com.catech.fire_shield_localidades_ms.service.OcorrenciaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnriquecimentoEnderecoJobTest {

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;
    @Mock
    private OcorrenciaService ocorrenciaService;

    @InjectMocks
    private EnriquecimentoEnderecoJob job;

    private Ocorrencia ocorrenciaPendente() {
        return Ocorrencia.builder()
                .latitude(-23.5)
                .longitude(-46.6)
                .severidade(SeveridadeOcorrencia.ALTA)
                .horarioDeteccao(Instant.now())
                .build();
    }

    @Test
    void deveEnriquecerTodasAsOcorrenciasPendentes() {
        Ocorrencia o1 = ocorrenciaPendente();
        Ocorrencia o2 = ocorrenciaPendente();
        when(ocorrenciaRepository.findByStatusEnriquecimento(StatusEnriquecimento.PENDENTE))
                .thenReturn(List.of(o1, o2));

        job.executar();

        verify(ocorrenciaService, times(2)).tentarEnriquecer(any());
    }

    @Test
    void naoDeveEnriquecerQuandoNaoHaPendentes() {
        when(ocorrenciaRepository.findByStatusEnriquecimento(StatusEnriquecimento.PENDENTE))
                .thenReturn(List.of());

        job.executar();

        verify(ocorrenciaService, never()).tentarEnriquecer(any());
    }

    @Test
    void deveContinuarEnriquecimentoMesmoQuandoUmaOcorrenciaLancaExcecao() {
        Ocorrencia o1 = ocorrenciaPendente();
        Ocorrencia o2 = ocorrenciaPendente();
        when(ocorrenciaRepository.findByStatusEnriquecimento(StatusEnriquecimento.PENDENTE))
                .thenReturn(List.of(o1, o2));
        doThrow(new RuntimeException("Falha simulada")).when(ocorrenciaService).tentarEnriquecer(o1);

        job.executar(); // não deve propagar a exceção

        verify(ocorrenciaService).tentarEnriquecer(o1);
        verify(ocorrenciaService).tentarEnriquecer(o2);
    }
}

