package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.ProcedimientoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedimientoRepositoryASEJ extends JpaRepository<ProcedimientoASEJ, Long> {

    Optional<ProcedimientoASEJ> findByCodigoASEJIgnoreCase(String codigoASEJ);

    List<ProcedimientoASEJ> findAllByOrderByCategoriaASEJAscCodigoASEJAsc();
}
