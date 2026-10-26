package com.odontogest.servicio.dto;

import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;

import java.time.LocalDate;

public record PacienteFormASEJ(
        Long idASEJ, TipoDocumentoASEJ tipoDocumentoASEJ, String numeroDocumentoASEJ,
        String nombresASEJ, String apellidoPaternoASEJ, String apellidoMaternoASEJ,
        LocalDate fechaNacimientoASEJ, SexoASEJ sexoASEJ, String telefonoASEJ, String emailASEJ, String direccionASEJ, String ocupacionASEJ,
        String contactoEmergenciaNombreASEJ, String contactoEmergenciaTelefonoASEJ,
        String apoderadoNombreASEJ, String apoderadoDocumentoASEJ, String apoderadoParentescoASEJ,
        boolean aceptaRecordatoriosASEJ) {
}
