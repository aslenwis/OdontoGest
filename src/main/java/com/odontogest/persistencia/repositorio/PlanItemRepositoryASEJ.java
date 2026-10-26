package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.PlanItemASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PlanItemRepositoryASEJ extends JpaRepository<PlanItemASEJ, Long> {

    @Query("""
            SELECT i FROM PlanItemASEJ i JOIN FETCH i.procedimientoASEJ
            WHERE i.planASEJ.idASEJ = :planIdASEJ ORDER BY i.faseASEJ, i.ordenASEJ, i.idASEJ""")
    List<PlanItemASEJ> listarPorPlanASEJ(long planIdASEJ);

    @Query("""
            SELECT i FROM PlanItemASEJ i JOIN FETCH i.procedimientoASEJ JOIN FETCH i.planASEJ
            WHERE i.planASEJ.idASEJ IN :planIdsASEJ ORDER BY i.planASEJ.idASEJ, i.faseASEJ, i.ordenASEJ, i.idASEJ""")
    List<PlanItemASEJ> listarPorPlanesASEJ(Collection<Long> planIdsASEJ);

    @Query("""
            SELECT DISTINCT i.hallazgoIdASEJ FROM PlanItemASEJ i
            WHERE i.planASEJ.pacienteASEJ.idASEJ = :pacienteIdASEJ AND i.planASEJ.idASEJ <> :exceptoASEJ AND i.hallazgoIdASEJ IS NOT NULL
              AND i.planASEJ.estadoASEJ <> com.odontogest.modelo.EstadoPlanASEJ.ANULADO
              AND i.estadoASEJ <> com.odontogest.modelo.EstadoPlanItemASEJ.CANCELADO""")
    List<Long> hallazgosEnOtrosPlanesASEJ(long pacienteIdASEJ, long exceptoASEJ);
}
