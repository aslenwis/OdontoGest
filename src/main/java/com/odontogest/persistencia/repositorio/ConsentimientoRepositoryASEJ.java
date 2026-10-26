package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.ConsentimientoASEJ;
import com.odontogest.modelo.TipoConsentimientoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimientoRepositoryASEJ extends JpaRepository<ConsentimientoASEJ, Long> {

    List<ConsentimientoASEJ> findByPacienteASEJIdASEJOrderByAceptadoEnASEJDesc(Long pacienteIdASEJ);

    Optional<ConsentimientoASEJ> findFirstByPacienteASEJIdASEJAndTipoASEJOrderByAceptadoEnASEJDesc(Long pacienteIdASEJ, TipoConsentimientoASEJ tipoASEJ);

    Optional<ConsentimientoASEJ> findFirstByPlanIdASEJAndTipoASEJOrderByAceptadoEnASEJDesc(Long planIdASEJ, TipoConsentimientoASEJ tipoASEJ);
}
