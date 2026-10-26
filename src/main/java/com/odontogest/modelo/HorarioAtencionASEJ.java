package com.odontogest.modelo;

import com.odontogest.persistencia.conversor.DiaSemanaConverterASEJ;
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

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horario_atencion")
@Getter
@Setter
@NoArgsConstructor
public class HorarioAtencionASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private OdontologoASEJ odontologoASEJ;

    @Convert(converter = DiaSemanaConverterASEJ.class)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemanaASEJ;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicioASEJ;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFinASEJ;
}
