package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.OdontologoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdontologoRepositoryASEJ extends JpaRepository<OdontologoASEJ, Long> {

    List<OdontologoASEJ> findAllByOrderByApellidosASEJAscNombresASEJAsc();

    List<OdontologoASEJ> findByActivoASEJTrueOrderByApellidosASEJAscNombresASEJAsc();

    Optional<OdontologoASEJ> findByCopASEJIgnoreCase(String copASEJ);
}
