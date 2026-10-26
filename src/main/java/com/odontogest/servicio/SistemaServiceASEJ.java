package com.odontogest.servicio;

import com.odontogest.config.DirectorioDatosASEJ;
import com.odontogest.config.RestauracionPendienteASEJ;
import com.odontogest.servicio.dto.InformacionSistemaDtoASEJ;
import com.odontogest.servicio.dto.IntegridadBaseDtoASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SistemaServiceASEJ {

    public static final String NOMBREASEJ = "OdontoGest";
    public static final String DESCRIPCIONASEJ = "Sistema de gestión de consultorio odontológico con odontograma interactivo";
    public static final List<String> AUTORESASEJ = List.of("Apaza Mamani Ruben", "Apaza Suca Elwin junior",
            "Inofuente Trujillo David Alex");
    public static final String CURSOASEJ = "Ingeniería de Software";
    public static final String UNIVERSIDADASEJ = "Universidad Nacional de Juliaca";
    public static final int ANIOASEJ = 2026;
    public static final String AVISOASEJ = "Programa de ordenador (soporte lógico) protegido por el Decreto Legislativo"
            + " N.° 822, Ley sobre el Derecho de Autor. © " + ANIOASEJ + " los autores. Todos los derechos reservados."
            + " Prohibida su reproducción, distribución o modificación sin autorización.";
    private static final int MAXIMO_PROBLEMASASEJ = 100;

    private final JdbcTemplate jdbcTemplateASEJ;
    private final ObjectProvider<BuildProperties> buildPropertiesASEJ;

    public InformacionSistemaDtoASEJ informacionASEJ() {
        BuildProperties buildASEJ = buildPropertiesASEJ.getIfAvailable();
        Path homeASEJ = DirectorioDatosASEJ.homeASEJ().toAbsolutePath();
        return new InformacionSistemaDtoASEJ(NOMBREASEJ, DESCRIPCIONASEJ, buildASEJ == null ? "desarrollo" : buildASEJ.getVersion(),
                buildASEJ == null || buildASEJ.getTime() == null ? null
                        : LocalDateTime.ofInstant(buildASEJ.getTime(), ZoneId.systemDefault()),
                AUTORESASEJ, CURSOASEJ, UNIVERSIDADASEJ, ANIOASEJ, AVISOASEJ,
                System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
                homeASEJ.toString(), homeASEJ.resolve(RestauracionPendienteASEJ.BASEASEJ).toString(),
                homeASEJ.resolve(DirectorioDatosASEJ.LOGSASEJ).toString(), homeASEJ.resolve(DirectorioDatosASEJ.RESPALDOSASEJ).toString());
    }

    public IntegridadBaseDtoASEJ verificarIntegridadASEJ() {
        List<String> mensajesASEJ = jdbcTemplateASEJ.queryForList("PRAGMA integrity_check(" + MAXIMO_PROBLEMASASEJ + ")", String.class);
        List<String> problemasASEJ = mensajesASEJ.stream().filter(mASEJ -> !"ok".equalsIgnoreCase(mASEJ)).toList();
        int foraneasASEJ = jdbcTemplateASEJ.queryForList("PRAGMA foreign_key_check").size();
        long tamanioASEJ;
        try {
            tamanioASEJ = Files.size(DirectorioDatosASEJ.homeASEJ().resolve(RestauracionPendienteASEJ.BASEASEJ));
        } catch (IOException eASEJ) {
            tamanioASEJ = -1;
        }
        if (!problemasASEJ.isEmpty() || foraneasASEJ > 0) {
            logASEJ.error("Verificación de integridad: {} problema(s), {} clave(s) foránea(s) rota(s): {}", problemasASEJ.size(),
                    foraneasASEJ, problemasASEJ);
        } else {
            logASEJ.info("Verificación de integridad: correcta");
        }
        return new IntegridadBaseDtoASEJ(problemasASEJ, foraneasASEJ, tamanioASEJ);
    }
}
