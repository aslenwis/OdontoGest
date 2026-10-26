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

public final class CadenaIntegridadASEJ {

    public static final String GENESISASEJ = "GENESIS";

    private static final DateTimeFormatter FECHA_HORAASEJ = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private CadenaIntegridadASEJ() {
    }

    public static String calcularASEJ(String hashAnteriorASEJ, OdontogramaCanonicoASEJ oASEJ) {
        Objects.requireNonNull(hashAnteriorASEJ, "hashAnterior");
        StringBuilder textoASEJ = new StringBuilder();
        textoASEJ.append(hashAnteriorASEJ).append('\n').append(cabeceraASEJ(oASEJ));
        oASEJ.hallazgosASEJ().stream()
                .sorted(Comparator.comparingLong(OdontogramaCanonicoASEJ.LineaASEJ::idASEJ))
                .forEach(lASEJ -> textoASEJ.append('\n').append(lineaASEJ(lASEJ)));
        return sha256ASEJ(textoASEJ.toString());
    }

    public static ResultadoVerificacionASEJ verificarASEJ(List<OdontogramaCanonicoASEJ> cerradosEnOrdenASEJ) {
        String esperadoAnteriorASEJ = GENESISASEJ;
        for (OdontogramaCanonicoASEJ oASEJ : cerradosEnOrdenASEJ) {
            if (!esperadoAnteriorASEJ.equals(oASEJ.hashAnteriorASEJ())) {
                return ResultadoVerificacionASEJ.fallaASEJ(oASEJ.idASEJ(),
                        "El enlace con el odontograma anterior no coincide (se eliminó o reordenó un cierre).");
            }
            String recalculadoASEJ = calcularASEJ(oASEJ.hashAnteriorASEJ(), oASEJ);
            if (!recalculadoASEJ.equals(oASEJ.hashCierreASEJ())) {
                return ResultadoVerificacionASEJ.fallaASEJ(oASEJ.idASEJ(),
                        "El contenido del odontograma cambió después de su cierre.");
            }
            esperadoAnteriorASEJ = oASEJ.hashCierreASEJ();
        }
        return ResultadoVerificacionASEJ.correctaASEJ();
    }

    static String cabeceraASEJ(OdontogramaCanonicoASEJ oASEJ) {
        return String.join("|",
                String.valueOf(oASEJ.idASEJ()), String.valueOf(oASEJ.pacienteIdASEJ()), campoASEJ(oASEJ.tipoASEJ()), campoASEJ(oASEJ.denticionASEJ()),
                fechaASEJ(oASEJ.cerradoEnASEJ()), String.valueOf(oASEJ.cerradoPorASEJ()), campoASEJ(oASEJ.especificacionesASEJ()));
    }

    static String lineaASEJ(OdontogramaCanonicoASEJ.LineaASEJ lASEJ) {
        return String.join("|",
                String.valueOf(lASEJ.idASEJ()), campoASEJ(lASEJ.codigoTipoASEJ()), String.valueOf(lASEJ.piezaASEJ()),
                lASEJ.piezaFinASEJ() == null ? "\\N" : String.valueOf(lASEJ.piezaFinASEJ()), String.valueOf(lASEJ.superficiesASEJ()),
                campoASEJ(lASEJ.estadoASEJ()), campoASEJ(lASEJ.siglaASEJ()), fechaASEJ(lASEJ.registradoEnASEJ()), String.valueOf(lASEJ.registradoPorASEJ()));
    }

    private static String campoASEJ(String valorASEJ) {
        if (valorASEJ == null) {
            return "\\N";
        }
        return valorASEJ.replace("\\", "\\\\").replace("|", "\\|").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String fechaASEJ(LocalDateTime valorASEJ) {
        return FECHA_HORAASEJ.format(valorASEJ);
    }

    private static String sha256ASEJ(String textoASEJ) {
        try {
            MessageDigest digestASEJ = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digestASEJ.digest(textoASEJ.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException eASEJ) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", eASEJ);
        }
    }
}
