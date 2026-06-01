package br.com.catech.fire_shield_localidades_ms.entity;

import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.SeveridadeOcorrencia;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.StatusEnriquecimento;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcorrenciaTest {

    private Ocorrencia ocorrenciaValida() {
        return Ocorrencia.builder()
                .latitude(-23.5505)
                .longitude(-46.6333)
                .severidade(SeveridadeOcorrencia.ALTA)
                .horarioDeteccao(Instant.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Construção e validações iniciais
    // -------------------------------------------------------------------------

    @Test
    void deveCriarOcorrenciaComStatusPendenteEZeroTentativas() {
        Ocorrencia o = ocorrenciaValida();

        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);
        assertThat(o.getTentativasEnriquecimento()).isZero();
    }

    @Test
    void deveRejeitarLatitudeInvalida() {
        assertThatThrownBy(() ->
                Ocorrencia.builder()
                        .latitude(91.0)
                        .longitude(-46.0)
                        .severidade(SeveridadeOcorrencia.ALTA)
                        .horarioDeteccao(Instant.now())
                        .build()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }

    @Test
    void deveRejeitarLongitudeInvalida() {
        assertThatThrownBy(() ->
                Ocorrencia.builder()
                        .latitude(-23.0)
                        .longitude(181.0)
                        .severidade(SeveridadeOcorrencia.ALTA)
                        .horarioDeteccao(Instant.now())
                        .build()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude");
    }

    @Test
    void deveRejeitarSeveridadeNula() {
        assertThatThrownBy(() ->
                Ocorrencia.builder()
                        .latitude(-23.0)
                        .longitude(-46.0)
                        .severidade(null)
                        .horarioDeteccao(Instant.now())
                        .build()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Severidade");
    }

    @Test
    void deveRejeitarHorarioDeteccaoNulo() {
        assertThatThrownBy(() ->
                Ocorrencia.builder()
                        .latitude(-23.0)
                        .longitude(-46.0)
                        .severidade(SeveridadeOcorrencia.BAIXA)
                        .horarioDeteccao(null)
                        .build()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Horario de deteccao");
    }

    // -------------------------------------------------------------------------
    // Ciclo de enriquecimento — caminho feliz
    // -------------------------------------------------------------------------

    @Test
    void deveAplicarEnderecoETransicionarParaEnriquecida() {
        Ocorrencia o = ocorrenciaValida();

        o.aplicarEndereco(123L, "Sé", "Sé, São Paulo, Brasil",
                "Glicério", "São Paulo", "São Paulo",
                "Região Sudeste", "01016-020", "Brasil", "br");

        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.ENRIQUECIDA);
        assertThat(o.getNome()).isEqualTo("Sé");
        assertThat(o.getCidade()).isEqualTo("São Paulo");
        assertThat(o.getCodigoPais()).isEqualTo("br");
    }

    // -------------------------------------------------------------------------
    // Ciclo de enriquecimento — falhas 5xx
    // -------------------------------------------------------------------------

    @Test
    void deveIncrementarTentativasEPermaneceremPendentaNasPrimeirasAoFalhar() {
        Ocorrencia o = ocorrenciaValida();

        o.registrarFalhaEnriquecimento();
        assertThat(o.getTentativasEnriquecimento()).isEqualTo(1);
        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);

        o.registrarFalhaEnriquecimento();
        assertThat(o.getTentativasEnriquecimento()).isEqualTo(2);
        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.PENDENTE);
    }

    @Test
    void deveMarcaServicoIndisponivelNaTerceiraTentativaFalhada() {
        Ocorrencia o = ocorrenciaValida();

        o.registrarFalhaEnriquecimento();
        o.registrarFalhaEnriquecimento();
        o.registrarFalhaEnriquecimento();

        assertThat(o.getTentativasEnriquecimento()).isEqualTo(3);
        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.SERVICO_INDISPONIVEL);
    }

    // -------------------------------------------------------------------------
    // Ciclo de enriquecimento — coordenadas inválidas (4xx)
    // -------------------------------------------------------------------------

    @Test
    void deveMarcaCoordenadasInvalidasQuandoChamadoExplicitamente() {
        Ocorrencia o = ocorrenciaValida();

        o.marcarCoordenadasInvalidas();

        assertThat(o.getStatusEnriquecimento()).isEqualTo(StatusEnriquecimento.COORDENADAS_INVALIDAS);
    }
}

