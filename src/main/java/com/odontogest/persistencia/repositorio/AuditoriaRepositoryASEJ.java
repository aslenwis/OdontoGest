package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.AuditoriaASEJ;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepositoryASEJ extends JpaRepository<AuditoriaASEJ, Long> {

    @Query("""
            SELECT a FROM AuditoriaASEJ a
            WHERE a.fechaASEJ >= :desdeASEJ AND a.fechaASEJ < :hastaASEJ AND (:accionASEJ = '' OR a.accionASEJ = :accionASEJ)
            ORDER BY a.fechaASEJ DESC, a.idASEJ DESC""")
    List<AuditoriaASEJ> buscarASEJ(LocalDateTime desdeASEJ, LocalDateTime hastaASEJ, String accionASEJ, Pageable paginaASEJ);

    List<AuditoriaASEJ> findByEntidadASEJAndEntidadIdASEJOrderByIdASEJAsc(String entidadASEJ, Long entidadIdASEJ);

    @Query("SELECT DISTINCT a.accionASEJ FROM AuditoriaASEJ a ORDER BY a.accionASEJ")
    List<String> accionesASEJ();
}
