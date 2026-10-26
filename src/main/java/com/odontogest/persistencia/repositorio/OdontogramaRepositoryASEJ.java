package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.OdontogramaASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdontogramaRepositoryASEJ extends JpaRepository<OdontogramaASEJ, Long> {

    List<OdontogramaASEJ> findByPacienteASEJIdASEJOrderByCreadoEnASEJAscIdASEJAsc(Long pacienteIdASEJ);

    List<OdontogramaASEJ> findByPacienteASEJIdASEJAndEstadoASEJOrderByCerradoEnASEJAscIdASEJAsc(Long pacienteIdASEJ, EstadoOdontogramaRegistroASEJ estadoASEJ);

    Optional<OdontogramaASEJ> findFirstByPacienteASEJIdASEJAndEstadoASEJ(Long pacienteIdASEJ, EstadoOdontogramaRegistroASEJ estadoASEJ);

    boolean existsByPacienteASEJIdASEJAndTipoASEJ(Long pacienteIdASEJ, TipoOdontogramaASEJ tipoASEJ);

    List<OdontogramaASEJ> findByAtencionASEJIdASEJ(Long atencionIdASEJ);
}
