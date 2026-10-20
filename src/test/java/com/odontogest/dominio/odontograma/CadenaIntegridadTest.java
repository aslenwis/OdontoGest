/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hash SHA-256 encadenado de odontogramas cerrados.
 */
class CadenaIntegridadTest {

    private static final LocalDateTime CIERRE = LocalDateTime.of(2026, 3, 2, 10, 15, 30);

    private static OdontogramaCanonico.Linea linea(long id, String codigo, int pieza, int superficies, String estado) {
        return new OdontogramaCanonico.Linea(id, codigo, pieza, null, superficies, estado, null,
                LocalDateTime.of(2026, 3, 2, 9, 0), 7);
    }

    private static OdontogramaCanonico odontograma(long id, String especificaciones, List<OdontogramaCanonico.Linea> lineas,
                                                   String hashAnterior, String hashCierre) {
        return new OdontogramaCanonico(id, 3, "INICIAL", "PERMANENTE", CIERRE, 7, especificaciones, lineas,
                hashAnterior, hashCierre);
    }

    /** Cierra una cadena de odontogramas como lo hará OdontogramaService. */
    private static List<OdontogramaCanonico> cadena() {
        OdontogramaCanonico a = odontograma(1, "Inicial", List.of(linea(10, "CARIES", 36, 1, null)), CadenaIntegridad.GENESIS, null);
        String hashA = CadenaIntegridad.calcular(CadenaIntegridad.GENESIS, a);
        a = odontograma(1, "Inicial", a.hallazgos(), CadenaIntegridad.GENESIS, hashA);

        OdontogramaCanonico b = odontograma(2, "Evolución", List.of(
                linea(21, "RESTAURACION_DEFINITIVA", 36, 1, "BUENO"),
                linea(20, "SELLANTE", 46, 1, "BUENO")), hashA, null);
        String hashB = CadenaIntegridad.calcular(hashA, b);
        b = odontograma(2, "Evolución", b.hallazgos(), hashA, hashB);
        return List.of(a, b);
    }

    @Test
    void elFormatoCanonicoEsElDocumentado() throws Exception {
        OdontogramaCanonico o = odontograma(5, "Paciente colaborador", List.of(
                new OdontogramaCanonico.Linea(9, "PROTESIS_FIJA", 14, 16, 0, "MALO", "PF",
                        LocalDateTime.of(2026, 3, 1, 8, 30), 2),
                linea(8, "CARIES", 36, 7, null)), "GENESIS", null);

        String esperado = """
                GENESIS
                5|3|INICIAL|PERMANENTE|2026-03-02T10:15:30|7|Paciente colaborador
                8|CARIES|36|\\N|7|\\N|\\N|2026-03-02T09:00:00|7
                9|PROTESIS_FIJA|14|16|0|MALO|PF|2026-03-01T08:30:00|2""";
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(esperado.getBytes(StandardCharsets.UTF_8)));

        assertThat(CadenaIntegridad.calcular("GENESIS", o)).isEqualTo(sha).hasSize(64);
    }

    @Test
    void elHashEsDeterministaYNoDependeDelOrdenDeLasLineas() {
        OdontogramaCanonico x = odontograma(1, "e", List.of(linea(1, "A", 11, 0, null), linea(2, "B", 12, 0, null)), "G", null);
        OdontogramaCanonico y = odontograma(1, "e", List.of(linea(2, "B", 12, 0, null), linea(1, "A", 11, 0, null)), "G", null);
        assertThat(CadenaIntegridad.calcular("G", x)).isEqualTo(CadenaIntegridad.calcular("G", y));
    }

    @Test
    void elHashDependeDelAnterior() {
        OdontogramaCanonico o = odontograma(1, "e", List.of(), "G", null);
        assertThat(CadenaIntegridad.calcular("A", o)).isNotEqualTo(CadenaIntegridad.calcular("B", o));
    }

    @Test
    void elEscapeEvitaColisionesEntreCampos() {
        OdontogramaCanonico conBarra = odontograma(1, "a|b", List.of(), "G", null);
        OdontogramaCanonico conSalto = odontograma(1, "a\nb", List.of(), "G", null);
        OdontogramaCanonico nulo = odontograma(1, null, List.of(), "G", null);
        OdontogramaCanonico textoN = odontograma(1, "\\N", List.of(), "G", null);
        assertThat(CadenaIntegridad.cabecera(conBarra)).endsWith("|a\\|b");
        assertThat(CadenaIntegridad.cabecera(conSalto)).endsWith("|a\\nb");
        assertThat(CadenaIntegridad.calcular("G", nulo)).isNotEqualTo(CadenaIntegridad.calcular("G", textoN));
    }

    @Test
    void cadenaIntactaEsIntegra() {
        assertThat(CadenaIntegridad.verificar(cadena())).isEqualTo(ResultadoVerificacion.correcta());
        assertThat(CadenaIntegridad.verificar(List.of()).integra()).isTrue();
    }

    @Test
    void detectaHallazgoAlteradoEnLaBase() {
        List<OdontogramaCanonico> c = cadena();
        OdontogramaCanonico a = c.get(0);
        OdontogramaCanonico alterado = odontograma(1, "Inicial",
                List.of(linea(10, "CARIES", 36, 3, null)),                           // O → O+M
                a.hashAnterior(), a.hashCierre());

        ResultadoVerificacion r = CadenaIntegridad.verificar(List.of(alterado, c.get(1)));

        assertThat(r.integra()).isFalse();
        assertThat(r.odontogramaIdInvalido()).isEqualTo(1L);
        assertThat(r.motivo()).contains("contenido");
    }

    @Test
    void detectaEspecificacionesAlteradasEnUnOdontogramaPosterior() {
        List<OdontogramaCanonico> c = cadena();
        OdontogramaCanonico b = c.get(1);
        OdontogramaCanonico alterado = odontograma(2, "Evolución editada", b.hallazgos(), b.hashAnterior(), b.hashCierre());

        ResultadoVerificacion r = CadenaIntegridad.verificar(List.of(c.get(0), alterado));

        assertThat(r.odontogramaIdInvalido()).isEqualTo(2L);
    }

    @Test
    void detectaUnOdontogramaEliminadoDeLaCadena() {
        ResultadoVerificacion r = CadenaIntegridad.verificar(List.of(cadena().get(1)));
        assertThat(r.integra()).isFalse();
        assertThat(r.odontogramaIdInvalido()).isEqualTo(2L);
        assertThat(r.motivo()).contains("enlace");
    }
}
