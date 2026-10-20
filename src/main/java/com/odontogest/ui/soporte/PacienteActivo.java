/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.dto.PacienteDetalle;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Paciente abierto en la ficha. Las pestañas de la ficha escuchan {@link #pacienteProperty()} y recargan
 * sus datos cuando cambia. Solo se modifica en el hilo de UI.
 */
@Component
@RequiredArgsConstructor
public class PacienteActivo {

    private final PacienteService pacienteService;
    private final TareaUi tareaUi;
    private final ReadOnlyObjectWrapper<PacienteDetalle> paciente = new ReadOnlyObjectWrapper<>(this, "paciente");

    public ReadOnlyObjectProperty<PacienteDetalle> pacienteProperty() {
        return paciente.getReadOnlyProperty();
    }

    public PacienteDetalle get() {
        return paciente.get();
    }

    /** Carga la ficha en segundo plano y la publica al terminar. */
    public void abrir(long pacienteId) {
        tareaUi.ejecutar(() -> pacienteService.obtener(pacienteId), paciente::set);
    }

    /** Vuelve a leer el paciente actual (p. ej. tras guardar la anamnesis, para actualizar la alerta de alergias). */
    public void recargar() {
        PacienteDetalle actual = paciente.get();
        if (actual != null) {
            abrir(actual.id());
        }
    }

    public void cerrar() {
        paciente.set(null);
    }
}
