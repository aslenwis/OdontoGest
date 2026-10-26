package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;
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

@Entity
@Table(name = "regla_sugerencia")
@Getter
@Setter
@NoArgsConstructor
public class ReglaSugerenciaASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hallazgo_tipo_id")
    private HallazgoTipoASEJ hallazgoTipoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoHallazgoASEJ estadoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_radicular")
    private TipoRadicularASEJ tipoRadicularASEJ;

    @Column(name = "min_superficies", nullable = false)
    private int minSuperficiesASEJ;

    @Column(name = "max_superficies", nullable = false)
    private int maxSuperficiesASEJ = 5;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private ProcedimientoASEJ procedimientoASEJ;

    @Column(name = "fase", nullable = false)
    private int faseASEJ;

    @Column(name = "prioridad", nullable = false)
    private int prioridadASEJ = 100;

    @Column(name = "activo", nullable = false)
    private boolean activoASEJ = true;
}
