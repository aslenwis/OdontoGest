package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Immutable
@Table(name = "hallazgo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HallazgoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontograma_id", nullable = false)
    private OdontogramaASEJ odontogramaASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hallazgo_tipo_id", nullable = false)
    private HallazgoTipoASEJ hallazgoTipoASEJ;

    @Column(name = "pieza_fdi", nullable = false)
    private int piezaFdiASEJ;

    @Column(name = "pieza_fdi_fin")
    private Integer piezaFdiFinASEJ;

    @Convert(converter = SuperficiesConverterASEJ.class)
    @Column(name = "superficies", nullable = false)
    private Set<SuperficieASEJ> superficiesASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoHallazgoASEJ estadoASEJ;

    @Column(name = "sigla")
    private String siglaASEJ;

    @Column(name = "nota")
    private String notaASEJ;

    @Column(name = "registrado_por", nullable = false)
    private Long registradoPorASEJ;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEnASEJ;

    public static HallazgoASEJ registrarASEJ(OdontogramaASEJ odontogramaASEJ, HallazgoTipoASEJ tipoASEJ, int piezaFdiASEJ, Integer piezaFdiFinASEJ,
                                     Set<SuperficieASEJ> superficiesASEJ, EstadoHallazgoASEJ estadoASEJ, String siglaASEJ, String notaASEJ,
                                     long registradoPorASEJ, LocalDateTime registradoEnASEJ) {
        HallazgoASEJ hASEJ = new HallazgoASEJ();
        hASEJ.odontogramaASEJ = odontogramaASEJ;
        hASEJ.hallazgoTipoASEJ = tipoASEJ;
        hASEJ.piezaFdiASEJ = piezaFdiASEJ;
        hASEJ.piezaFdiFinASEJ = piezaFdiFinASEJ;
        hASEJ.superficiesASEJ = superficiesASEJ == null || superficiesASEJ.isEmpty() ? EnumSet.noneOf(SuperficieASEJ.class) : EnumSet.copyOf(superficiesASEJ);
        hASEJ.estadoASEJ = estadoASEJ;
        hASEJ.siglaASEJ = siglaASEJ;
        hASEJ.notaASEJ = notaASEJ;
        hASEJ.registradoPorASEJ = registradoPorASEJ;
        hASEJ.registradoEnASEJ = registradoEnASEJ;
        return hASEJ;
    }
}
