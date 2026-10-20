/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Cadena de integridad SHA-256 entre los odontogramas cerrados de un paciente.
 * <pre>
 * hash = SHA-256( hashAnterior + "\n" + cabecera + "\n" + líneas de hallazgos ordenadas por id ) en hex
 * cabecera: id|pacienteId|tipo|denticion|cerradoEn|cerradoPor|especificaciones
 * hallazgo: id|codigoTipo|pieza|piezaFin|superficies|estado|sigla|registradoEn|registradoPor
 * </pre>
 * Cada campo de texto se escapa ({@code \\}, {@code \|}, {@code \n}) y {@code null} se escribe {@code \N}, de modo
 * que dos contenidos distintos nunca producen el mismo texto canónico. Modificar un hallazgo o los datos de un
 * odontograma cerrado en la BD rompe su hash y el de todos los posteriores.
 */
public final class CadenaIntegridad {

    public static final String GENESIS = "GENESIS";

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private CadenaIntegridad() {
    }

    public static String calcular(String hashAnterior, OdontogramaCanonico o) {
        Objects.requireNonNull(hashAnterior, "hashAnterior");
        StringBuilder texto = new StringBuilder();
        texto.append(hashAnterior).append('\n').append(cabecera(o));
        o.hallazgos().stream()
                .sorted(Comparator.comparingLong(OdontogramaCanonico.Linea::id))
                .forEach(l -> texto.append('\n').append(linea(l)));
        return sha256(texto.toString());
    }

    /**
     * Recalcula la cadena completa.
     *
     * @param cerradosEnOrden odontogramas cerrados del paciente en orden de cierre
     */
    public static ResultadoVerificacion verificar(List<OdontogramaCanonico> cerradosEnOrden) {
        String esperadoAnterior = GENESIS;
        for (OdontogramaCanonico o : cerradosEnOrden) {
            if (!esperadoAnterior.equals(o.hashAnterior())) {
                return ResultadoVerificacion.falla(o.id(),
                        "El enlace con el odontograma anterior no coincide (se eliminó o reordenó un cierre).");
            }
            String recalculado = calcular(o.hashAnterior(), o);
            if (!recalculado.equals(o.hashCierre())) {
                return ResultadoVerificacion.falla(o.id(),
                        "El contenido del odontograma cambió después de su cierre.");
            }
            esperadoAnterior = o.hashCierre();
        }
        return ResultadoVerificacion.correcta();
    }

    static String cabecera(OdontogramaCanonico o) {
        return String.join("|",
                String.valueOf(o.id()), String.valueOf(o.pacienteId()), campo(o.tipo()), campo(o.denticion()),
                fecha(o.cerradoEn()), String.valueOf(o.cerradoPor()), campo(o.especificaciones()));
    }

    static String linea(OdontogramaCanonico.Linea l) {
        return String.join("|",
                String.valueOf(l.id()), campo(l.codigoTipo()), String.valueOf(l.pieza()),
                l.piezaFin() == null ? "\\N" : String.valueOf(l.piezaFin()), String.valueOf(l.superficies()),
                campo(l.estado()), campo(l.sigla()), fecha(l.registradoEn()), String.valueOf(l.registradoPor()));
    }

    private static String campo(String valor) {
        if (valor == null) {
            return "\\N";
        }
        return valor.replace("\\", "\\\\").replace("|", "\\|").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String fecha(LocalDateTime valor) {
        return FECHA_HORA.format(valor);
    }

    private static String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
