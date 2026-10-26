package com.odontogest.servicio.dto;

import java.util.List;

public record OdontologoDtoASEJ(long idASEJ, String nombresASEJ, String apellidosASEJ, String copASEJ, String rneASEJ, String especialidadASEJ,
                            String telefonoASEJ, String emailASEJ, String colorAgendaASEJ, boolean activoASEJ,
                            List<HorarioDtoASEJ> horariosASEJ) {

    public OdontologoDtoASEJ {
        horariosASEJ = List.copyOf(horariosASEJ);
    }

    public String nombreCompletoASEJ() {
        return nombresASEJ + " " + apellidosASEJ;
    }

    @Override
    public String toString() {
        return nombreCompletoASEJ();
    }
}
