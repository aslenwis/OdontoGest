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
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
public class CitaASEJ {

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

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicioASEJ;

    @Column(name = "fin", nullable = false)
    private LocalDateTime finASEJ;

    @Column(name = "motivo", nullable = false)
    private String motivoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCitaASEJ estadoASEJ = EstadoCitaASEJ.PROGRAMADA;

    @Column(name = "plan_item_id")
    private Long planItemIdASEJ;

    @Column(name = "notas")
    private String notasASEJ;

    @Column(name = "recordatorio_enviado_en")
    private LocalDateTime recordatorioEnviadoEnASEJ;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private Long creadoPorASEJ;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEnASEJ;
}
