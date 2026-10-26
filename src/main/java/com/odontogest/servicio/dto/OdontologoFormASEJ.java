package com.odontogest.servicio.dto;

import java.util.List;

public record OdontologoFormASEJ(Long idASEJ, String nombresASEJ, String apellidosASEJ, String copASEJ, String rneASEJ, String especialidadASEJ,
                             String telefonoASEJ, String emailASEJ, String colorAgendaASEJ, boolean activoASEJ,
                             List<HorarioDtoASEJ> horariosASEJ) {

    public OdontologoFormASEJ {
        horariosASEJ = horariosASEJ == null ? List.of() : List.copyOf(horariosASEJ);
    }
}
