package com.odontogest.modelo;

import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "procedimiento")
@Getter
@Setter
@NoArgsConstructor
public class ProcedimientoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigoASEJ;

    @Column(name = "nombre", nullable = false)
    private String nombreASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private CategoriaProcedimientoASEJ categoriaASEJ;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "precio_cent", nullable = false)
    private BigDecimal precioASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_cobro", nullable = false)
    private FormaCobroASEJ formaCobroASEJ;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMinASEJ = 30;

    @Column(name = "activo", nullable = false)
    private boolean activoASEJ = true;
}
