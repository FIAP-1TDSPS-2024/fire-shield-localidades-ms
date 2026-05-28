package br.com.catech.fire_shield_localidades_ms.repository;

import br.com.catech.fire_shield_localidades_ms.entity.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {
}
