/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.evento.NavegarA;
import com.odontogest.evento.SolicitudNuevaAtencion;
import com.odontogest.modelo.EstadoAtencion;
import com.odontogest.modelo.Rol;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AtencionService;
import com.odontogest.servicio.OdontologoService;
import com.odontogest.servicio.dto.AtencionDetalle;
import com.odontogest.servicio.dto.AtencionForm;
import com.odontogest.servicio.dto.AtencionResumen;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.UsuarioSesion;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.TareaUi;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Pestaña "Atenciones" de la ficha (RF-ATE-01, RF-ATE-03, RF-ATE-04): línea de tiempo de atenciones, datos
 * clínicos editables mientras la atención está abierta y cierre. Abrir una atención abre su odontograma en borrador.
 */
@Component
@RequiredArgsConstructor
public class AtencionesController {

    private final AtencionService atencionService;
    private final OdontologoService odontologoService;
    private final OdontogramaController odontogramaController;
    private final PacienteActivo pacienteActivo;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    @FXML private Button btnNueva;
    @FXML private ListView<AtencionResumen> lstAtenciones;
    @FXML private VBox panelDetalle;
    @FXML private Label lblTitulo;
    @FXML private Label lblDetalle;
    @FXML private TextArea txtMotivo;
    @FXML private TextArea txtExamen;
    @FXML private TextArea txtDiagnostico;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnOdontograma;
    @FXML private Button btnCerrar;
    @FXML private Button btnGuardar;

    private ChangeListener<PacienteDetalle> oyente;
    private AtencionDetalle actual;

