package com.odontogest.ui.controlador;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;
import com.odontogest.servicio.CatalogoHallazgoServiceASEJ;
import com.odontogest.servicio.ProcedimientoServiceASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ReglaSugerenciaDtoASEJ;
import com.odontogest.servicio.dto.ReglaSugerenciaFormASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ.OpcionASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ReglasTabControllerASEJ extends ListadoEditableASEJ<ReglaSugerenciaDtoASEJ> {

    private record ListasASEJ(List<HallazgoTipoDtoASEJ> tiposASEJ, List<ProcedimientoDtoASEJ> procedimientosASEJ) {
    }

    private final CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    private final ProcedimientoServiceASEJ procedimientoServiceASEJ;

    @FXML private Button btnEliminarASEJ;

    public ReglasTabControllerASEJ(TareaUiASEJ tareaUiASEJ, NotificadorASEJ notificadorASEJ, CatalogoHallazgoServiceASEJ catalogoServiceASEJ,
                               ProcedimientoServiceASEJ procedimientoServiceASEJ) {
        super(tareaUiASEJ, notificadorASEJ);
        this.catalogoServiceASEJ = catalogoServiceASEJ;
        this.procedimientoServiceASEJ = procedimientoServiceASEJ;
    }

    static String estadoASEJ(EstadoHallazgoASEJ eASEJ) {
        return eASEJ == null ? "Cualquiera" : eASEJ == EstadoHallazgoASEJ.BUENO ? "Bueno" : "Malo";
    }

    static String radicularASEJ(TipoRadicularASEJ tASEJ) {
        return tASEJ == null ? "Cualquiera" : switch (tASEJ) {
            case UNIRRADICULAR -> "Unirradicular";
            case BIRRADICULAR -> "Birradicular";
            case MULTIRRADICULAR -> "Multirradicular";
        };
    }

    @Override
    protected void alInicializarASEJ() {
        btnEliminarASEJ.disableProperty().bind(tablaASEJ.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    protected void configurarColumnasASEJ() {
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Prioridad", ReglaSugerenciaDtoASEJ::prioridadASEJ, 80),
                FormulariosASEJ.columnaASEJ("Hallazgo", ReglaSugerenciaDtoASEJ::hallazgoTipoNombreASEJ, 200),
                FormulariosASEJ.columnaASEJ("Estado", rASEJ -> estadoASEJ(rASEJ.estadoASEJ()), 90),
                FormulariosASEJ.columnaASEJ("Tipo radicular", rASEJ -> radicularASEJ(rASEJ.tipoRadicularASEJ()), 120),
                FormulariosASEJ.columnaASEJ("Superficies", rASEJ -> rASEJ.minSuperficiesASEJ() + "–" + rASEJ.maxSuperficiesASEJ(), 90),
                FormulariosASEJ.columnaASEJ("Procedimiento", ReglaSugerenciaDtoASEJ::procedimientoNombreASEJ, 260),
                FormulariosASEJ.columnaASEJ("Fase", ReglaSugerenciaDtoASEJ::faseASEJ, 60),
                FormulariosASEJ.columnaASEJ("Activa", rASEJ -> FormulariosASEJ.siNoASEJ(rASEJ.activoASEJ()), 70));
    }

    @Override
    protected List<ReglaSugerenciaDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        return catalogoServiceASEJ.listarReglasASEJ();
    }

    @Override
    protected void abrirFormularioASEJ(ReglaSugerenciaDtoASEJ reglaASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> new ListasASEJ(catalogoServiceASEJ.listarTiposASEJ(true), procedimientoServiceASEJ.listarASEJ(true)),
                listasASEJ -> mostrarFormularioASEJ(reglaASEJ, listasASEJ));
    }

    private void mostrarFormularioASEJ(ReglaSugerenciaDtoASEJ rASEJ, ListasASEJ listasASEJ) {
        List<OpcionASEJ<Long>> opcionesTipoASEJ = new ArrayList<>();
        opcionesTipoASEJ.add(new OpcionASEJ<>("(general · toda la boca)", null));
        listasASEJ.tiposASEJ().forEach(tASEJ -> opcionesTipoASEJ.add(new OpcionASEJ<>(tASEJ.nombreASEJ(), tASEJ.idASEJ())));
        ComboBox<OpcionASEJ<Long>> hallazgoASEJ = new ComboBox<>();
        hallazgoASEJ.getItems().setAll(opcionesTipoASEJ);
        hallazgoASEJ.setValue(opcionesTipoASEJ.stream()
                .filter(oASEJ -> rASEJ != null && Objects.equals(oASEJ.valorASEJ(), rASEJ.hallazgoTipoIdASEJ())).findFirst()
                .orElse(opcionesTipoASEJ.get(rASEJ == null && opcionesTipoASEJ.size() > 1 ? 1 : 0)));

        ComboBox<OpcionASEJ<EstadoHallazgoASEJ>> estadoASEJ = new ComboBox<>();
        estadoASEJ.getItems().setAll(new OpcionASEJ<>(estadoASEJ(null), null), new OpcionASEJ<>("Bueno", EstadoHallazgoASEJ.BUENO),
                new OpcionASEJ<>("Malo", EstadoHallazgoASEJ.MALO));
        estadoASEJ.setValue(estadoASEJ.getItems().stream().filter(oASEJ -> rASEJ != null && oASEJ.valorASEJ() == rASEJ.estadoASEJ()).findFirst()
                .orElse(estadoASEJ.getItems().get(0)));

        ComboBox<OpcionASEJ<TipoRadicularASEJ>> radicularASEJ = new ComboBox<>();
        radicularASEJ.getItems().add(new OpcionASEJ<>(radicularASEJ(null), null));
        for (TipoRadicularASEJ tASEJ : TipoRadicularASEJ.values()) {
            radicularASEJ.getItems().add(new OpcionASEJ<>(radicularASEJ(tASEJ), tASEJ));
        }
        radicularASEJ.setValue(radicularASEJ.getItems().stream().filter(oASEJ -> rASEJ != null && oASEJ.valorASEJ() == rASEJ.tipoRadicularASEJ())
                .findFirst().orElse(radicularASEJ.getItems().get(0)));

        Spinner<Integer> minimoASEJ = new Spinner<>(0, 5, rASEJ == null ? 0 : rASEJ.minSuperficiesASEJ());
        Spinner<Integer> maximoASEJ = new Spinner<>(0, 5, rASEJ == null ? 5 : rASEJ.maxSuperficiesASEJ());
        ComboBox<ProcedimientoDtoASEJ> procedimientoASEJ = new ComboBox<>();
        procedimientoASEJ.getItems().setAll(listasASEJ.procedimientosASEJ());
        procedimientoASEJ.setValue(rASEJ == null ? null : listasASEJ.procedimientosASEJ().stream()
                .filter(pASEJ -> pASEJ.idASEJ() == rASEJ.procedimientoIdASEJ()).findFirst().orElse(null));
        Spinner<Integer> faseASEJ = new Spinner<>(1, 4, rASEJ == null ? 2 : rASEJ.faseASEJ());
        Spinner<Integer> prioridadASEJ = new Spinner<>(0, 9999, rASEJ == null ? 100 : rASEJ.prioridadASEJ(), 1);
        prioridadASEJ.setEditable(true);
        CheckBox activaASEJ = new CheckBox("Activa");
        activaASEJ.setSelected(rASEJ == null || rASEJ.activoASEJ());

        hallazgoASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            boolean generalASEJ = bASEJ == null || bASEJ.valorASEJ() == null;
            estadoASEJ.setDisable(generalASEJ);
            radicularASEJ.setDisable(generalASEJ);
        });
        boolean generalASEJ = hallazgoASEJ.getValue().valorASEJ() == null;
        estadoASEJ.setDisable(generalASEJ);
        radicularASEJ.setDisable(generalASEJ);

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Hallazgo", hallazgoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Estado", estadoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Tipo radicular", radicularASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Superficies desde", minimoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Superficies hasta", maximoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Procedimiento *", procedimientoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Fase (1–4)", faseASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Prioridad", prioridadASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", activaASEJ);

        Long idASEJ = rASEJ == null ? null : rASEJ.idASEJ();
        abrirPanelASEJ(rASEJ == null ? "Nueva regla" : "Editar regla", gASEJ, () -> {
            Long tipoIdASEJ = FormulariosASEJ.seleccionASEJ(hallazgoASEJ);
            ReglaSugerenciaFormASEJ formASEJ = new ReglaSugerenciaFormASEJ(idASEJ, tipoIdASEJ,
                    tipoIdASEJ == null ? null : FormulariosASEJ.seleccionASEJ(estadoASEJ),
                    tipoIdASEJ == null ? null : FormulariosASEJ.seleccionASEJ(radicularASEJ),
                    minimoASEJ.getValue(), maximoASEJ.getValue(),
                    procedimientoASEJ.getValue() == null ? null : procedimientoASEJ.getValue().idASEJ(),
                    faseASEJ.getValue(), prioridadASEJ.getValue(), activaASEJ.isSelected());
            return () -> catalogoServiceASEJ.guardarReglaASEJ(formASEJ);
        }, "Regla guardada.");
    }

    @FXML
    private void onEliminarASEJ() {
        ReglaSugerenciaDtoASEJ rASEJ = tablaASEJ.getSelectionModel().getSelectedItem();
        if (rASEJ == null || !notificadorASEJ.confirmarASEJ("Eliminar regla",
                "¿Eliminar la regla \"" + rASEJ.hallazgoTipoNombreASEJ() + " → " + rASEJ.procedimientoNombreASEJ() + "\"?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnEliminarASEJ, () -> {
            catalogoServiceASEJ.eliminarReglaASEJ(rASEJ.idASEJ());
            return null;
        }, xASEJ -> {
            notificadorASEJ.exitoASEJ("Regla eliminada.");
            refrescarASEJ();
        });
    }
}
