package com.odontogest.ui.soporte;

import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PacienteActivoASEJ {

    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final ReadOnlyObjectWrapper<PacienteDetalleASEJ> pacienteASEJ = new ReadOnlyObjectWrapper<>(this, "paciente");

    public ReadOnlyObjectProperty<PacienteDetalleASEJ> pacientePropertyASEJ() {
        return pacienteASEJ.getReadOnlyProperty();
    }

    public PacienteDetalleASEJ getASEJ() {
        return pacienteASEJ.get();
    }

    public void abrirASEJ(long pacienteIdASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ), pacienteASEJ::set);
    }

    public void recargarASEJ() {
        PacienteDetalleASEJ actualASEJ = pacienteASEJ.get();
        if (actualASEJ != null) {
            abrirASEJ(actualASEJ.idASEJ());
        }
    }

    public void cerrarASEJ() {
        pacienteASEJ.set(null);
    }
}
