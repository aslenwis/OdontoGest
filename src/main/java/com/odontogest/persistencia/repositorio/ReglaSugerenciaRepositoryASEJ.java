package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.ReglaSugerenciaASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReglaSugerenciaRepositoryASEJ extends JpaRepository<ReglaSugerenciaASEJ, Long> {

    @Query("""
            SELECT r FROM ReglaSugerenciaASEJ r JOIN FETCH r.procedimientoASEJ LEFT JOIN FETCH r.hallazgoTipoASEJ
            ORDER BY r.prioridadASEJ, r.idASEJ""")
    List<ReglaSugerenciaASEJ> listarConRelacionesASEJ();
}
