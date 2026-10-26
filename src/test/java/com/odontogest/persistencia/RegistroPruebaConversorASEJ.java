package com.odontogest.persistencia;

import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import com.odontogest.persistencia.conversor.SuperficiesConverterASEJ;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = RegistroPruebaConversorASEJ.TABLAASEJ)
@Getter
@Setter
public class RegistroPruebaConversorASEJ {

    static final String TABLAASEJ = "prueba_conversor";
    static final String DDLASEJ = """
            CREATE TABLE IF NOT EXISTS prueba_conversor (
                id          INTEGER PRIMARY KEY,
                fecha       TEXT,
                fecha_hora  TEXT,
                hora        TEXT,
                monto_cent  INTEGER,
                superficies INTEGER NOT NULL DEFAULT 0 CHECK (superficies BETWEEN 0 AND 31)
            )""";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idASEJ;

    @Column(name = "fecha")
    private LocalDate fechaASEJ;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHoraASEJ;

    @Column(name = "hora")
    private LocalTime horaASEJ;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "monto_cent")
    private BigDecimal montoASEJ;

    @Convert(converter = SuperficiesConverterASEJ.class)
    @Column(name = "superficies", nullable = false)
    private Set<SuperficieASEJ> superficiesASEJ;
}
