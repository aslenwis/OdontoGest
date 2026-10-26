package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.dominio.odontograma.CadenaIntegridadASEJ;
import com.odontogest.dominio.odontograma.ClaveCapaASEJ;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.ResultadoVerificacionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.EstadoAtencionASEJ;
import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;
import com.odontogest.servicio.dto.AtencionFormASEJ;
import com.odontogest.servicio.dto.DiferenciaVistaASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.OdontogramaResumenASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OdontogramaServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private AtencionServiceASEJ atencionServiceASEJ;
    @Autowired private OdontogramaServiceASEJ odontogramaServiceASEJ;
    @Autowired private IntegridadServiceASEJ integridadServiceASEJ;
    @Autowired private CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private Map<String, Long> tiposASEJ;
    private long pacienteIdASEJ;

    @BeforeEach
    void prepararASEJ() {
        iniciarSesionASEJ("odontologo");
        tiposASEJ = catalogoServiceASEJ.listarTiposASEJ(true).stream().collect(Collectors.toMap(HallazgoTipoDtoASEJ::codigoASEJ, HallazgoTipoDtoASEJ::idASEJ));
        pacienteIdASEJ = pacienteASEJ(35);
    }

    private long pacienteASEJ(int edadASEJ) {
        return pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Paciente", "Odonto" + sufijoASEJ(),
                null, LocalDate.now().minusYears(edadASEJ), SexoASEJ.F, null, null, null, null, null, null,
                edadASEJ < 18 ? "Apoderado Prueba" : null, edadASEJ < 18 ? "DNI 40000000" : null, edadASEJ < 18 ? "Madre" : null, true));
    }

    private long abrirAtencionASEJ(long pacienteASEJ) {
        return atencionServiceASEJ.abrirASEJ(new AtencionFormASEJ(null, pacienteASEJ, null, "Control", null, null, null, null));
    }

    private long borradorASEJ() {
        return odontogramaServiceASEJ.obtenerVistaASEJ(pacienteIdASEJ, null).odontogramaIdASEJ();
    }

    private OdontogramaVistaASEJ registrarASEJ(long odontogramaASEJ, String tipoASEJ, int piezaASEJ, Integer finASEJ, Set<SuperficieASEJ> supASEJ,
                                       EstadoHallazgoASEJ estadoASEJ, String siglaASEJ) {
        return odontogramaServiceASEJ.registrarHallazgoASEJ(new NuevoHallazgoASEJ(odontogramaASEJ, tiposASEJ.get(tipoASEJ), piezaASEJ, finASEJ, supASEJ, estadoASEJ, siglaASEJ, null));
    }

    private static HallazgoDtoASEJ unicoASEJ(OdontogramaVistaASEJ vASEJ, String codigoASEJ) {
        return vASEJ.historialASEJ().stream().filter(hASEJ -> hASEJ.codigoTipoASEJ().equals(codigoASEJ)).findFirst().orElseThrow();
    }

    @Test
    void abrirAtencionCreaElOdontogramaInicialEnBorradorASEJ() {
        long atencionASEJ = abrirAtencionASEJ(pacienteIdASEJ);
        OdontogramaVistaASEJ vASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pacienteIdASEJ, null);

        assertThat(vASEJ.tipoASEJ()).isEqualTo(TipoOdontogramaASEJ.INICIAL);
        assertThat(vASEJ.borradorASEJ()).isTrue();
        assertThat(vASEJ.editableASEJ()).isTrue();
        assertThat(vASEJ.atencionIdASEJ()).isEqualTo(atencionASEJ);
        assertThat(vASEJ.denticionASEJ()).isEqualTo(DenticionASEJ.PERMANENTE);
        assertThat(atencionServiceASEJ.obtenerASEJ(atencionASEJ).odontologoNombreASEJ()).isEqualTo("Carlos Alberto Quispe Mamani");

        assertThatThrownBy(() -> abrirAtencionASEJ(pacienteIdASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ya tiene una atención abierta");
    }

    @Test
    void laDenticionSugeridaDependeDeLaEdadASEJ() {
        long ninoASEJ = pacienteASEJ(4);
        abrirAtencionASEJ(ninoASEJ);
        assertThat(odontogramaServiceASEJ.obtenerVistaASEJ(ninoASEJ, null).denticionASEJ()).isEqualTo(DenticionASEJ.DECIDUA);
        long escolarASEJ = pacienteASEJ(9);
        assertThat(odontogramaServiceASEJ.obtenerVistaASEJ(escolarASEJ, null).odontogramaIdASEJ()).as("sin atención aún").isNull();
        assertThat(odontogramaServiceASEJ.obtenerVistaASEJ(escolarASEJ, null).denticionASEJ()).isEqualTo(DenticionASEJ.MIXTA);
    }

    @Test
    void registraLosCuatroTiposDeRepresentacionASEJ() {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        registrarASEJ(oASEJ, "CARIES", 36, null, Set.of(SuperficieASEJ.O, SuperficieASEJ.M), null, "cd");
        registrarASEJ(oASEJ, "CORONA", 11, null, Set.of(), EstadoHallazgoASEJ.BUENO, "CMC");
        registrarASEJ(oASEJ, "DIASTEMA", 11, 21, Set.of(), null, null);
        OdontogramaVistaASEJ vASEJ = registrarASEJ(oASEJ, "PROTESIS_FIJA", 16, 14, Set.of(), EstadoHallazgoASEJ.MALO, null);

        assertThat(vASEJ.historialASEJ()).hasSize(4).allMatch(HallazgoDtoASEJ::vigenteASEJ).allMatch(HallazgoDtoASEJ::eliminableASEJ);
        assertThat(unicoASEJ(vASEJ, "CARIES").siglaASEJ()).isEqualTo("CD");
        assertThat(vASEJ.estadoBocaASEJ().enSuperficieASEJ(36, SuperficieASEJ.M, "SUPERFICIE")).isPresent();
        assertThat(vASEJ.estadoBocaASEJ().rangosASEJ()).hasSize(2);
        assertThat(vASEJ.estadoBocaASEJ().clavesASEJ()).contains(new ClaveCapaASEJ(14, 16, null, "PROTESIS_FIJA"));
        assertThat(vASEJ.indicesASEJ().cpodASEJ().cASEJ()).isEqualTo(1);
        assertThat(vASEJ.indicesASEJ().cpodASEJ().oASEJ()).isEqualTo(1);
        assertThat(unicoASEJ(vASEJ, "CORONA").registradoPorASEJ()).isEqualTo("Carlos Alberto Quispe Mamani");
    }

    @Test
    void validaElRegistroSegunElPlanASEJ() {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "CARIES", 36, null, Set.of(), null, "CD"))
                .hasMessageContaining("al menos una superficie");
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "CORONA", 36, null, Set.of(SuperficieASEJ.O), null, "XX"))
                .hasMessageContaining("sin superficies").hasMessageContaining("estado").hasMessageContaining("CM, CF, CMC");
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "DIASTEMA", 11, 22, Set.of(), null, null))
                .hasMessageContaining("contiguas");
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "PROTESIS_FIJA", 16, 46, Set.of(), EstadoHallazgoASEJ.BUENO, null))
                .hasMessageContaining("misma arcada");
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "PIEZA_EN_ERUPCION", 55, null, Set.of(), null, null))
                .hasMessageContaining("no pertenece a la dentición");
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "CARIES", 99, null, Set.of(SuperficieASEJ.O), null, "CD"))
                .hasMessageContaining("no existe");

        registrarASEJ(oASEJ, "PIEZA_AUSENTE_DEX", 46, null, Set.of(), null, null);
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "CARIES", 46, null, Set.of(SuperficieASEJ.O), null, "CD"))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessage("La pieza 46 figura como ausente: no se pueden registrar hallazgos en sus superficies.");
    }

    @Test
    void edentuloTotalCuentaYBloqueaSuperficiesASEJ() {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        OdontogramaVistaASEJ vASEJ = registrarASEJ(oASEJ, "EDENTULO_TOTAL", 18, 28, Set.of(), null, null);
        assertThat(vASEJ.indicesASEJ().cpodASEJ().pASEJ()).isEqualTo(14);
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "CARIES", 16, null, Set.of(SuperficieASEJ.O), null, "CD"))
                .hasMessageContaining("figura como ausente");
    }

    @Test
    void enBorradorSePuedeEliminarYCambiarLaDenticionASEJ() {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        OdontogramaVistaASEJ vASEJ = registrarASEJ(oASEJ, "FRACTURA", 21, null, Set.of(), null, null);
        assertThatThrownBy(() -> odontogramaServiceASEJ.cambiarDenticionASEJ(oASEJ, DenticionASEJ.DECIDUA))
                .hasMessageContaining("dentición elegida");
        assertThatThrownBy(() -> odontogramaServiceASEJ.anularHallazgoASEJ(vASEJ.historialASEJ().get(0).idASEJ(), "Error de registro"))
                .hasMessageContaining("elimine el hallazgo");

        assertThat(odontogramaServiceASEJ.eliminarHallazgoBorradorASEJ(vASEJ.historialASEJ().get(0).idASEJ()).historialASEJ()).isEmpty();
        assertThat(odontogramaServiceASEJ.cambiarDenticionASEJ(oASEJ, DenticionASEJ.MIXTA).denticionASEJ()).isEqualTo(DenticionASEJ.MIXTA);
        assertThat(odontogramaServiceASEJ.actualizarEspecificacionesASEJ(oASEJ, " Fluorosis leve ", "Paciente colaborador").especificacionesASEJ())
                .isEqualTo("Fluorosis leve");
    }

    @Test
    void cerrarFirmaConHashYElOdontogramaQuedaInmutableASEJ() {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        OdontogramaVistaASEJ conCariesASEJ = registrarASEJ(oASEJ, "CARIES", 36, null, Set.of(SuperficieASEJ.O), null, "CE");
        long cariesASEJ = conCariesASEJ.historialASEJ().get(0).idASEJ();

        OdontogramaVistaASEJ cerradoASEJ = odontogramaServiceASEJ.cerrarASEJ(oASEJ);

        assertThat(cerradoASEJ.estadoASEJ()).isEqualTo(EstadoOdontogramaRegistroASEJ.CERRADO);
        assertThat(cerradoASEJ.hashCierreASEJ()).matches("[0-9a-f]{64}");
        assertThat(cerradoASEJ.editableASEJ()).isFalse();
        assertThat(jdbcASEJ.queryForObject("SELECT hash_anterior FROM odontograma WHERE id = ?", String.class, oASEJ))
                .isEqualTo(CadenaIntegridadASEJ.GENESISASEJ);
        assertThat(integridadServiceASEJ.verificarASEJ(pacienteIdASEJ).integraASEJ()).isTrue();

        assertThatThrownBy(() -> registrarASEJ(oASEJ, "FRACTURA", 21, null, Set.of(), null, null)).hasMessageContaining("cerrado y firmado");
        assertThatThrownBy(() -> odontogramaServiceASEJ.actualizarEspecificacionesASEJ(oASEJ, "x", null)).hasMessageContaining("cerrado");
        assertThatThrownBy(() -> odontogramaServiceASEJ.eliminarHallazgoBorradorASEJ(cariesASEJ)).hasMessageContaining("solo anular");
        assertThatThrownBy(() -> odontogramaServiceASEJ.cerrarASEJ(oASEJ)).hasMessageContaining("cerrado");
    }

    @Test
    void anularConMotivoDejaElHallazgoEnElHistorialASEJ() {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        long cariesASEJ = registrarASEJ(oASEJ, "CARIES", 36, null, Set.of(SuperficieASEJ.O), null, "CE").historialASEJ().get(0).idASEJ();
        odontogramaServiceASEJ.cerrarASEJ(oASEJ);

        assertThatThrownBy(() -> odontogramaServiceASEJ.anularHallazgoASEJ(cariesASEJ, "corto")).hasMessageContaining("al menos 10");
        OdontogramaVistaASEJ vASEJ = odontogramaServiceASEJ.anularHallazgoASEJ(cariesASEJ, "Se registró en la pieza equivocada");

        HallazgoDtoASEJ anuladoASEJ = vASEJ.historialASEJ().get(0);
        assertThat(anuladoASEJ.anuladoASEJ()).isTrue();
        assertThat(anuladoASEJ.vigenteASEJ()).isFalse();
        assertThat(anuladoASEJ.motivoAnulacionASEJ()).isEqualTo("Se registró en la pieza equivocada");
        assertThat(vASEJ.estadoBocaASEJ().estaVacioASEJ()).isTrue();
        assertThat(vASEJ.indicesASEJ().cpodASEJ().totalASEJ()).isZero();
        assertThat(integridadServiceASEJ.verificarASEJ(pacienteIdASEJ).integraASEJ()).as("la anulación no altera el hash").isTrue();
        assertThatThrownBy(() -> odontogramaServiceASEJ.anularHallazgoASEJ(cariesASEJ, "Otro motivo cualquiera")).hasMessageContaining("ya está anulado");
    }

    @Test
    void evolucionEncadenaElHashYLaLineaDeTiempoReconstruyeElInicialASEJ() {
        long atencion1ASEJ = abrirAtencionASEJ(pacienteIdASEJ);
        long inicialASEJ = borradorASEJ();
        registrarASEJ(inicialASEJ, "CARIES", 36, null, Set.of(SuperficieASEJ.O), null, "CD");
        String hashInicialASEJ = odontogramaServiceASEJ.cerrarASEJ(inicialASEJ).hashCierreASEJ();
        atencionServiceASEJ.cerrarASEJ(atencion1ASEJ);

        abrirAtencionASEJ(pacienteIdASEJ);
        long evolucionASEJ = borradorASEJ();
        assertThat(evolucionASEJ).isNotEqualTo(inicialASEJ);
        OdontogramaVistaASEJ actualASEJ = registrarASEJ(evolucionASEJ, "RESTAURACION_DEFINITIVA", 36, null, Set.of(SuperficieASEJ.O), EstadoHallazgoASEJ.BUENO, "R");
        assertThat(actualASEJ.tipoASEJ()).isEqualTo(TipoOdontogramaASEJ.EVOLUCION);
        assertThat(actualASEJ.estadoBocaASEJ().enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE").orElseThrow().tipoASEJ().codigoASEJ())
                .isEqualTo("RESTAURACION_DEFINITIVA");
        assertThat(actualASEJ.indicesASEJ().cpodASEJ().oASEJ()).isEqualTo(1);
        assertThat(actualASEJ.historialASEJ().stream().filter(hASEJ -> !hASEJ.vigenteASEJ())).as("la caries quedó reemplazada").hasSize(1);

        List<OdontogramaResumenASEJ> lineaASEJ = odontogramaServiceASEJ.listarPorPacienteASEJ(pacienteIdASEJ);
        assertThat(lineaASEJ).extracting(OdontogramaResumenASEJ::tipoASEJ).containsExactly(TipoOdontogramaASEJ.INICIAL, TipoOdontogramaASEJ.EVOLUCION);
        assertThat(lineaASEJ.get(0).etiquetaASEJ()).startsWith("Inicial · ");
        assertThat(lineaASEJ.get(1).etiquetaASEJ()).isEqualTo("Evolución · borrador");

        OdontogramaVistaASEJ pasadoASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pacienteIdASEJ, lineaASEJ.get(0).cerradoEnASEJ());
        assertThat(pasadoASEJ.tipoASEJ()).isEqualTo(TipoOdontogramaASEJ.INICIAL);
        assertThat(pasadoASEJ.editableASEJ()).isFalse();
        assertThat(pasadoASEJ.estadoBocaASEJ().enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE").orElseThrow().tipoASEJ().codigoASEJ()).isEqualTo("CARIES");
        assertThat(pasadoASEJ.historialASEJ()).hasSize(1);

        DiferenciaVistaASEJ dASEJ = odontogramaServiceASEJ.compararASEJ(pacienteIdASEJ, lineaASEJ.get(0).cerradoEnASEJ(), null);
        assertThat(dASEJ.diferenciaASEJ().modificadasASEJ()).containsExactly(new ClaveCapaASEJ(36, 36, SuperficieASEJ.O, "SUPERFICIE"));
        assertThat(dASEJ.diferenciaASEJ().piezasAfectadasASEJ()).containsExactly(36);

        odontogramaServiceASEJ.cerrarASEJ(evolucionASEJ);
        assertThat(jdbcASEJ.queryForObject("SELECT hash_anterior FROM odontograma WHERE id = ?", String.class, evolucionASEJ))
                .isEqualTo(hashInicialASEJ);
        assertThat(integridadServiceASEJ.verificarASEJ(pacienteIdASEJ).integraASEJ()).isTrue();

        jdbcASEJ.update("UPDATE hallazgo SET superficies = 3 WHERE odontograma_id = ?", inicialASEJ);
        ResultadoVerificacionASEJ rASEJ = integridadServiceASEJ.verificarASEJ(pacienteIdASEJ);
        assertThat(rASEJ.integraASEJ()).isFalse();
        assertThat(rASEJ.odontogramaIdInvalidoASEJ()).isEqualTo(inicialASEJ);
    }

    @Test
    void cerrarLaAtencionExigeElOdontogramaFirmadoASEJ() {
        long atencionASEJ = abrirAtencionASEJ(pacienteIdASEJ);
        assertThatThrownBy(() -> atencionServiceASEJ.cerrarASEJ(atencionASEJ)).hasMessageContaining("Cierre y firme el odontograma");
        odontogramaServiceASEJ.cerrarASEJ(borradorASEJ());
        assertThat(atencionServiceASEJ.cerrarASEJ(atencionASEJ).estadoASEJ()).isEqualTo(EstadoAtencionASEJ.CERRADA);
        assertThatThrownBy(() -> atencionServiceASEJ.actualizarASEJ(new AtencionFormASEJ(atencionASEJ, pacienteIdASEJ, null, "x", null, null, null, null)))
                .hasMessageContaining("solo lectura");

        long otraASEJ = abrirAtencionASEJ(pacienteIdASEJ);
        assertThat(odontogramaServiceASEJ.obtenerVistaASEJ(pacienteIdASEJ, null).tipoASEJ()).isEqualTo(TipoOdontogramaASEJ.EVOLUCION);
        atencionServiceASEJ.cerrarASEJ(otraASEJ);
        assertThat(odontogramaServiceASEJ.listarPorPacienteASEJ(pacienteIdASEJ)).as("evolución vacía descartada").hasSize(1);
        assertThat(atencionServiceASEJ.listarPorPacienteASEJ(pacienteIdASEJ)).hasSize(2);
    }

    @Test
    void permisosDeAtencionYOdontogramaASEJ() {
        iniciarSesionASEJ("recepcion");
        assertThatThrownBy(() -> abrirAtencionASEJ(pacienteIdASEJ)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        iniciarSesionASEJ("admin");
        assertThatThrownBy(() -> abrirAtencionASEJ(pacienteIdASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("el odontólogo que atiende");
        long odontologoASEJ = jdbcASEJ.queryForObject("SELECT MIN(id) FROM odontologo", Long.class);
        atencionServiceASEJ.abrirASEJ(new AtencionFormASEJ(null, pacienteIdASEJ, odontologoASEJ, "Urgencia", null, null, null, DenticionASEJ.MIXTA));
        assertThat(odontogramaServiceASEJ.obtenerVistaASEJ(pacienteIdASEJ, null).denticionASEJ()).isEqualTo(DenticionASEJ.MIXTA);

        iniciarSesionASEJ("recepcion");
        long oASEJ = borradorASEJ();
        assertThatThrownBy(() -> registrarASEJ(oASEJ, "FRACTURA", 21, null, Set.of(), null, null))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }

    @Test
    void generaLaFichaOdontologicaEnPdfASEJ() throws Exception {
        abrirAtencionASEJ(pacienteIdASEJ);
        long oASEJ = borradorASEJ();
        registrarASEJ(oASEJ, "CARIES", 36, null, Set.of(SuperficieASEJ.O, SuperficieASEJ.D), null, "CDP");
        odontogramaServiceASEJ.cerrarASEJ(oASEJ);

        BufferedImage imgASEJ = new BufferedImage(160, 90, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream pngASEJ = new ByteArrayOutputStream();
        ImageIO.write(imgASEJ, "png", pngASEJ);
        byte[] pdfASEJ = odontogramaServiceASEJ.pdfFichaASEJ(pacienteIdASEJ, null, pngASEJ.toByteArray());

        assertThat(new String(pdfASEJ, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdfASEJ.length).isGreaterThan(2000);
        assertThatThrownBy(() -> odontogramaServiceASEJ.pdfFichaASEJ(pacienteASEJ(30), null, null)).hasMessageContaining("aún no tiene odontograma");
    }
}
