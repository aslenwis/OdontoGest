package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.CuotaASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface CuotaRepositoryASEJ extends JpaRepository<CuotaASEJ, Long> {

    List<CuotaASEJ> findByPlanASEJIdASEJOrderByNumeroASEJAsc(long planIdASEJ);

    @Query("""
            SELECT c FROM CuotaASEJ c JOIN FETCH c.planASEJ p
            WHERE p.pacienteASEJ.idASEJ = :pacienteIdASEJ AND p.estadoASEJ IN :estadosASEJ
            ORDER BY c.vencimientoASEJ, p.idASEJ, c.numeroASEJ""")
    List<CuotaASEJ> listarPorPacienteASEJ(long pacienteIdASEJ, Collection<EstadoPlanASEJ> estadosASEJ);
}
