package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PacienteRepositoryASEJ extends JpaRepository<PacienteASEJ, Long>, JpaSpecificationExecutor<PacienteASEJ> {

    Optional<PacienteASEJ> findByTipoDocumentoASEJAndNumeroDocumentoASEJ(TipoDocumentoASEJ tipoASEJ, String numeroASEJ);
}
