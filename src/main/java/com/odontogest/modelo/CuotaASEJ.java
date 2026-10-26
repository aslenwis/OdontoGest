package com.odontogest.modelo;

import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;

@Entity
@Table(name = "cuota")
@Getter
@Setter
@NoArgsConstructor
public class CuotaASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private PlanTratamientoASEJ planASEJ;

    @Column(name = "numero", nullable = false)
    private int numeroASEJ;

    @Column(name = "vencimiento", nullable = false)
    private LocalDate vencimientoASEJ;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "monto_cent", nullable = false)
    private BigDecimal montoASEJ;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "pagado_cent", nullable = false)
    private BigDecimal pagadoASEJ = BigDecimal.ZERO;
}
