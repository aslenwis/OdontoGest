package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.AtencionASEJ;
import com.odontogest.modelo.EstadoAtencionASEJ;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AtencionRepositoryASEJ extends JpaRepository<AtencionASEJ, Long> {

    @EntityGraph(attributePaths = "odontologoASEJ")
    List<AtencionASEJ> findByPacienteASEJIdASEJOrderByFechaASEJDescIdASEJDesc(Long pacienteIdASEJ);

    Optional<AtencionASEJ> findFirstByPacienteASEJIdASEJAndEstadoASEJ(Long pacienteIdASEJ, EstadoAtencionASEJ estadoASEJ);

    List<AtencionASEJ> findByCitaIdASEJIn(Collection<Long> citaIdsASEJ);
}
