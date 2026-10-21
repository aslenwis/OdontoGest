/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.dni;

import java.util.Map;

/**
 * Configuración de la API de DNI (claves {@code api.dni.*}). El proveedor no se fija en el código: URL, cabecera,
 * token y nombres de los campos JSON son configurables.
 *
 * @param urlPlantilla URL con {@code {numero}}, p. ej. {@code https://proveedor/v1/dni/{numero}}
 * @param cabeceraAuth cabecera de autenticación ({@code Authorization}); vacía = sin autenticación
 * @param mapeo        campo propio → nombre del campo en la respuesta (admite rutas con punto, p. ej. {@code data.nombres})
 */
public record ParametrosDni(String urlPlantilla, String cabeceraAuth, String prefijoToken, String token,
                            Map<String, String> mapeo, int timeoutSeg) {

    public ParametrosDni {
        mapeo = Map.copyOf(mapeo);
    }
}
