/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.persistencia.conversor.SuperficiesConverter;
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

/**
 * Hallazgo clínico registrado en un odontograma (tabla {@code hallazgo}).
 * <p>
 * Inmutable (NTS 188, 5.6: sin enmendaduras): no tiene setters y Hibernate no emite UPDATE. En un borrador
 * puede eliminarse; en un odontograma cerrado solo se corrige con una {@link HallazgoAnulacion}.
 */
@Entity
@Immutable
@Table(name = "hallazgo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hallazgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontograma_id", nullable = false)
    private Odontograma odontograma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hallazgo_tipo_id", nullable = false)
    private HallazgoTipo hallazgoTipo;

    @Column(name = "pieza_fdi", nullable = false)
    private int piezaFdi;

    @Column(name = "pieza_fdi_fin")
    private Integer piezaFdiFin;

    @Convert(converter = SuperficiesConverter.class)
    @Column(name = "superficies", nullable = false)
    private Set<Superficie> superficies;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoHallazgo estado;

    /** Sigla elegida cuando el tipo tiene opciones; {@code null} si usa la sigla fija del tipo. */
    @Column(name = "sigla")
    private String sigla;

    @Column(name = "nota")
    private String nota;

    @Column(name = "registrado_por", nullable = false)
    private Long registradoPor;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    public static Hallazgo registrar(Odontograma odontograma, HallazgoTipo tipo, int piezaFdi, Integer piezaFdiFin,
                                     Set<Superficie> superficies, EstadoHallazgo estado, String sigla, String nota,
                                     long registradoPor, LocalDateTime registradoEn) {
        Hallazgo h = new Hallazgo();
        h.odontograma = odontograma;
        h.hallazgoTipo = tipo;
        h.piezaFdi = piezaFdi;
        h.piezaFdiFin = piezaFdiFin;
        h.superficies = superficies == null || superficies.isEmpty() ? EnumSet.noneOf(Superficie.class) : EnumSet.copyOf(superficies);
        h.estado = estado;
        h.sigla = sigla;
        h.nota = nota;
        h.registradoPor = registradoPor;
        h.registradoEn = registradoEn;
        return h;
    }
}