    @FXML
    public void initialize() {
        btnNueva.setDisable(!sesionUsuario.tiene(Permiso.ATENCION_GESTIONAR));
        lstAtenciones.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(AtencionResumen a, boolean vacio) {
                super.updateItem(a, vacio);
                if (vacio || a == null) {
                    setText(null);
                    return;
                }
                setText(Formularios.fechaHora(a.fecha()) + (a.estado() == EstadoAtencion.ABIERTA ? "  · ABIERTA" : "")
                        + "\n" + a.motivoConsulta().replaceAll("\\s+", " ") + "\n" + a.odontologoNombre());
            }
        });
        lstAtenciones.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> {
            if (b != null) {
                tareaUi.ejecutar(() -> atencionService.obtener(b.id()), this::mostrar);
            }
        });
        mostrar(null);

        if (oyente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyente);
        }
        oyente = (obs, anterior, nuevo) -> recargar(null);
        pacienteActivo.pacienteProperty().addListener(oyente);
        recargar(null);
    }

    /** Recarga la línea de tiempo y selecciona la atención indicada (o la primera). */
    private void recargar(Long seleccionar) {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || lstAtenciones == null) {
            return;
        }
        tareaUi.ejecutar(() -> atencionService.listarPorPaciente(p.id()), lista -> {
            lstAtenciones.getItems().setAll(lista);
            AtencionResumen elegido = lista.stream().filter(a -> seleccionar != null && a.id() == seleccionar).findFirst()
                    .orElse(lista.isEmpty() ? null : lista.get(0));
            if (elegido == null) {
                mostrar(null);
            } else {
                lstAtenciones.getSelectionModel().select(elegido);
            }
        });
    }

    private void mostrar(AtencionDetalle a) {
        actual = a;
        boolean hay = a != null;
        panelDetalle.setDisable(!hay);
        lblTitulo.setText(hay ? "Atención del " + Formularios.fechaHora(a.fecha()) : "Sin atenciones registradas");
        lblDetalle.setText(hay ? a.odontologoNombre() + " · " + (a.abierta() ? "Abierta"
                : "Cerrada el " + Formularios.fechaHora(a.cerradaEn()) + " (solo lectura)") : "");
        txtMotivo.setText(hay ? texto(a.motivoConsulta()) : "");
        txtExamen.setText(hay ? texto(a.examenClinico()) : "");
        txtDiagnostico.setText(hay ? texto(a.diagnostico()) : "");
        txtObservaciones.setText(hay ? texto(a.observaciones()) : "");
        boolean editable = hay && a.abierta() && sesionUsuario.tiene(Permiso.ATENCION_GESTIONAR);
        for (TextArea t : List.of(txtMotivo, txtExamen, txtDiagnostico, txtObservaciones)) {
            t.setEditable(editable);
        }
        btnGuardar.setDisable(!editable);
        btnCerrar.setDisable(!editable);
        btnOdontograma.setDisable(!hay);
    }

    /** Desde la cabecera de la ficha o la pestaña Odontograma. */
    @EventListener
    public void alSolicitarNuevaAtencion(SolicitudNuevaAtencion solicitud) {
        if (Platform.isFxApplicationThread()) {
            onNueva();
        } else {
            Platform.runLater(this::onNueva);
        }
    }

    @FXML
    private void onNueva() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || !sesionUsuario.tiene(Permiso.ATENCION_GESTIONAR)) {
            return;
        }
        tareaUi.ejecutar(() -> odontologoService.listar(false), odontologos -> formularioNueva(p, odontologos));
    }

    private void formularioNueva(PacienteDetalle p, List<OdontologoDto> odontologos) {
        UsuarioSesion usuario = sesionUsuario.exigirUsuario();
        boolean esOdontologo = Rol.valueOf(usuario.rol()) == Rol.ODONTOLOGO;
        ComboBox<OdontologoDto> odontologo = new ComboBox<>();
        odontologo.getItems().setAll(odontologos);
        odontologo.setValue(odontologos.stream()
                .filter(o -> esOdontologo && usuario.odontologoId() != null && o.id() == usuario.odontologoId())
                .findFirst().orElse(odontologos.size() == 1 ? odontologos.get(0) : null));
        odontologo.setDisable(esOdontologo);
        TextArea motivo = new TextArea();
        motivo.setPrefRowCount(3);
        motivo.setWrapText(true);
        motivo.setPromptText("Dolor, control, limpieza…");
        ComboBox<Denticion> denticion = new ComboBox<>();
        denticion.getItems().setAll(Denticion.values());
        denticion.setConverter(Formularios.conversor(d -> switch (d) {
            case PERMANENTE -> "Permanente";
            case DECIDUA -> "Decidua";
            case MIXTA -> "Mixta";
        }));
        denticion.setValue(Denticion.sugerida(p.edad()));

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Paciente", new Label(p.nombreCompleto() + " · " + p.numeroHistoria()));
        Formularios.fila(g, "Odontólogo *", odontologo);
        Formularios.fila(g, "Motivo *", motivo);
        Formularios.fila(g, "Dentición", denticion);
        Label ayuda = new Label("Se abrirá el odontograma en borrador (inicial si es el primero del paciente, "
                + "si no de evolución). La dentición sugerida corresponde a la edad (" + p.edad() + " años).");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button abrir = Formularios.boton("Abrir atención", Feather.CHECK, Styles.ACCENT);
        abrir.setOnAction(e -> {
            AtencionForm form = new AtencionForm(null, p.id(), odontologo.getValue() == null ? null : odontologo.getValue().id(),
                    motivo.getText(), null, null, null, denticion.getValue());
            tareaUi.ejecutar(abrir, () -> atencionService.abrir(form), id -> {
                notificador.cerrarPanel();
                notificador.exito("Atención abierta: registre el odontograma.");
                recargar(id);
                odontogramaController.recargar();
                eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", p.id(), "pestana", "odontograma")));
            });
        });
        notificador.abrirPanel(Formularios.panelLateral("Nueva atención", new VBox(12, g, ayuda), cancelar, abrir));
        Platform.runLater(motivo::requestFocus);
    }

    @FXML
    private void onGuardar() {
        AtencionDetalle a = actual;
        if (a == null) {
            return;
        }
        AtencionForm form = new AtencionForm(a.id(), a.pacienteId(), a.odontologoId(), txtMotivo.getText(), txtExamen.getText(),
                txtDiagnostico.getText(), txtObservaciones.getText(), null);
        tareaUi.ejecutar(btnGuardar, () -> atencionService.actualizar(form), d -> {
            notificador.exito("Atención guardada.");
            recargar(d.id());
        });
    }

    @FXML
    private void onCerrar() {
        AtencionDetalle a = actual;
        if (a == null || !notificador.confirmar("Cerrar atención",
                "La atención quedará de solo lectura. El odontograma de la atención debe estar firmado. ¿Continuar?")) {
            return;
        }
        tareaUi.ejecutar(btnCerrar, () -> atencionService.cerrar(a.id()), d -> {
            notificador.exito("Atención cerrada.");
            recargar(d.id());
            odontogramaController.recargar();
        });
    }

    @FXML
    private void onIrOdontograma() {
        PacienteDetalle p = pacienteActivo.get();
        if (p != null) {
            eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", p.id(), "pestana", "odontograma")));
        }
    }

    private static String texto(String v) {
        return v == null ? "" : v;
    }
}
