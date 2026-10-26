package com.odontogest.ui.soporte;

import atlantafx.base.theme.Styles;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.PagoServiceASEJ;
import com.odontogest.servicio.dto.PagoDtoASEJ;
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

import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_ALERTAASEJ;
import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_OKASEJ;
import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_PELIGROASEJ;

@Component
@RequiredArgsConstructor
public class AccionesPagoASEJ {

    private final PagoServiceASEJ pagoServiceASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ExportadorASEJ exportadorASEJ;

    public boolean puedeAnularASEJ(PagoDtoASEJ pASEJ) {
        return pASEJ != null && !pASEJ.anuladoASEJ() && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PAGO_ANULAR);
    }

    public void imprimirReciboASEJ(Node origenASEJ, PagoDtoASEJ pASEJ) {
        if (pASEJ != null) {
            exportadorASEJ.guardarPdfASEJ(origenASEJ, "recibo-" + pASEJ.numeroReciboASEJ() + ".pdf", () -> pagoServiceASEJ.pdfReciboASEJ(pASEJ.idASEJ()));
        }
    }

    public void anularASEJ(PagoDtoASEJ pASEJ, Consumer<PagoDtoASEJ> alAnularASEJ) {
        if (!puedeAnularASEJ(pASEJ)) {
            return;
        }
        TextArea motivoASEJ = new TextArea();
        motivoASEJ.setPrefRowCount(3);
        motivoASEJ.setWrapText(true);
        motivoASEJ.setPromptText("Por ejemplo: monto digitado por error, pago registrado a otro paciente…");
        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Recibo", new Label(pASEJ.numeroReciboASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Paciente", new Label(pASEJ.pacienteNombreASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Monto", new Label(FormulariosASEJ.solesASEJ(pASEJ.montoASEJ()) + " · " + pASEJ.medioASEJ().etiquetaASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Motivo *", motivoASEJ);
        Label avisoASEJ = new Label("Lo pagado se descuenta de las cuotas y vuelven a quedar pendientes. El recibo se "
                + "conserva con la marca ANULADO; el número no se reutiliza.");
        avisoASEJ.setWrapText(true);
        avisoASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button confirmarASEJ = FormulariosASEJ.botonASEJ("Anular pago", Feather.X_CIRCLE, Styles.DANGER);
        confirmarASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(confirmarASEJ, () -> pagoServiceASEJ.anularASEJ(pASEJ.idASEJ(), motivoASEJ.getText()), dASEJ -> {
            notificadorASEJ.cerrarPanelASEJ();
            notificadorASEJ.exitoASEJ("Pago " + dASEJ.numeroReciboASEJ() + " anulado.");
            alAnularASEJ.accept(dASEJ);
        }));
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Anular pago", new VBox(12, gASEJ, avisoASEJ), cancelarASEJ, confirmarASEJ));
        Platform.runLater(motivoASEJ::requestFocus);
    }

    public static TableColumn<PagoDtoASEJ, String> columnaEstadoASEJ() {
        TableColumn<PagoDtoASEJ, String> cASEJ = FormulariosASEJ.columnaASEJ("Estado",
                pASEJ -> pASEJ.anuladoASEJ() ? "Anulado: " + pASEJ.motivoAnulacionASEJ() : "Válido", 120);
        cASEJ.setCellFactory(colASEJ -> new TableCell<>() {
            @Override
            protected void updateItem(String textoASEJ, boolean vacioASEJ) {
                super.updateItem(textoASEJ, vacioASEJ);
                getStyleClass().removeAll(ESTADO_OKASEJ, ESTADO_PELIGROASEJ);
                setText(vacioASEJ ? null : textoASEJ);
                if (!vacioASEJ && textoASEJ != null) {
                    getStyleClass().add(textoASEJ.startsWith("Anulado") ? ESTADO_PELIGROASEJ : ESTADO_OKASEJ);
                }
            }
        });
        return cASEJ;
    }

    public static String aplicadoAASEJ(PagoDtoASEJ pASEJ) {
        List<String> partesASEJ = pASEJ.aplicacionesASEJ().stream().map(aASEJ -> aASEJ.etiquetaASEJ()).toList();
        return String.join(", ", partesASEJ);
    }
}
