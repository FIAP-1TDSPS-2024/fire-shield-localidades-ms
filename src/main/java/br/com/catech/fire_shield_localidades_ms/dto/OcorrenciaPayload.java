package br.com.catech.fire_shield_localidades_ms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO que representa a mensagem recebida da fila RabbitMQ.
 * Os campos seguem o contrato do producer externo (PascalCase).
 */
public record OcorrenciaPayload(

        @JsonProperty("Id")
        String id,

        @JsonProperty("Tipo")
        String tipo,

        @JsonProperty("Latitude")
        Double latitude,

        @JsonProperty("Longitude")
        Double longitude,

        @JsonProperty("Urgencia")
        String urgencia,

        @JsonProperty("Area")
        Double area,

        @JsonProperty("Distancia")
        Double distancia,

        @JsonProperty("DataUtc")
        Instant dataUtc,

        @JsonProperty("ReportadoPor")
        String reportadoPor,

        @JsonProperty("ContatoAutor")
        String contatoAutor
) {}

