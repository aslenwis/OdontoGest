/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.dominio.odontograma.CadenaIntegridad;
import com.odontogest.dominio.odontograma.ClaveCapa;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.ResultadoVerificacion;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.EstadoAtencion;
import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.modelo.TipoOdontograma;
import com.odontogest.servicio.dto.AtencionForm;
import com.odontogest.servicio.dto.DiferenciaVista;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.OdontogramaResumen;
import com.odontogest.servicio.dto.OdontogramaVista;
import com.odontogest.servicio.dto.PacienteForm;
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

/**
 * Atenciones y odontograma: registro de los 4 tipos de representación, validaciones, cierre con hash, anulación,
 * línea de tiempo, comparación, índices, integridad y PDF (Fase 3).
 */
class OdontogramaServiceTest extends PruebaIntegracion {

    @Autowired private PacienteService pacienteService;
    @Autowired private AtencionService atencionService;
    @Autowired private OdontogramaService odontogramaService;
    @Autowired private IntegridadService integridadService;
    @Autowired private CatalogoHallazgoService catalogoService;
    @Autowired private JdbcTemplate jdbc;

    private Map<String, Long> tipos;
    private long pacienteId;

    @BeforeEach
    void preparar() {
        iniciarSesion("odontologo");
        tipos = catalogoService.listarTipos(true).stream().collect(Collectors.toMap(HallazgoTipoDto::codigo, HallazgoTipoDto::id));
        pacienteId = paciente(35);
    }

