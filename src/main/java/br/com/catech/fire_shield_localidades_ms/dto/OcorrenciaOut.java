package br.com.catech.fire_shield_localidades_ms.dto;

import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia.SeveridadeOcorrencia;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload publicado na fila após o enriquecimento bem-sucedido da ocorrência.
 */
public record OcorrenciaOut(

        UUID uuid,
        double latitude,
        double longitude,
        SeveridadeOcorrencia severidade,
        Instant horarioDeteccao,

        // Endereço do estado para baixo
        String nome,
        String bairro,
        String cidade,
        String estado,
        String cep
) {}

