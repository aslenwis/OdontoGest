package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.DenticionASEJ;
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
@Table(name = "odontograma")
@Getter
@Setter
@NoArgsConstructor
public class OdontogramaASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacienteASEJ pacienteASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_id", nullable = false)
    private AtencionASEJ atencionASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false)
    private TipoOdontogramaASEJ tipoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "denticion", nullable = false)
    private DenticionASEJ denticionASEJ;

    @Column(name = "especificaciones")
    private String especificacionesASEJ;

    @Column(name = "observaciones")
    private String observacionesASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoOdontogramaRegistroASEJ estadoASEJ = EstadoOdontogramaRegistroASEJ.BORRADOR;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEnASEJ;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEnASEJ;

    @Column(name = "cerrado_por")
    private Long cerradoPorASEJ;

    @Column(name = "hash_anterior")
    private String hashAnteriorASEJ;

    @Column(name = "hash_cierre")
    private String hashCierreASEJ;

    public boolean esBorradorASEJ() {
        return estadoASEJ == EstadoOdontogramaRegistroASEJ.BORRADOR;
    }
}
