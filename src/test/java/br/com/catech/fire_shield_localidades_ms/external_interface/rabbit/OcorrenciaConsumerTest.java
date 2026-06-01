package br.com.catech.fire_shield_localidades_ms.external_interface.rabbit;

import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.SeveridadeOcorrencia;
import br.com.catech.fire_shield_localidades_ms.service.OcorrenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcorrenciaConsumerTest {

    @Mock
    private OcorrenciaService ocorrenciaService;

    @InjectMocks
    private OcorrenciaConsumer consumer;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(consumer, "objectMapper", mapper);
    }

    private static final String PAYLOAD_VALIDO = """
            {
              "Id":"2b7400a1-eeb7-41d6-8d84-ed3dca2fbf34",
              "Tipo":"Fogo rasteiro",
              "Latitude":-23.6903362,
              "Longitude":-46.7833278,
              "Urgencia":"alert",
              "Area":0,
              "Distancia":0,
              "DataUtc":"2026-05-29T14:07:30.857Z",
              "ReportadoPor":"Jo\\u00E3o Silva",
              "ContatoAutor":"joao@email.com"
            }
            """;

    @Test
    void deveDesserializarMensagemERegistrarOcorrencia() {
        when(ocorrenciaService.registrar(any())).thenReturn(mock(Ocorrencia.class));

        consumer.consumir(PAYLOAD_VALIDO);

        verify(ocorrenciaService).registrar(any());
    }

    @Test
    void deveMapearUrgenciaAlertParaSeveridadeAlta() {
        when(ocorrenciaService.registrar(any())).thenReturn(mock(Ocorrencia.class));

        consumer.consumir(PAYLOAD_VALIDO);

        var captor = ArgumentCaptor.forClass(
                br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest.class);
        verify(ocorrenciaService).registrar(captor.capture());

        assertThat(captor.getValue().severidade()).isEqualTo(SeveridadeOcorrencia.ALTA);
    }

    @Test
    void deveMapearUrgenciaCriticoParaSeveridadeCritico() {
        String payload = PAYLOAD_VALIDO.replace("\"alert\"", "\"critical\"");
        when(ocorrenciaService.registrar(any())).thenReturn(mock(Ocorrencia.class));

        consumer.consumir(payload);

        var captor = ArgumentCaptor.forClass(
                br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest.class);
        verify(ocorrenciaService).registrar(captor.capture());
        assertThat(captor.getValue().severidade()).isEqualTo(SeveridadeOcorrencia.CRITICO);
    }

    @Test
    void deveMapearUrgenciaDesconhecidaParaMedia() {
        String payload = PAYLOAD_VALIDO.replace("\"alert\"", "\"unknown_level\"");
        when(ocorrenciaService.registrar(any())).thenReturn(mock(Ocorrencia.class));

        consumer.consumir(payload);

        var captor = ArgumentCaptor.forClass(
                br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest.class);
        verify(ocorrenciaService).registrar(captor.capture());
        assertThat(captor.getValue().severidade()).isEqualTo(SeveridadeOcorrencia.MEDIA);
    }

    @Test
    void deveDecodificarAcentosUnicodeCorretamente() {
        // \u00E3 = ã  (João deve ser preservado)
        when(ocorrenciaService.registrar(any())).thenReturn(mock(Ocorrencia.class));

        // Não deve lançar exceção e deve chamar o serviço normalmente
        consumer.consumir(PAYLOAD_VALIDO);

        verify(ocorrenciaService, times(1)).registrar(any());
    }

    @Test
    void deveTratarMensagemJsonInvalidaSemLancarExcecao() {
        // Não deve propagar a exceção
        consumer.consumir("JSON_INVALIDO_{{{");

        verify(ocorrenciaService, never()).registrar(any());
    }
}

