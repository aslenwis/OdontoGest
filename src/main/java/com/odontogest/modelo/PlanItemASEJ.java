package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import com.odontogest.persistencia.conversor.SuperficiesConverterASEJ;
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
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "plan_item")
@Getter
@Setter
@NoArgsConstructor
public class PlanItemASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private PlanTratamientoASEJ planASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private ProcedimientoASEJ procedimientoASEJ;

    @Column(name = "hallazgo_id")
    private Long hallazgoIdASEJ;

    @Column(name = "pieza_fdi")
    private Integer piezaFdiASEJ;

    @Convert(converter = SuperficiesConverterASEJ.class)
    @Column(name = "superficies", nullable = false)
    private Set<SuperficieASEJ> superficiesASEJ = EnumSet.noneOf(SuperficieASEJ.class);

    @Column(name = "fase", nullable = false)
    private int faseASEJ;

    @Column(name = "cantidad", nullable = false)
    private int cantidadASEJ = 1;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "precio_unit_cent", nullable = false)
    private BigDecimal precioUnitASEJ;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "descuento_cent", nullable = false)
    private BigDecimal descuentoASEJ = BigDecimal.ZERO;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "total_cent", nullable = false)
    private BigDecimal totalASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPlanItemASEJ estadoASEJ = EstadoPlanItemASEJ.PENDIENTE;

    @Column(name = "atencion_id")
    private Long atencionIdASEJ;

    @Column(name = "realizado_en")
    private LocalDateTime realizadoEnASEJ;

    @Column(name = "orden", nullable = false)
    private int ordenASEJ;
}
