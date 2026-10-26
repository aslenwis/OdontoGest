package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.AnamnesisASEJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnamnesisRepositoryASEJ extends JpaRepository<AnamnesisASEJ, Long> {

    Optional<AnamnesisASEJ> findByPacienteASEJIdASEJ(Long pacienteIdASEJ);
}
