package com.odontogest.servicio.dto;

public record DatosClinicaASEJ(String nombreASEJ, String rucASEJ, String direccionASEJ, String telefonoASEJ, String emailASEJ, String logoRutaASEJ) {

    public DatosClinicaASEJ(String nombreASEJ, String rucASEJ, String direccionASEJ, String telefonoASEJ, String emailASEJ) {
        this(nombreASEJ, rucASEJ, direccionASEJ, telefonoASEJ, emailASEJ, "");
    }
}
