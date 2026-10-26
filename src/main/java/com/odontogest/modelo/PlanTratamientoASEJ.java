package com.odontogest.modelo;

import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_tratamiento")
@Getter
@Setter
@NoArgsConstructor
public class PlanTratamientoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, updatable = false)
    private PacienteASEJ pacienteASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private OdontologoASEJ odontologoASEJ;

    @Column(name = "odontograma_id")
    private Long odontogramaIdASEJ;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fechaASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPlanASEJ estadoASEJ = EstadoPlanASEJ.BORRADOR;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "subtotal_cent", nullable = false)
    private BigDecimal subtotalASEJ = BigDecimal.ZERO;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "descuento_cent", nullable = false)
    private BigDecimal descuentoASEJ = BigDecimal.ZERO;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "total_cent", nullable = false)
    private BigDecimal totalASEJ = BigDecimal.ZERO;

    @Column(name = "observaciones")
    private String observacionesASEJ;

    @Column(name = "aceptado_en")
    private LocalDateTime aceptadoEnASEJ;
}
