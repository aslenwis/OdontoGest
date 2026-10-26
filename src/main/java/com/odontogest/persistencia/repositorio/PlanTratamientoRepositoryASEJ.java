package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.PlanTratamientoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PlanTratamientoRepositoryASEJ extends JpaRepository<PlanTratamientoASEJ, Long> {

    @Query("""
            SELECT p FROM PlanTratamientoASEJ p JOIN FETCH p.odontologoASEJ
            WHERE p.pacienteASEJ.idASEJ = :pacienteIdASEJ ORDER BY p.fechaASEJ DESC, p.idASEJ DESC""")
    List<PlanTratamientoASEJ> listarPorPacienteASEJ(long pacienteIdASEJ);

    List<PlanTratamientoASEJ> findByPacienteASEJIdASEJAndEstadoASEJIn(long pacienteIdASEJ, Collection<EstadoPlanASEJ> estadosASEJ);
}
