package com.odontogest.servicio.dto;

import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteDetalleASEJ(
        long idASEJ, String numeroHistoriaASEJ, TipoDocumentoASEJ tipoDocumentoASEJ, String numeroDocumentoASEJ,
        String nombresASEJ, String apellidoPaternoASEJ, String apellidoMaternoASEJ,
        LocalDate fechaNacimientoASEJ, SexoASEJ sexoASEJ, String telefonoASEJ, String emailASEJ, String direccionASEJ, String ocupacionASEJ,
        String contactoEmergenciaNombreASEJ, String contactoEmergenciaTelefonoASEJ,
        String apoderadoNombreASEJ, String apoderadoDocumentoASEJ, String apoderadoParentescoASEJ,
        boolean aceptaRecordatoriosASEJ, boolean activoASEJ, int edadASEJ, String alergiasASEJ, LocalDateTime consentimientoDatosEnASEJ,
        boolean demoASEJ) {

    public static final int MAYORIA_DE_EDADASEJ = 18;

    public String nombreCompletoASEJ() {
        return nombresASEJ + " " + apellidoPaternoASEJ + (apellidoMaternoASEJ == null ? "" : " " + apellidoMaternoASEJ);
    }

    public String documentoASEJ() {
        return tipoDocumentoASEJ == TipoDocumentoASEJ.SIN_DOCUMENTO ? "Sin documento" : tipoDocumentoASEJ.name() + " " + numeroDocumentoASEJ;
    }

    public boolean esMenorDeEdadASEJ() {
        return edadASEJ < MAYORIA_DE_EDADASEJ;
    }

    public boolean tieneAlergiasASEJ() {
        return alergiasASEJ != null && !alergiasASEJ.isBlank();
    }
}
