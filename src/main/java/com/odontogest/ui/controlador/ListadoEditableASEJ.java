package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public abstract class ListadoEditableASEJ<T> implements RefrescableASEJ {

    protected final TareaUiASEJ tareaUiASEJ;
    protected final NotificadorASEJ notificadorASEJ;

    @FXML protected TableView<T> tablaASEJ;
    @FXML protected Button btnEditarASEJ;
    @FXML protected CheckBox chkInactivosASEJ;

    protected ListadoEditableASEJ(TareaUiASEJ tareaUiASEJ, NotificadorASEJ notificadorASEJ) {
        this.tareaUiASEJ = tareaUiASEJ;
        this.notificadorASEJ = notificadorASEJ;
    }

    @FXML
    public void initialize() {
        configurarColumnasASEJ();
        tablaASEJ.setRowFactory(tASEJ -> {
            TableRow<T> filaASEJ = new TableRow<>();
            filaASEJ.setOnMouseClicked(eASEJ -> {
                if (eASEJ.getClickCount() == 2 && !filaASEJ.isEmpty()) {
                    abrirFormularioASEJ(filaASEJ.getItem());
                }
            });
            return filaASEJ;
        });
        btnEditarASEJ.disableProperty().bind(tablaASEJ.getSelectionModel().selectedItemProperty().isNull());
        if (chkInactivosASEJ != null) {
            chkInactivosASEJ.selectedProperty().addListener((obsASEJ, aASEJ, bASEJ) -> refrescarASEJ());
        }
        alInicializarASEJ();
    }

    protected void alInicializarASEJ() {
    }

    @Override
    public void refrescarASEJ() {
        boolean inactivosASEJ = chkInactivosASEJ != null && chkInactivosASEJ.isSelected();
        tareaUiASEJ.ejecutarASEJ(() -> listarASEJ(inactivosASEJ), filasASEJ -> tablaASEJ.getItems().setAll(filasASEJ));
    }

    @FXML
    protected void onNuevoASEJ() {
        abrirFormularioASEJ(null);
    }

    @FXML
    protected void onEditarASEJ() {
        T seleccionadoASEJ = tablaASEJ.getSelectionModel().getSelectedItem();
        if (seleccionadoASEJ != null) {
            abrirFormularioASEJ(seleccionadoASEJ);
        }
    }

    protected abstract void configurarColumnasASEJ();

    protected abstract List<T> listarASEJ(boolean incluirInactivosASEJ);

    protected abstract void abrirFormularioASEJ(T existenteASEJ);

    protected void abrirPanelASEJ(String tituloASEJ, Node contenidoASEJ, Supplier<Callable<?>> armarASEJ, String mensajeExitoASEJ) {
        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setCancelButton(true);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button guardarASEJ = FormulariosASEJ.botonASEJ("Guardar", Feather.SAVE, Styles.ACCENT);
        guardarASEJ.setOnAction(eASEJ -> {
            Callable<?> trabajoASEJ;
            try {
                trabajoASEJ = armarASEJ.get();
            } catch (NegocioExceptionASEJ exASEJ) {
                tareaUiASEJ.manejarErrorASEJ(exASEJ);
                return;
            }
            tareaUiASEJ.ejecutarASEJ(guardarASEJ, trabajoASEJ, rASEJ -> {
                notificadorASEJ.cerrarPanelASEJ();
                notificadorASEJ.exitoASEJ(mensajeExitoASEJ);
                refrescarASEJ();
            });
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ(tituloASEJ, contenidoASEJ, cancelarASEJ, guardarASEJ));
    }
}
