package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HallazgoAnulacionASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HallazgoAnulacionRepositoryASEJ extends JpaRepository<HallazgoAnulacionASEJ, Long> {

    @Query("""
            SELECT a FROM HallazgoAnulacionASEJ a JOIN FETCH a.hallazgoASEJ h
            WHERE h.odontogramaASEJ.pacienteASEJ.idASEJ = :pacienteIdASEJ""")
    List<HallazgoAnulacionASEJ> listarPorPacienteASEJ(Long pacienteIdASEJ);

    boolean existsByHallazgoASEJIdASEJ(Long hallazgoIdASEJ);
}
