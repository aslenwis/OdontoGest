/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.modelo.HorarioAtencion;
import com.odontogest.modelo.Odontologo;
import com.odontogest.persistencia.repositorio.HorarioAtencionRepository;
import com.odontogest.persistencia.repositorio.OdontologoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tres odontólogos de demostración (general, endodoncista y ortodoncista) con horario
 * L–V 09:00–13:00 y 15:00–19:00, S 09:00–13:00. Nombres y colegiaturas son ficticios.
 */
@Component
@RequiredArgsConstructor
public class OdontologoSeeder {

    private final OdontologoRepository odontologoRepository;
    private final HorarioAtencionRepository horarioRepository;

    /** @return los odontólogos creados; el primero es el odontólogo general */
    public List<Odontologo> sembrar() {
        List<Odontologo> creados = new ArrayList<>();
        creados.add(crear("Carlos Alberto", "Quispe Mamani", "90101", null, "Odontología general", "#2563EB"));
        creados.add(crear("María Elena", "Condori Huanca", "90102", "R-0901", "Endodoncia", "#16A34A"));
        creados.add(crear("Jorge Luis", "Ticona Flores", "90103", "R-0902", "Ortodoncia", "#DB2777"));
        return creados;
    }

    private Odontologo crear(String nombres, String apellidos, String cop, String rne, String especialidad, String color) {
        Odontologo o = new Odontologo();
        o.setNombres(nombres);
        o.setApellidos(apellidos);
        o.setCop(cop);
        o.setRne(rne);
        o.setEspecialidad(especialidad);
        o.setColorAgenda(color);
        o.setActivo(true);
        Odontologo guardado = odontologoRepository.save(o);

        for (DayOfWeek dia : DayOfWeek.values()) {
            if (dia == DayOfWeek.SUNDAY) {
                continue;
            }
            bloque(guardado, dia, 9, 13);
            if (dia != DayOfWeek.SATURDAY) {
                bloque(guardado, dia, 15, 19);
            }
        }
        return guardado;
    }

    private void bloque(Odontologo o, DayOfWeek dia, int desde, int hasta) {
        HorarioAtencion h = new HorarioAtencion();
        h.setOdontologo(o);
        h.setDiaSemana(dia);
        h.setHoraInicio(LocalTime.of(desde, 0));
        h.setHoraFin(LocalTime.of(hasta, 0));
        horarioRepository.save(h);
    }
}
