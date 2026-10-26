package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.PagoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepositoryASEJ extends JpaRepository<PagoASEJ, Long> {

    @Query("""
            SELECT p FROM PagoASEJ p JOIN FETCH p.pacienteASEJ
            WHERE p.pacienteASEJ.idASEJ = :pacienteIdASEJ ORDER BY p.fechaASEJ DESC, p.idASEJ DESC""")
    List<PagoASEJ> listarPorPacienteASEJ(long pacienteIdASEJ);

    @Query("""
            SELECT p FROM PagoASEJ p JOIN FETCH p.pacienteASEJ
            WHERE p.fechaASEJ >= :desdeASEJ AND p.fechaASEJ < :hastaASEJ ORDER BY p.fechaASEJ, p.idASEJ""")
    List<PagoASEJ> listarEntreASEJ(LocalDateTime desdeASEJ, LocalDateTime hastaASEJ);
}
