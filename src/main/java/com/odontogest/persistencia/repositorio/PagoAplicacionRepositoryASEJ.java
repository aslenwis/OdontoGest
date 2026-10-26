package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.PagoAplicacionASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PagoAplicacionRepositoryASEJ extends JpaRepository<PagoAplicacionASEJ, Long> {

    @Query("""
            SELECT a FROM PagoAplicacionASEJ a JOIN FETCH a.cuotaASEJ c JOIN FETCH c.planASEJ
            WHERE a.pagoASEJ.idASEJ IN :pagoIdsASEJ ORDER BY a.pagoASEJ.idASEJ, c.vencimientoASEJ, c.numeroASEJ""")
    List<PagoAplicacionASEJ> listarPorPagosASEJ(Collection<Long> pagoIdsASEJ);
}
