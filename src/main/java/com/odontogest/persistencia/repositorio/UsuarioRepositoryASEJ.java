package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryASEJ extends JpaRepository<UsuarioASEJ, Long> {

    @EntityGraph(attributePaths = "odontologoASEJ")
    Optional<UsuarioASEJ> findByUsernameASEJIgnoreCase(String usernameASEJ);

    @EntityGraph(attributePaths = "odontologoASEJ")
    List<UsuarioASEJ> findAllByOrderByUsernameASEJAsc();

    long countByRolASEJAndActivoASEJTrue(RolASEJ rolASEJ);
}
