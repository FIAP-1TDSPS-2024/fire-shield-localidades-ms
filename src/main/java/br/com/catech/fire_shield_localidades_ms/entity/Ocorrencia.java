package br.com.catech.fire_shield_localidades_ms.entity;

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
@Table(name = "ocorrencias")
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
    private SeveridadeOcorrencia severidade;

    @Column(nullable = false)
    private Instant horarioDeteccao;

    @Column(nullable = false, updatable = false)
    private Instant horarioSalvamento;

    private Long placeId;

    private String addressType;

    private String nome;

    @Column(length = 500)
    private String displayName;

    private String tipoVia;

    private String via;

    private String bairro;

    private String distrito;

    private String cidade;

    private String municipio;

    private String estado;

    private String iso3166Lvl4;

    private String regiao;

    private String cep;

    private String pais;

    @Column(length = 2)
    private String codigoPais;

    @Builder
    public Ocorrencia(double latitude,
                      double longitude,
                      SeveridadeOcorrencia severidade,
                      Instant horarioDeteccao,
                      Long placeId,
                      String addressType,
                      String nome,
                      String displayName,
                      String tipoVia,
                      String via,
                      String bairro,
                      String distrito,
                      String cidade,
                      String municipio,
                      String estado,
                      String iso3166Lvl4,
                      String regiao,
                      String cep,
                      String pais,
                      String codigoPais) {

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
        this.placeId = placeId;
        this.addressType = addressType;
        this.nome = nome;
        this.displayName = displayName;
        this.tipoVia = tipoVia;
        this.via = via;
        this.bairro = bairro;
        this.distrito = distrito;
        this.cidade = cidade;
        this.municipio = municipio;
        this.estado = estado;
        this.iso3166Lvl4 = iso3166Lvl4;
        this.regiao = regiao;
        this.cep = cep;
        this.pais = pais;
        this.codigoPais = codigoPais;
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

    public enum SeveridadeOcorrencia {
        CRITICO,
        ALTA,
        MEDIA,
        BAIXA
    }
}
