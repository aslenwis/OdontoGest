package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.CitaASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CitaRepositoryASEJ extends JpaRepository<CitaASEJ, Long> {

    @Query("""
            SELECT COUNT(c) > 0 FROM CitaASEJ c
            WHERE c.odontologoASEJ.idASEJ = :odontologoIdASEJ AND c.estadoASEJ IN :ocupanASEJ
              AND c.inicioASEJ < :finASEJ AND c.finASEJ > :inicioASEJ AND c.idASEJ <> :excluirIdASEJ""")
    boolean existeSolapamientoOdontologoASEJ(long odontologoIdASEJ, LocalDateTime inicioASEJ, LocalDateTime finASEJ, long excluirIdASEJ,
                                         Collection<EstadoCitaASEJ> ocupanASEJ);

    @Query("""
            SELECT COUNT(c) > 0 FROM CitaASEJ c
            WHERE c.pacienteASEJ.idASEJ = :pacienteIdASEJ AND c.estadoASEJ IN :ocupanASEJ
              AND c.inicioASEJ < :finASEJ AND c.finASEJ > :inicioASEJ AND c.idASEJ <> :excluirIdASEJ""")
    boolean existeSolapamientoPacienteASEJ(long pacienteIdASEJ, LocalDateTime inicioASEJ, LocalDateTime finASEJ, long excluirIdASEJ,
                                       Collection<EstadoCitaASEJ> ocupanASEJ);

    @Query("""
            SELECT c FROM CitaASEJ c JOIN FETCH c.pacienteASEJ JOIN FETCH c.odontologoASEJ
            WHERE c.odontologoASEJ.idASEJ = :odontologoIdASEJ AND c.estadoASEJ IN :estadosASEJ
              AND c.inicioASEJ < :hastaASEJ AND c.finASEJ > :desdeASEJ
            ORDER BY c.inicioASEJ""")
    List<CitaASEJ> listarOdontologoASEJ(long odontologoIdASEJ, LocalDateTime desdeASEJ, LocalDateTime hastaASEJ, Collection<EstadoCitaASEJ> estadosASEJ);

    @Query("""
            SELECT c FROM CitaASEJ c JOIN FETCH c.pacienteASEJ JOIN FETCH c.odontologoASEJ
            WHERE c.estadoASEJ IN :estadosASEJ AND c.inicioASEJ >= :desdeASEJ AND c.inicioASEJ < :hastaASEJ
            ORDER BY c.inicioASEJ, c.odontologoASEJ.idASEJ""")
    List<CitaASEJ> listarEntreASEJ(LocalDateTime desdeASEJ, LocalDateTime hastaASEJ, Collection<EstadoCitaASEJ> estadosASEJ);

    @Query("""
            SELECT c FROM CitaASEJ c JOIN FETCH c.pacienteASEJ JOIN FETCH c.odontologoASEJ
            WHERE c.pacienteASEJ.idASEJ = :pacienteIdASEJ AND c.estadoASEJ IN :estadosASEJ
              AND c.inicioASEJ < :hastaASEJ AND c.finASEJ > :desdeASEJ
            ORDER BY c.inicioASEJ""")
    List<CitaASEJ> listarPacienteASEJ(long pacienteIdASEJ, LocalDateTime desdeASEJ, LocalDateTime hastaASEJ, Collection<EstadoCitaASEJ> estadosASEJ);

    @Query("""
            SELECT c FROM CitaASEJ c JOIN FETCH c.odontologoASEJ
            WHERE c.pacienteASEJ.idASEJ = :pacienteIdASEJ ORDER BY c.inicioASEJ DESC""")
    List<CitaASEJ> listarPorPacienteASEJ(long pacienteIdASEJ);

    @Query("""
            SELECT c FROM CitaASEJ c JOIN FETCH c.pacienteASEJ p JOIN FETCH c.odontologoASEJ
            WHERE c.estadoASEJ IN :estadosASEJ AND c.inicioASEJ >= :desdeASEJ AND c.inicioASEJ < :hastaASEJ
              AND c.recordatorioEnviadoEnASEJ IS NULL
              AND p.emailASEJ IS NOT NULL AND p.emailASEJ <> '' AND p.aceptaRecordatoriosASEJ = true
            ORDER BY c.inicioASEJ""")
    List<CitaASEJ> pendientesDeRecordatorioASEJ(LocalDateTime desdeASEJ, LocalDateTime hastaASEJ, Collection<EstadoCitaASEJ> estadosASEJ);
}
