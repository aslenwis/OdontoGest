package com.odontogest.servicio.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InformacionSistemaDtoASEJ(String nombreASEJ, String descripcionASEJ, String versionASEJ, LocalDateTime compiladoASEJ,
                                    List<String> autoresASEJ, String cursoASEJ, String universidadASEJ, int anioASEJ, String avisoASEJ,
                                    String javaVersionASEJ, String carpetaDatosASEJ, String baseDatosASEJ, String carpetaLogsASEJ,
                                    String carpetaRespaldosASEJ) {

    public InformacionSistemaDtoASEJ {
        autoresASEJ = List.copyOf(autoresASEJ);
    }
}
