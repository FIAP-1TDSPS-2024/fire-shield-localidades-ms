package br.com.catech.fire_shield_localidades_ms.entity;

import br.com.catech.fire_shield_localidades_ms.enums.SeveridadeOcorrenciaEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loc_ocorrencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID uuid;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeveridadeOcorrenciaEnum severidade;

    @Column(nullable = false)
    private Instant horarioDeteccao;

    @Column(nullable = false, updatable = false)
    private Instant horarioSalvamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusEnriquecimento statusEnriquecimento = StatusEnriquecimento.PENDENTE;

    @Column(nullable = false)
    private int tentativasEnriquecimento = 0;

    private Long placeId;

    private String nome;

    @Column(length = 500)
    private String displayName;

    private String bairro;

    private String cidade;

    private String estado;

    private String regiao;

    private String cep;

    private String pais;

    @Column(length = 2)
    private String codigoPais;

    /** Cria uma ocorrência com apenas os dados obrigatórios. O endereço é preenchido posteriormente pelo job. */
    @Builder
    public Ocorrencia(double latitude,
                      double longitude,
                      SeveridadeOcorrenciaEnum severidade,
                      Instant horarioDeteccao) {

        validarCoordenadas(latitude, longitude);
        if (severidade == null) {
            throw new IllegalArgumentException("Severidade da ocorrencia e obrigatoria");
        }
        if (horarioDeteccao == null) {
            throw new IllegalArgumentException("Horario de deteccao e obrigatorio");
        }

        this.latitude = latitude;
        this.longitude = longitude;
        this.severidade = severidade;
        this.horarioDeteccao = horarioDeteccao;
        this.statusEnriquecimento = StatusEnriquecimento.PENDENTE;
        this.tentativasEnriquecimento = 0;
    }

    // -------------------------------------------------------------------------
    // Métodos de domínio para o ciclo de enriquecimento de endereço
    // -------------------------------------------------------------------------

    /** Aplica os dados de endereço retornados pela API e marca como enriquecida. */
    public void aplicarEndereco(Long placeId,
                                String nome,
                                String displayName,
                                String bairro,
                                String cidade,
                                String estado,
                                String regiao,
                                String cep,
                                String pais,
                                String codigoPais) {
        this.placeId = placeId;
        this.nome = nome;
        this.displayName = displayName;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.regiao = regiao;
        this.cep = cep;
        this.pais = pais;
        this.codigoPais = codigoPais;
        this.statusEnriquecimento = StatusEnriquecimento.ENRIQUECIDA;
    }

    /** Registra uma tentativa malsucedida por erro 5xx. Após 3 tentativas marca como serviço indisponível. */
    public void registrarFalhaEnriquecimento() {
        this.tentativasEnriquecimento++;
        if (this.tentativasEnriquecimento >= 3) {
            this.statusEnriquecimento = StatusEnriquecimento.SERVICO_INDISPONIVEL;
        }
    }

    /** Marca a ocorrência como inválida (coordenadas recusadas pela API com 4xx). Não haverá novas tentativas. */
    public void marcarCoordenadasInvalidas() {
        this.statusEnriquecimento = StatusEnriquecimento.COORDENADAS_INVALIDAS;
    }

    @PrePersist
    void registrarHorarioSalvamento() {
        this.horarioSalvamento = Instant.now();
    }

    private void validarCoordenadas(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude deve estar entre -90 e 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude deve estar entre -180 e 180");
        }
    }

    public enum StatusEnriquecimento {
        /** Aguardando chamada à API de localidade. */
        PENDENTE,
        /** Endereço aplicado com sucesso. */
        ENRIQUECIDA,
        /** API retornou 4xx — coordenadas inválidas ou fora do Brasil. Sem novas tentativas. */
        COORDENADAS_INVALIDAS,
        /** API retornou 5xx nas 3 tentativas — serviço indisponível no momento. */
        SERVICO_INDISPONIVEL
    }
}
