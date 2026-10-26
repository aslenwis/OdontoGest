package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HallazgoASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HallazgoRepositoryASEJ extends JpaRepository<HallazgoASEJ, Long> {

    @Query("""
            SELECT h FROM HallazgoASEJ h JOIN FETCH h.hallazgoTipoASEJ JOIN FETCH h.odontogramaASEJ o
            WHERE o.pacienteASEJ.idASEJ = :pacienteIdASEJ ORDER BY h.registradoEnASEJ, h.idASEJ""")
    List<HallazgoASEJ> listarPorPacienteASEJ(Long pacienteIdASEJ);

    @Query("SELECT h FROM HallazgoASEJ h JOIN FETCH h.hallazgoTipoASEJ WHERE h.odontogramaASEJ.idASEJ = :odontogramaIdASEJ ORDER BY h.idASEJ")
    List<HallazgoASEJ> listarPorOdontogramaASEJ(Long odontogramaIdASEJ);

    long countByOdontogramaASEJIdASEJ(Long odontogramaIdASEJ);
}
