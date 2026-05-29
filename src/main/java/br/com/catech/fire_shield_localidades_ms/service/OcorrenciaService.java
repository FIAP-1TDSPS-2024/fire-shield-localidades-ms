package br.com.catech.fire_shield_localidades_ms.service;

import br.com.catech.fire_shield_localidades_ms.dto.OcorrenciaRequest;
import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;

import java.util.List;
import java.util.UUID;

public interface OcorrenciaService {
    Ocorrencia registrar(OcorrenciaRequest request);
    Ocorrencia buscarPorId(UUID id);
    List<Ocorrencia> listarTodas();

    /**
     * Realiza uma única tentativa de enriquecimento de endereço para a ocorrência informada.
     * Deve ser chamado pelo job agendado.
     */
    void tentarEnriquecer(Ocorrencia ocorrencia);
}
