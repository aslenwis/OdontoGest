package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CadenaIntegridadTestASEJ {

    private static final LocalDateTime CIERREASEJ = LocalDateTime.of(2026, 3, 2, 10, 15, 30);

    private static OdontogramaCanonicoASEJ.LineaASEJ lineaASEJ(long idASEJ, String codigoASEJ, int piezaASEJ, int superficiesASEJ, String estadoASEJ) {
        return new OdontogramaCanonicoASEJ.LineaASEJ(idASEJ, codigoASEJ, piezaASEJ, null, superficiesASEJ, estadoASEJ, null,
                LocalDateTime.of(2026, 3, 2, 9, 0), 7);
    }

    private static OdontogramaCanonicoASEJ odontogramaASEJ(long idASEJ, String especificacionesASEJ, List<OdontogramaCanonicoASEJ.LineaASEJ> lineasASEJ,
                                                   String hashAnteriorASEJ, String hashCierreASEJ) {
        return new OdontogramaCanonicoASEJ(idASEJ, 3, "INICIAL", "PERMANENTE", CIERREASEJ, 7, especificacionesASEJ, lineasASEJ,
                hashAnteriorASEJ, hashCierreASEJ);
    }

    private static List<OdontogramaCanonicoASEJ> cadenaASEJ() {
        OdontogramaCanonicoASEJ aASEJ = odontogramaASEJ(1, "Inicial", List.of(lineaASEJ(10, "CARIES", 36, 1, null)), CadenaIntegridadASEJ.GENESISASEJ, null);
        String hashAASEJ = CadenaIntegridadASEJ.calcularASEJ(CadenaIntegridadASEJ.GENESISASEJ, aASEJ);
        aASEJ = odontogramaASEJ(1, "Inicial", aASEJ.hallazgosASEJ(), CadenaIntegridadASEJ.GENESISASEJ, hashAASEJ);

        OdontogramaCanonicoASEJ bASEJ = odontogramaASEJ(2, "Evolución", List.of(
                lineaASEJ(21, "RESTAURACION_DEFINITIVA", 36, 1, "BUENO"),
                lineaASEJ(20, "SELLANTE", 46, 1, "BUENO")), hashAASEJ, null);
        String hashBASEJ = CadenaIntegridadASEJ.calcularASEJ(hashAASEJ, bASEJ);
        bASEJ = odontogramaASEJ(2, "Evolución", bASEJ.hallazgosASEJ(), hashAASEJ, hashBASEJ);
        return List.of(aASEJ, bASEJ);
    }

    @Test
    void elFormatoCanonicoEsElDocumentadoASEJ() throws Exception {
        OdontogramaCanonicoASEJ oASEJ = odontogramaASEJ(5, "Paciente colaborador", List.of(
                new OdontogramaCanonicoASEJ.LineaASEJ(9, "PROTESIS_FIJA", 14, 16, 0, "MALO", "PF",
                        LocalDateTime.of(2026, 3, 1, 8, 30), 2),
                lineaASEJ(8, "CARIES", 36, 7, null)), "GENESIS", null);

        String esperadoASEJ = """
                GENESIS
                5|3|INICIAL|PERMANENTE|2026-03-02T10:15:30|7|Paciente colaborador
                8|CARIES|36|\\N|7|\\N|\\N|2026-03-02T09:00:00|7
                9|PROTESIS_FIJA|14|16|0|MALO|PF|2026-03-01T08:30:00|2""";
        String shaASEJ = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(esperadoASEJ.getBytes(StandardCharsets.UTF_8)));

        assertThat(CadenaIntegridadASEJ.calcularASEJ("GENESIS", oASEJ)).isEqualTo(shaASEJ).hasSize(64);
    }

    @Test
    void elHashEsDeterministaYNoDependeDelOrdenDeLasLineasASEJ() {
        OdontogramaCanonicoASEJ xASEJ = odontogramaASEJ(1, "e", List.of(lineaASEJ(1, "A", 11, 0, null), lineaASEJ(2, "B", 12, 0, null)), "G", null);
        OdontogramaCanonicoASEJ yASEJ = odontogramaASEJ(1, "e", List.of(lineaASEJ(2, "B", 12, 0, null), lineaASEJ(1, "A", 11, 0, null)), "G", null);
        assertThat(CadenaIntegridadASEJ.calcularASEJ("G", xASEJ)).isEqualTo(CadenaIntegridadASEJ.calcularASEJ("G", yASEJ));
    }

    @Test
    void elHashDependeDelAnteriorASEJ() {
        OdontogramaCanonicoASEJ oASEJ = odontogramaASEJ(1, "e", List.of(), "G", null);
        assertThat(CadenaIntegridadASEJ.calcularASEJ("A", oASEJ)).isNotEqualTo(CadenaIntegridadASEJ.calcularASEJ("B", oASEJ));
    }

    @Test
    void elEscapeEvitaColisionesEntreCamposASEJ() {
        OdontogramaCanonicoASEJ conBarraASEJ = odontogramaASEJ(1, "a|b", List.of(), "G", null);
        OdontogramaCanonicoASEJ conSaltoASEJ = odontogramaASEJ(1, "a\nb", List.of(), "G", null);
        OdontogramaCanonicoASEJ nuloASEJ = odontogramaASEJ(1, null, List.of(), "G", null);
        OdontogramaCanonicoASEJ textoNASEJ = odontogramaASEJ(1, "\\N", List.of(), "G", null);
        assertThat(CadenaIntegridadASEJ.cabeceraASEJ(conBarraASEJ)).endsWith("|a\\|b");
        assertThat(CadenaIntegridadASEJ.cabeceraASEJ(conSaltoASEJ)).endsWith("|a\\nb");
        assertThat(CadenaIntegridadASEJ.calcularASEJ("G", nuloASEJ)).isNotEqualTo(CadenaIntegridadASEJ.calcularASEJ("G", textoNASEJ));
    }

    @Test
    void cadenaIntactaEsIntegraASEJ() {
        assertThat(CadenaIntegridadASEJ.verificarASEJ(cadenaASEJ())).isEqualTo(ResultadoVerificacionASEJ.correctaASEJ());
        assertThat(CadenaIntegridadASEJ.verificarASEJ(List.of()).integraASEJ()).isTrue();
    }

    @Test
    void detectaHallazgoAlteradoEnLaBaseASEJ() {
        List<OdontogramaCanonicoASEJ> cASEJ = cadenaASEJ();
        OdontogramaCanonicoASEJ aASEJ = cASEJ.get(0);
        OdontogramaCanonicoASEJ alteradoASEJ = odontogramaASEJ(1, "Inicial",
                List.of(lineaASEJ(10, "CARIES", 36, 3, null)),
                aASEJ.hashAnteriorASEJ(), aASEJ.hashCierreASEJ());

        ResultadoVerificacionASEJ rASEJ = CadenaIntegridadASEJ.verificarASEJ(List.of(alteradoASEJ, cASEJ.get(1)));

        assertThat(rASEJ.integraASEJ()).isFalse();
        assertThat(rASEJ.odontogramaIdInvalidoASEJ()).isEqualTo(1L);
        assertThat(rASEJ.motivoASEJ()).contains("contenido");
    }

    @Test
    void detectaEspecificacionesAlteradasEnUnOdontogramaPosteriorASEJ() {
        List<OdontogramaCanonicoASEJ> cASEJ = cadenaASEJ();
        OdontogramaCanonicoASEJ bASEJ = cASEJ.get(1);
        OdontogramaCanonicoASEJ alteradoASEJ = odontogramaASEJ(2, "Evolución editada", bASEJ.hallazgosASEJ(), bASEJ.hashAnteriorASEJ(), bASEJ.hashCierreASEJ());

        ResultadoVerificacionASEJ rASEJ = CadenaIntegridadASEJ.verificarASEJ(List.of(cASEJ.get(0), alteradoASEJ));

        assertThat(rASEJ.odontogramaIdInvalidoASEJ()).isEqualTo(2L);
    }

    @Test
    void detectaUnOdontogramaEliminadoDeLaCadenaASEJ() {
        ResultadoVerificacionASEJ rASEJ = CadenaIntegridadASEJ.verificarASEJ(List.of(cadenaASEJ().get(1)));
        assertThat(rASEJ.integraASEJ()).isFalse();
        assertThat(rASEJ.odontogramaIdInvalidoASEJ()).isEqualTo(2L);
        assertThat(rASEJ.motivoASEJ()).contains("enlace");
    }
}