    private long paciente(int edad) {
        return pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Paciente", "Odonto" + sufijo(),
                null, LocalDate.now().minusYears(edad), Sexo.F, null, null, null, null, null, null,
                edad < 18 ? "Apoderado Prueba" : null, edad < 18 ? "DNI 40000000" : null, edad < 18 ? "Madre" : null, true));
    }

    private long abrirAtencion(long paciente) {
        return atencionService.abrir(new AtencionForm(null, paciente, null, "Control", null, null, null, null));
    }

    private long borrador() {
        return odontogramaService.obtenerVista(pacienteId, null).odontogramaId();
    }

    private OdontogramaVista registrar(long odontograma, String tipo, int pieza, Integer fin, Set<Superficie> sup,
                                       EstadoHallazgo estado, String sigla) {
        return odontogramaService.registrarHallazgo(new NuevoHallazgo(odontograma, tipos.get(tipo), pieza, fin, sup, estado, sigla, null));
    }

    private static HallazgoDto unico(OdontogramaVista v, String codigo) {
        return v.historial().stream().filter(h -> h.codigoTipo().equals(codigo)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ atención y borrador

    @Test
    void abrirAtencionCreaElOdontogramaInicialEnBorrador() {
        long atencion = abrirAtencion(pacienteId);
        OdontogramaVista v = odontogramaService.obtenerVista(pacienteId, null);

        assertThat(v.tipo()).isEqualTo(TipoOdontograma.INICIAL);
        assertThat(v.borrador()).isTrue();
        assertThat(v.editable()).isTrue();
        assertThat(v.atencionId()).isEqualTo(atencion);
        assertThat(v.denticion()).isEqualTo(Denticion.PERMANENTE);
        assertThat(atencionService.obtener(atencion).odontologoNombre()).isEqualTo("Carlos Alberto Quispe Mamani");

        assertThatThrownBy(() -> abrirAtencion(pacienteId))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya tiene una atención abierta");
    }

    @Test
    void laDenticionSugeridaDependeDeLaEdad() {
        long nino = paciente(4);
        abrirAtencion(nino);
        assertThat(odontogramaService.obtenerVista(nino, null).denticion()).isEqualTo(Denticion.DECIDUA);
        long escolar = paciente(9);
        assertThat(odontogramaService.obtenerVista(escolar, null).odontogramaId()).as("sin atención aún").isNull();
        assertThat(odontogramaService.obtenerVista(escolar, null).denticion()).isEqualTo(Denticion.MIXTA);
    }

    // ------------------------------------------------------------------ registro

    @Test
    void registraLosCuatroTiposDeRepresentacion() {
        abrirAtencion(pacienteId);
        long o = borrador();
        registrar(o, "CARIES", 36, null, Set.of(Superficie.O, Superficie.M), null, "cd");
        registrar(o, "CORONA", 11, null, Set.of(), EstadoHallazgo.BUENO, "CMC");
        registrar(o, "DIASTEMA", 11, 21, Set.of(), null, null);
        OdontogramaVista v = registrar(o, "PROTESIS_FIJA", 16, 14, Set.of(), EstadoHallazgo.MALO, null);

        assertThat(v.historial()).hasSize(4).allMatch(HallazgoDto::vigente).allMatch(HallazgoDto::eliminable);
        assertThat(unico(v, "CARIES").sigla()).isEqualTo("CD");
        assertThat(v.estadoBoca().enSuperficie(36, Superficie.M, "SUPERFICIE")).isPresent();
        assertThat(v.estadoBoca().rangos()).hasSize(2);
        assertThat(v.estadoBoca().claves()).contains(new ClaveCapa(14, 16, null, "PROTESIS_FIJA"));
        assertThat(v.indices().cpod().c()).isEqualTo(1);
        assertThat(v.indices().cpod().o()).isEqualTo(1);
        assertThat(unico(v, "CORONA").registradoPor()).isEqualTo("Carlos Alberto Quispe Mamani");
    }

    @Test
    void validaElRegistroSegunElPlan() {
        abrirAtencion(pacienteId);
        long o = borrador();
        assertThatThrownBy(() -> registrar(o, "CARIES", 36, null, Set.of(), null, "CD"))
                .hasMessageContaining("al menos una superficie");
        assertThatThrownBy(() -> registrar(o, "CORONA", 36, null, Set.of(Superficie.O), null, "XX"))
                .hasMessageContaining("sin superficies").hasMessageContaining("estado").hasMessageContaining("CM, CF, CMC");
        assertThatThrownBy(() -> registrar(o, "DIASTEMA", 11, 22, Set.of(), null, null))
                .hasMessageContaining("contiguas");
        assertThatThrownBy(() -> registrar(o, "PROTESIS_FIJA", 16, 46, Set.of(), EstadoHallazgo.BUENO, null))
                .hasMessageContaining("misma arcada");
        assertThatThrownBy(() -> registrar(o, "PIEZA_EN_ERUPCION", 55, null, Set.of(), null, null))
                .hasMessageContaining("no pertenece a la dentición");
        assertThatThrownBy(() -> registrar(o, "CARIES", 99, null, Set.of(Superficie.O), null, "CD"))
                .hasMessageContaining("no existe");

        registrar(o, "PIEZA_AUSENTE_DEX", 46, null, Set.of(), null, null);
        assertThatThrownBy(() -> registrar(o, "CARIES", 46, null, Set.of(Superficie.O), null, "CD"))
                .isInstanceOf(NegocioException.class).hasMessage("La pieza 46 figura como ausente: no se pueden registrar hallazgos en sus superficies.");
    }

    @Test
    void edentuloTotalCuentaYBloqueaSuperficies() {
        abrirAtencion(pacienteId);
        long o = borrador();
        OdontogramaVista v = registrar(o, "EDENTULO_TOTAL", 18, 28, Set.of(), null, null);
        assertThat(v.indices().cpod().p()).isEqualTo(14);
        assertThatThrownBy(() -> registrar(o, "CARIES", 16, null, Set.of(Superficie.O), null, "CD"))
                .hasMessageContaining("figura como ausente");
    }

    @Test
    void enBorradorSePuedeEliminarYCambiarLaDenticion() {
        abrirAtencion(pacienteId);
        long o = borrador();
        OdontogramaVista v = registrar(o, "FRACTURA", 21, null, Set.of(), null, null);
        assertThatThrownBy(() -> odontogramaService.cambiarDenticion(o, Denticion.DECIDUA))
                .hasMessageContaining("dentición elegida");
        assertThatThrownBy(() -> odontogramaService.anularHallazgo(v.historial().get(0).id(), "Error de registro"))
                .hasMessageContaining("elimine el hallazgo");

        assertThat(odontogramaService.eliminarHallazgoBorrador(v.historial().get(0).id()).historial()).isEmpty();
        assertThat(odontogramaService.cambiarDenticion(o, Denticion.MIXTA).denticion()).isEqualTo(Denticion.MIXTA);
        assertThat(odontogramaService.actualizarEspecificaciones(o, " Fluorosis leve ", "Paciente colaborador").especificaciones())
                .isEqualTo("Fluorosis leve");
    }

    // ------------------------------------------------------------------ cierre, anulación e integridad

    @Test
    void cerrarFirmaConHashYElOdontogramaQuedaInmutable() {
        abrirAtencion(pacienteId);
        long o = borrador();
        OdontogramaVista conCaries = registrar(o, "CARIES", 36, null, Set.of(Superficie.O), null, "CE");
        long caries = conCaries.historial().get(0).id();

        OdontogramaVista cerrado = odontogramaService.cerrar(o);

        assertThat(cerrado.estado()).isEqualTo(EstadoOdontogramaRegistro.CERRADO);
        assertThat(cerrado.hashCierre()).matches("[0-9a-f]{64}");
        assertThat(cerrado.editable()).isFalse();
        assertThat(jdbc.queryForObject("SELECT hash_anterior FROM odontograma WHERE id = ?", String.class, o))
                .isEqualTo(CadenaIntegridad.GENESIS);
        assertThat(integridadService.verificar(pacienteId).integra()).isTrue();

        assertThatThrownBy(() -> registrar(o, "FRACTURA", 21, null, Set.of(), null, null)).hasMessageContaining("cerrado y firmado");
        assertThatThrownBy(() -> odontogramaService.actualizarEspecificaciones(o, "x", null)).hasMessageContaining("cerrado");
        assertThatThrownBy(() -> odontogramaService.eliminarHallazgoBorrador(caries)).hasMessageContaining("solo anular");
        assertThatThrownBy(() -> odontogramaService.cerrar(o)).hasMessageContaining("cerrado");
    }

    @Test
    void anularConMotivoDejaElHallazgoEnElHistorial() {
        abrirAtencion(pacienteId);
        long o = borrador();
        long caries = registrar(o, "CARIES", 36, null, Set.of(Superficie.O), null, "CE").historial().get(0).id();
        odontogramaService.cerrar(o);

        assertThatThrownBy(() -> odontogramaService.anularHallazgo(caries, "corto")).hasMessageContaining("al menos 10");
        OdontogramaVista v = odontogramaService.anularHallazgo(caries, "Se registró en la pieza equivocada");

        HallazgoDto anulado = v.historial().get(0);
        assertThat(anulado.anulado()).isTrue();
        assertThat(anulado.vigente()).isFalse();
        assertThat(anulado.motivoAnulacion()).isEqualTo("Se registró en la pieza equivocada");
        assertThat(v.estadoBoca().estaVacio()).isTrue();
        assertThat(v.indices().cpod().total()).isZero();
        assertThat(integridadService.verificar(pacienteId).integra()).as("la anulación no altera el hash").isTrue();
        assertThatThrownBy(() -> odontogramaService.anularHallazgo(caries, "Otro motivo cualquiera")).hasMessageContaining("ya está anulado");
    }

    @Test
    void evolucionEncadenaElHashYLaLineaDeTiempoReconstruyeElInicial() {
        long atencion1 = abrirAtencion(pacienteId);
        long inicial = borrador();
        registrar(inicial, "CARIES", 36, null, Set.of(Superficie.O), null, "CD");
        String hashInicial = odontogramaService.cerrar(inicial).hashCierre();
        atencionService.cerrar(atencion1);

        abrirAtencion(pacienteId);
        long evolucion = borrador();
        assertThat(evolucion).isNotEqualTo(inicial);
        OdontogramaVista actual = registrar(evolucion, "RESTAURACION_DEFINITIVA", 36, null, Set.of(Superficie.O), EstadoHallazgo.BUENO, "R");
        assertThat(actual.tipo()).isEqualTo(TipoOdontograma.EVOLUCION);
        assertThat(actual.estadoBoca().enSuperficie(36, Superficie.O, "SUPERFICIE").orElseThrow().tipo().codigo())
                .isEqualTo("RESTAURACION_DEFINITIVA");
        assertThat(actual.indices().cpod().o()).isEqualTo(1);
        assertThat(actual.historial().stream().filter(h -> !h.vigente())).as("la caries quedó reemplazada").hasSize(1);

        List<OdontogramaResumen> linea = odontogramaService.listarPorPaciente(pacienteId);
        assertThat(linea).extracting(OdontogramaResumen::tipo).containsExactly(TipoOdontograma.INICIAL, TipoOdontograma.EVOLUCION);
        assertThat(linea.get(0).etiqueta()).startsWith("Inicial · ");
        assertThat(linea.get(1).etiqueta()).isEqualTo("Evolución · borrador");

        OdontogramaVista pasado = odontogramaService.obtenerVista(pacienteId, linea.get(0).cerradoEn());
        assertThat(pasado.tipo()).isEqualTo(TipoOdontograma.INICIAL);
        assertThat(pasado.editable()).isFalse();
        assertThat(pasado.estadoBoca().enSuperficie(36, Superficie.O, "SUPERFICIE").orElseThrow().tipo().codigo()).isEqualTo("CARIES");
        assertThat(pasado.historial()).hasSize(1);

        DiferenciaVista d = odontogramaService.comparar(pacienteId, linea.get(0).cerradoEn(), null);
        assertThat(d.diferencia().modificadas()).containsExactly(new ClaveCapa(36, 36, Superficie.O, "SUPERFICIE"));
        assertThat(d.diferencia().piezasAfectadas()).containsExactly(36);

        odontogramaService.cerrar(evolucion);
        assertThat(jdbc.queryForObject("SELECT hash_anterior FROM odontograma WHERE id = ?", String.class, evolucion))
                .isEqualTo(hashInicial);
        assertThat(integridadService.verificar(pacienteId).integra()).isTrue();

        // Manipulación posterior de la BD: la cadena lo detecta en el odontograma inicial
        jdbc.update("UPDATE hallazgo SET superficies = 3 WHERE odontograma_id = ?", inicial);
        ResultadoVerificacion r = integridadService.verificar(pacienteId);
        assertThat(r.integra()).isFalse();
        assertThat(r.odontogramaIdInvalido()).isEqualTo(inicial);
    }

    @Test
    void cerrarLaAtencionExigeElOdontogramaFirmado() {
        long atencion = abrirAtencion(pacienteId);
        assertThatThrownBy(() -> atencionService.cerrar(atencion)).hasMessageContaining("Cierre y firme el odontograma");
        odontogramaService.cerrar(borrador());
        assertThat(atencionService.cerrar(atencion).estado()).isEqualTo(EstadoAtencion.CERRADA);
        assertThatThrownBy(() -> atencionService.actualizar(new AtencionForm(atencion, pacienteId, null, "x", null, null, null, null)))
                .hasMessageContaining("solo lectura");

        long otra = abrirAtencion(pacienteId);
        assertThat(odontogramaService.obtenerVista(pacienteId, null).tipo()).isEqualTo(TipoOdontograma.EVOLUCION);
        atencionService.cerrar(otra);
        assertThat(odontogramaService.listarPorPaciente(pacienteId)).as("evolución vacía descartada").hasSize(1);
        assertThat(atencionService.listarPorPaciente(pacienteId)).hasSize(2);
    }

    @Test
    void permisosDeAtencionYOdontograma() {
        iniciarSesion("recepcion");
        assertThatThrownBy(() -> abrirAtencion(pacienteId)).isInstanceOf(PermisoDenegadoException.class);
        iniciarSesion("admin");
        assertThatThrownBy(() -> abrirAtencion(pacienteId))
                .isInstanceOf(NegocioException.class).hasMessageContaining("el odontólogo que atiende");
        long odontologo = jdbc.queryForObject("SELECT MIN(id) FROM odontologo", Long.class);
        atencionService.abrir(new AtencionForm(null, pacienteId, odontologo, "Urgencia", null, null, null, Denticion.MIXTA));
        assertThat(odontogramaService.obtenerVista(pacienteId, null).denticion()).isEqualTo(Denticion.MIXTA);

        iniciarSesion("recepcion");
        long o = borrador();
        assertThatThrownBy(() -> registrar(o, "FRACTURA", 21, null, Set.of(), null, null))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    void generaLaFichaOdontologicaEnPdf() throws Exception {
        abrirAtencion(pacienteId);
        long o = borrador();
        registrar(o, "CARIES", 36, null, Set.of(Superficie.O, Superficie.D), null, "CDP");
        odontogramaService.cerrar(o);

        BufferedImage img = new BufferedImage(160, 90, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);
        byte[] pdf = odontogramaService.pdfFicha(pacienteId, null, png.toByteArray());

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(2000);
        assertThatThrownBy(() -> odontogramaService.pdfFicha(paciente(30), null, null)).hasMessageContaining("aún no tiene odontograma");
    }
}
