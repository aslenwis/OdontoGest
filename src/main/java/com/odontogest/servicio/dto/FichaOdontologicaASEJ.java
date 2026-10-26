package com.odontogest.servicio.dto;

public record FichaOdontologicaASEJ(DatosClinicaASEJ clinicaASEJ, PacienteDetalleASEJ pacienteASEJ, OdontogramaVistaASEJ vistaASEJ,
                                String odontologoNombreASEJ, String odontologoCopASEJ, byte[] imagenPngASEJ) {
}
