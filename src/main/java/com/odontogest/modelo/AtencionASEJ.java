package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "atencion")
@Getter
@Setter
@NoArgsConstructor
public class AtencionASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacienteASEJ pacienteASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private OdontologoASEJ odontologoASEJ;

    @Column(name = "cita_id")
    private Long citaIdASEJ;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fechaASEJ;

    @Column(name = "motivo_consulta", nullable = false)
    private String motivoConsultaASEJ;

    @Column(name = "examen_clinico")
    private String examenClinicoASEJ;

    @Column(name = "diagnostico")
    private String diagnosticoASEJ;

    @Column(name = "observaciones")
    private String observacionesASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoAtencionASEJ estadoASEJ = EstadoAtencionASEJ.ABIERTA;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEnASEJ;
}
