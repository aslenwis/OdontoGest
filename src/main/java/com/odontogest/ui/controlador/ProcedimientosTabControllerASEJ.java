package com.odontogest.ui.controlador;

import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.modelo.CategoriaProcedimientoASEJ;
import com.odontogest.servicio.ProcedimientoServiceASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ProcedimientoFormASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProcedimientosTabControllerASEJ extends ListadoEditableASEJ<ProcedimientoDtoASEJ> {

    private final ProcedimientoServiceASEJ procedimientoServiceASEJ;

    public ProcedimientosTabControllerASEJ(TareaUiASEJ tareaUiASEJ, NotificadorASEJ notificadorASEJ, ProcedimientoServiceASEJ procedimientoServiceASEJ) {
        super(tareaUiASEJ, notificadorASEJ);
        this.procedimientoServiceASEJ = procedimientoServiceASEJ;
    }

    static String formaCobroASEJ(FormaCobroASEJ fASEJ) {
        return switch (fASEJ) {
            case PIEZA -> "Por pieza";
            case SUPERFICIE -> "Por superficie";
            case ARCADA -> "Por arcada";
            case BOCA -> "Toda la boca";
            case SESION -> "Por sesión";
        };
    }

    @Override
    protected void configurarColumnasASEJ() {
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Código", ProcedimientoDtoASEJ::codigoASEJ, 90),
                FormulariosASEJ.columnaASEJ("Nombre", ProcedimientoDtoASEJ::nombreASEJ, 300),
                FormulariosASEJ.columnaASEJ("Categoría", pASEJ -> pASEJ.categoriaASEJ().etiquetaASEJ, 130),
                FormulariosASEJ.columnaASEJ("Cobro", pASEJ -> formaCobroASEJ(pASEJ.formaCobroASEJ()), 120),
                FormulariosASEJ.columnaASEJ("Precio", pASEJ -> FormulariosASEJ.solesASEJ(pASEJ.precioASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Duración", pASEJ -> pASEJ.duracionMinASEJ() + " min", 90),
                FormulariosASEJ.columnaASEJ("Activo", pASEJ -> FormulariosASEJ.siNoASEJ(pASEJ.activoASEJ()), 70));
    }

    @Override
    protected List<ProcedimientoDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        return procedimientoServiceASEJ.listarASEJ(incluirInactivosASEJ);
    }

    @Override
    protected void abrirFormularioASEJ(ProcedimientoDtoASEJ pASEJ) {
        TextField codigoASEJ = new TextField(pASEJ == null ? "" : pASEJ.codigoASEJ());
        codigoASEJ.setPromptText("RES-S");
        TextField nombreASEJ = new TextField(pASEJ == null ? "" : pASEJ.nombreASEJ());
        ComboBox<CategoriaProcedimientoASEJ> categoriaASEJ = new ComboBox<>();
        categoriaASEJ.getItems().setAll(CategoriaProcedimientoASEJ.values());
        categoriaASEJ.setConverter(FormulariosASEJ.conversorASEJ(cASEJ -> cASEJ.etiquetaASEJ));
        categoriaASEJ.setValue(pASEJ == null ? null : pASEJ.categoriaASEJ());
        ComboBox<FormaCobroASEJ> formaASEJ = new ComboBox<>();
        formaASEJ.getItems().setAll(FormaCobroASEJ.values());
        formaASEJ.setConverter(FormulariosASEJ.conversorASEJ(ProcedimientosTabControllerASEJ::formaCobroASEJ));
        formaASEJ.setValue(pASEJ == null ? FormaCobroASEJ.PIEZA : pASEJ.formaCobroASEJ());
        TextField precioASEJ = new TextField(pASEJ == null ? "" : pASEJ.precioASEJ().toPlainString());
        precioASEJ.setPromptText("0.00");
        Spinner<Integer> duracionASEJ = new Spinner<>(5, 480, pASEJ == null ? 30 : pASEJ.duracionMinASEJ(), 5);
        duracionASEJ.setEditable(true);
        CheckBox activoASEJ = new CheckBox("Activo");
        activoASEJ.setSelected(pASEJ == null || pASEJ.activoASEJ());

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Código *", codigoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Nombre *", nombreASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Categoría *", categoriaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Forma de cobro *", formaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Precio (S/) *", precioASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Duración (min)", duracionASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", activoASEJ);

        Long idASEJ = pASEJ == null ? null : pASEJ.idASEJ();
        abrirPanelASEJ(pASEJ == null ? "Nuevo procedimiento" : "Editar procedimiento", gASEJ, () -> {
            BigDecimal montoASEJ = FormulariosASEJ.montoASEJ(precioASEJ.getText(), "precio");
            ProcedimientoFormASEJ formASEJ = new ProcedimientoFormASEJ(idASEJ, codigoASEJ.getText(), nombreASEJ.getText(), categoriaASEJ.getValue(),
                    montoASEJ, formaASEJ.getValue(), duracionASEJ.getValue(), activoASEJ.isSelected());
            return () -> procedimientoServiceASEJ.guardarASEJ(formASEJ);
        }, "Procedimiento guardado.");
    }
}
