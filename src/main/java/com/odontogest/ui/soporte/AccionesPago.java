/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import atlantafx.base.theme.Styles;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.PagoService;
import com.odontogest.servicio.dto.PagoDto;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

import static com.odontogest.ui.soporte.Formularios.ESTADO_ALERTA;
import static com.odontogest.ui.soporte.Formularios.ESTADO_OK;
import static com.odontogest.ui.soporte.Formularios.ESTADO_PELIGRO;

/**
 * Acciones sobre un pago compartidas por la caja y la pestaña Pagos: imprimir el recibo, anular con motivo
 * (panel lateral) y la columna "Estado" coloreada.
 */
@Component
@RequiredArgsConstructor
public class AccionesPago {

    private final PagoService pagoService;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final SesionUsuario sesionUsuario;
    private final Exportador exportador;

    public boolean puedeAnular(PagoDto p) {
        return p != null && !p.anulado() && sesionUsuario.tiene(Permiso.PAGO_ANULAR);
    }

    public void imprimirRecibo(Node origen, PagoDto p) {
        if (p != null) {
            exportador.guardarPdf(origen, "recibo-" + p.numeroRecibo() + ".pdf", () -> pagoService.pdfRecibo(p.id()));
        }
    }

    /** Panel con el motivo; al anular llama a {@code alAnular} con el pago actualizado. */
    public void anular(PagoDto p, Consumer<PagoDto> alAnular) {
        if (!puedeAnular(p)) {
            return;
        }
        TextArea motivo = new TextArea();
        motivo.setPrefRowCount(3);
        motivo.setWrapText(true);
        motivo.setPromptText("Por ejemplo: monto digitado por error, pago registrado a otro paciente…");
        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Recibo", new Label(p.numeroRecibo()));
        Formularios.fila(g, "Paciente", new Label(p.pacienteNombre()));
        Formularios.fila(g, "Monto", new Label(Formularios.soles(p.monto()) + " · " + p.medio().etiqueta()));
        Formularios.fila(g, "Motivo *", motivo);
        Label aviso = new Label("Lo pagado se descuenta de las cuotas y vuelven a quedar pendientes. El recibo se "
                + "conserva con la marca ANULADO; el número no se reutiliza.");
        aviso.setWrapText(true);
        aviso.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button confirmar = Formularios.boton("Anular pago", Feather.X_CIRCLE, Styles.DANGER);
        confirmar.setOnAction(e -> tareaUi.ejecutar(confirmar, () -> pagoService.anular(p.id(), motivo.getText()), d -> {
            notificador.cerrarPanel();
            notificador.exito("Pago " + d.numeroRecibo() + " anulado.");
            alAnular.accept(d);
        }));
        notificador.abrirPanel(Formularios.panelLateral("Anular pago", new VBox(12, g, aviso), cancelar, confirmar));
        Platform.runLater(motivo::requestFocus);
    }

    /** Columna "Estado": Válido o Anulado (en rojo, con el motivo en la celda). */
    public static TableColumn<PagoDto, String> columnaEstado() {
        TableColumn<PagoDto, String> c = Formularios.columna("Estado",
                p -> p.anulado() ? "Anulado: " + p.motivoAnulacion() : "Válido", 120);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                getStyleClass().removeAll(ESTADO_OK, ESTADO_PELIGRO);
                setText(vacio ? null : texto);
                if (!vacio && texto != null) {
                    getStyleClass().add(texto.startsWith("Anulado") ? ESTADO_PELIGRO : ESTADO_OK);
                }
            }
        });
        return c;
    }

    /** Aplicaciones de un pago en una línea: "Plan N° 3 · Cuota 1, Plan N° 3 · Cuota 2". */
    public static String aplicadoA(PagoDto p) {
        List<String> partes = p.aplicaciones().stream().map(a -> a.etiqueta()).toList();
        return String.join(", ", partes);
    }
}
