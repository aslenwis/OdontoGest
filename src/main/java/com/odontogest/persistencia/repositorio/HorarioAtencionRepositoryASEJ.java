package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HorarioAtencionASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HorarioAtencionRepositoryASEJ extends JpaRepository<HorarioAtencionASEJ, Long> {

    List<HorarioAtencionASEJ> findByOdontologoASEJIdASEJOrderByDiaSemanaASEJAscHoraInicioASEJAsc(Long odontologoIdASEJ);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM HorarioAtencionASEJ h WHERE h.odontologoASEJ.idASEJ = :odontologoIdASEJ")
    void eliminarPorOdontologoASEJ(Long odontologoIdASEJ);
}
