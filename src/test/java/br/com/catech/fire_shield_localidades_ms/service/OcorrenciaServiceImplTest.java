package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.dto.EnderecoDto;
import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import br.com.catech.fire_shield_localidades_ms.enums.SeveridadeOcorrenciaEnum;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.StatusEnriquecimento;
import br.com.catech.fire_shield_localidades_ms.entity.OutboxEvent;
import br.com.catech.fire_shield_localidades_ms.exception.CoordenadasForaDoBrasilException;
import br.com.catech.fire_shield_localidades_ms.external_interface.feign.LocalidadeAppClient;
import br.com.catech.fire_shield_localidades_ms.repository.OcorrenciaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcorrenciaServiceImplTest {

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;
    @Mock
    private LocalidadeAppClient localidadeAppClient;
    @Mock
    private OutBoxService outBoxService;

    @InjectMocks
    private OcorrenciaServiceImpl service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "queue", "loc.ocorrencia.queue");
        // ObjectMapper real para serialização
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper()
                .findAndRegisterModules());
    }

    private Ocorrencia ocorrenciaPendente() {
        return Ocorrencia.builder()
                .latitude(-23.5505)
                .longitude(-46.6333)
                .severidade(SeveridadeOcorrenciaEnum.ALTA)
                .horarioDeteccao(Instant.now())
                .build();
    }

    private OcorrenciaRequest requestValido() {
        return new OcorrenciaRequest(-23.5505, -46.6333,
                SeveridadeOcorrenciaEnum.ALTA, Instant.now());
    }

    // -------------------------------------------------------------------------
    // registrar
    // -------------------------------------------------------------------------

    @Test
    void deveRegistrarOcorrenciaEPersistir() {
        when(ocorrenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Ocorrencia result = service.registrar(requestValido());

        assertThat(result.getLatitude()).isEqualTo(-23.5505);
        assertThat(result.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);
        verify(ocorrenciaRepository).save(any(Ocorrencia.class));
    }

    // -------------------------------------------------------------------------
    // buscarPorId
    // -------------------------------------------------------------------------

    @Test
    void deveLancarExcecaoQuandoOcorrenciaNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(ocorrenciaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void deveRetornarOcorrenciaExistente() {
        Ocorrencia o = ocorrenciaPendente();
        UUID id = UUID.randomUUID();
        when(ocorrenciaRepository.findById(id)).thenReturn(Optional.of(o));

        Ocorrencia result = service.buscarPorId(id);

        assertThat(result).isSameAs(o);
    }

    // -------------------------------------------------------------------------
    // listarTodas
    // -------------------------------------------------------------------------

    @Test
    void deveRetornarListaDeOcorrencias() {
        when(ocorrenciaRepository.findAll()).thenReturn(List.of(ocorrenciaPendente()));

        List<Ocorrencia> result = service.listarTodas();

        assertThat(result).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // tentarEnriquecer — sucesso 2xx
    // -------------------------------------------------------------------------

    @Test
    void deveEnriquecerOcorrenciaECriarOutboxEventQuandoApiRetorna2xx() {
        Ocorrencia o = ocorrenciaPendente();
        // uuid é gerado pelo JPA — em testes unitários precisamos setar manualmente
        ReflectionTestUtils.setField(o, "uuid", UUID.randomUUID());

        when(ocorrenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EnderecoDto.AddressDto addr = new EnderecoDto.AddressDto(
                "Rua X", null, "Bairro", "São Paulo", null, null,
                "SP", "BR-SP", "Sudeste", "01000-000", "Brasil", "br");
        EnderecoDto dto = new EnderecoDto(123L, "railway", "Estação", "Estação, SP", addr);

        when(localidadeAppClient.buscarEnderecoPorCoordenadas(anyDouble(), anyDouble(), anyString()))
                .thenReturn(dto);
        when(outBoxService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.tentarEnriquecer(o);

        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.ENRIQUECIDA);
        assertThat(o.getCidade()).isEqualTo("São Paulo");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outBoxService).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OCORRENCIA");
        assertThat(captor.getValue().getDestination()).isEqualTo("loc.ocorrencia.queue");
    }

    // -------------------------------------------------------------------------
    // tentarEnriquecer — erro 4xx (coordenadas inválidas)
    // -------------------------------------------------------------------------

    @Test
    void deveMarcaCoordenadasInvalidasQuandoApiRetorna4xxENaoGeraOutbox() {
        Ocorrencia o = ocorrenciaPendente();
        when(ocorrenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeignException.FeignClientException ex = mock(FeignException.FeignClientException.class);
        when(ex.status()).thenReturn(404);
        when(localidadeAppClient.buscarEnderecoPorCoordenadas(anyDouble(), anyDouble(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> service.tentarEnriquecer(o))
                .isInstanceOf(CoordenadasForaDoBrasilException.class);

        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.COORDENADAS_INVALIDAS);
        verify(outBoxService, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // tentarEnriquecer — erro 5xx (serviço indisponível — até 3 tentativas)
    // -------------------------------------------------------------------------

    @Test
    void deveIncrementarTentativasSemGerarOutboxQuandoApiRetorna5xx() {
        Ocorrencia o = ocorrenciaPendente();
        when(ocorrenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeignException.FeignClientException ex = mock(FeignException.FeignClientException.class);
        when(ex.status()).thenReturn(503);
        when(localidadeAppClient.buscarEnderecoPorCoordenadas(anyDouble(), anyDouble(), anyString()))
                .thenThrow(ex);

        // 1ª e 2ª tentativas — ainda PENDENTE
        service.tentarEnriquecer(o);
        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);
        assertThat(o.getTentativasEnriquecimento()).isEqualTo(1);

        service.tentarEnriquecer(o);
        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);
        assertThat(o.getTentativasEnriquecimento()).isEqualTo(2);

        // 3ª tentativa — SERVICO_INDISPONIVEL, mas sem OutboxEvent
        service.tentarEnriquecer(o);
        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.SERVICO_INDISPONIVEL);
        verify(outBoxService, never()).save(any());
    }
}

