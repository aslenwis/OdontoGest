package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;
import com.odontogest.servicio.CatalogoHallazgoServiceASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoFormASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.componente.PaletaOdontogramaASEJ;
import com.odontogest.ui.componente.RenderizadorOdontogramaASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import com.odontogest.ui.soporte.TemaASEJ;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class HallazgosTabControllerASEJ extends ListadoEditableASEJ<HallazgoTipoDtoASEJ> {

    private static final RenderizadorOdontogramaASEJ RENDERIZADORASEJ = new RenderizadorOdontogramaASEJ();

    private final CatalogoHallazgoServiceASEJ catalogoServiceASEJ;

    public HallazgosTabControllerASEJ(TareaUiASEJ tareaUiASEJ, NotificadorASEJ notificadorASEJ, CatalogoHallazgoServiceASEJ catalogoServiceASEJ) {
        super(tareaUiASEJ, notificadorASEJ);
        this.catalogoServiceASEJ = catalogoServiceASEJ;
    }

    static String representacionASEJ(RepresentacionASEJ rASEJ) {
        return switch (rASEJ) {
            case SUPERFICIE -> "Por superficie";
            case PIEZA -> "Por pieza";
            case ENTRE_PIEZAS -> "Entre dos piezas";
            case RANGO_PIEZAS -> "Rango de piezas";
        };
    }

    static String reglaColorASEJ(ReglaColorASEJ rASEJ) {
        return switch (rASEJ) {
            case POR_ESTADO -> "Según estado (azul bueno / rojo malo)";
            case SIEMPRE_ROJO -> "Siempre rojo";
            case SIEMPRE_AZUL -> "Siempre azul";
        };
    }

    static String siglaASEJ(HallazgoTipoDtoASEJ tASEJ) {
        return tASEJ.siglasOpcionesASEJ().isEmpty() ? (tASEJ.siglaASEJ() == null ? "" : tASEJ.siglaASEJ()) : String.join(" / ", tASEJ.siglasOpcionesASEJ());
    }

    @Override
    protected void configurarColumnasASEJ() {
        TableColumn<HallazgoTipoDtoASEJ, HallazgoTipoDtoASEJ> muestraASEJ = new TableColumn<>("Dibujo");
        muestraASEJ.setCellValueFactory(fASEJ -> new SimpleObjectProperty<>(fASEJ.getValue()));
        muestraASEJ.setCellFactory(cASEJ -> new TableCell<>() {
            private final Canvas lienzoASEJ = new Canvas(30, 30);

            @Override
            protected void updateItem(HallazgoTipoDtoASEJ tASEJ, boolean vacioASEJ) {
                super.updateItem(tASEJ, vacioASEJ);
                if (vacioASEJ || tASEJ == null) {
                    setGraphic(null);
                    return;
                }
                RENDERIZADORASEJ.muestraASEJ(lienzoASEJ.getGraphicsContext2D(), OdontogramaControllerASEJ.definicionASEJ(tASEJ), 30,
                        TemaASEJ.actualASEJ() == TemaASEJ.OSCURO ? PaletaOdontogramaASEJ.oscuraASEJ() : PaletaOdontogramaASEJ.claraASEJ());
                setGraphic(lienzoASEJ);
            }
        });
        muestraASEJ.setPrefWidth(60);
        muestraASEJ.setSortable(false);
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                muestraASEJ,
                FormulariosASEJ.columnaASEJ("Orden", HallazgoTipoDtoASEJ::ordenASEJ, 60),
                FormulariosASEJ.columnaASEJ("Nombre", HallazgoTipoDtoASEJ::nombreASEJ, 210),
                FormulariosASEJ.columnaASEJ("Sigla", HallazgosTabControllerASEJ::siglaASEJ, 110),
                FormulariosASEJ.columnaASEJ("Representación", tASEJ -> representacionASEJ(tASEJ.representacionASEJ()), 120),
                FormulariosASEJ.columnaASEJ("Símbolo", HallazgoTipoDtoASEJ::simboloASEJ, 140),
                FormulariosASEJ.columnaASEJ("Color", tASEJ -> switch (tASEJ.reglaColorASEJ()) {
                    case POR_ESTADO -> "Según estado";
                    case SIEMPRE_ROJO -> "Rojo";
                    case SIEMPRE_AZUL -> "Azul";
                }, 100),
                FormulariosASEJ.columnaASEJ("Capa", HallazgoTipoDtoASEJ::grupoExclusionASEJ, 150),
                FormulariosASEJ.columnaASEJ("CPO-D", HallazgoTipoDtoASEJ::conteoCpodASEJ, 65),
                FormulariosASEJ.columnaASEJ("ceo-d", HallazgoTipoDtoASEJ::conteoCeodASEJ, 65),
                FormulariosASEJ.columnaASEJ("Activo", tASEJ -> FormulariosASEJ.siNoASEJ(tASEJ.activoASEJ()), 60),
                FormulariosASEJ.columnaASEJ("En uso", tASEJ -> FormulariosASEJ.siNoASEJ(tASEJ.enUsoASEJ()), 60));
    }

    @Override
    protected List<HallazgoTipoDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        return catalogoServiceASEJ.listarTiposASEJ(incluirInactivosASEJ);
    }

    @Override
    protected void abrirFormularioASEJ(HallazgoTipoDtoASEJ tASEJ) {
        TextField codigoASEJ = new TextField(tASEJ == null ? "" : tASEJ.codigoASEJ());
        codigoASEJ.setPromptText("PIEZA_AUSENTE_DEX");
        TextField nombreASEJ = new TextField(tASEJ == null ? "" : tASEJ.nombreASEJ());
        TextField siglaASEJ = new TextField(tASEJ == null || tASEJ.siglaASEJ() == null ? "" : tASEJ.siglaASEJ());
        TextField opcionesASEJ = new TextField(tASEJ == null ? "" : String.join(", ", tASEJ.siglasOpcionesASEJ()));
        opcionesASEJ.setPromptText("AM, R, IV (el usuario elige al registrar)");
        ComboBox<RepresentacionASEJ> representacionASEJ = comboASEJ(RepresentacionASEJ.values(), tASEJ == null ? RepresentacionASEJ.PIEZA : tASEJ.representacionASEJ());
        representacionASEJ.setConverter(FormulariosASEJ.conversorASEJ(HallazgosTabControllerASEJ::representacionASEJ));
        ComboBox<SimboloHallazgoASEJ> simboloASEJ = comboASEJ(SimboloHallazgoASEJ.values(), tASEJ == null ? SimboloHallazgoASEJ.SOLO_SIGLA : tASEJ.simboloASEJ());
        ComboBox<ReglaColorASEJ> colorASEJ = comboASEJ(ReglaColorASEJ.values(), tASEJ == null ? ReglaColorASEJ.SIEMPRE_ROJO : tASEJ.reglaColorASEJ());
        colorASEJ.setConverter(FormulariosASEJ.conversorASEJ(HallazgosTabControllerASEJ::reglaColorASEJ));
        ComboBox<String> capaASEJ = new ComboBox<>();
        capaASEJ.setEditable(true);
        capaASEJ.getItems().setAll(tablaASEJ.getItems().stream().map(HallazgoTipoDtoASEJ::grupoExclusionASEJ).distinct().sorted().toList());
        capaASEJ.setValue(tASEJ == null ? null : tASEJ.grupoExclusionASEJ());
        CheckBox ocultaASEJ = new CheckBox("Oculta las superficies (pieza ausente)");
        ocultaASEJ.setSelected(tASEJ != null && tASEJ.ocultaSuperficiesASEJ());
        ComboBox<ConteoIndiceASEJ> cpodASEJ = comboASEJ(new ConteoIndiceASEJ[]{ConteoIndiceASEJ.NINGUNO, ConteoIndiceASEJ.C, ConteoIndiceASEJ.P, ConteoIndiceASEJ.O},
                tASEJ == null ? ConteoIndiceASEJ.NINGUNO : tASEJ.conteoCpodASEJ());
        ComboBox<ConteoIndiceASEJ> ceodASEJ = comboASEJ(new ConteoIndiceASEJ[]{ConteoIndiceASEJ.NINGUNO, ConteoIndiceASEJ.C, ConteoIndiceASEJ.E, ConteoIndiceASEJ.O},
                tASEJ == null ? ConteoIndiceASEJ.NINGUNO : tASEJ.conteoCeodASEJ());
        Spinner<Integer> ordenASEJ = new Spinner<>(0, 9999, tASEJ == null ? siguienteOrdenASEJ() : tASEJ.ordenASEJ(), 10);
        ordenASEJ.setEditable(true);
        CheckBox activoASEJ = new CheckBox("Activo");
        activoASEJ.setSelected(tASEJ == null || tASEJ.activoASEJ());

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Código *", codigoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Nombre *", nombreASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Sigla fija", siglaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Opciones de sigla", opcionesASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Representación *", representacionASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Símbolo *", simboloASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Regla de color *", colorASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Capa de exclusión *", capaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", ocultaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Cuenta en CPO-D", cpodASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Cuenta en ceo-d", ceodASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Orden en la paleta", ordenASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", activoASEJ);

        Node contenidoASEJ = gASEJ;
        if (tASEJ != null && tASEJ.enUsoASEJ()) {
            for (Node nASEJ : List.of(codigoASEJ, representacionASEJ, colorASEJ, capaASEJ, ocultaASEJ, cpodASEJ, ceodASEJ)) {
                nASEJ.setDisable(true);
            }
            Label avisoASEJ = new Label("Este tipo ya tiene hallazgos registrados: su estructura no puede cambiar porque "
                    + "alteraría odontogramas cerrados. Puede editar nombre, sigla, símbolo, orden y estado.");
            avisoASEJ.setWrapText(true);
            avisoASEJ.getStyleClass().addAll(Styles.WARNING, Styles.TEXT_SMALL);
            contenidoASEJ = new VBox(12, avisoASEJ, gASEJ);
        }

        Long idASEJ = tASEJ == null ? null : tASEJ.idASEJ();
        abrirPanelASEJ(tASEJ == null ? "Nuevo tipo de hallazgo" : "Editar tipo de hallazgo", contenidoASEJ, () -> {
            List<String> listaOpcionesASEJ = Arrays.stream(opcionesASEJ.getText().split("[,;/\\s]+"))
                    .filter(sASEJ -> !sASEJ.isBlank()).toList();
            HallazgoTipoFormASEJ formASEJ = new HallazgoTipoFormASEJ(idASEJ, codigoASEJ.getText(), nombreASEJ.getText(), siglaASEJ.getText(),
                    listaOpcionesASEJ, representacionASEJ.getValue(), simboloASEJ.getValue(), colorASEJ.getValue(),
                    capaASEJ.getEditor().getText(), ocultaASEJ.isSelected(), cpodASEJ.getValue(), ceodASEJ.getValue(),
                    activoASEJ.isSelected(), ordenASEJ.getValue());
            return () -> catalogoServiceASEJ.guardarTipoASEJ(formASEJ);
        }, "Tipo de hallazgo guardado.");
    }

    private int siguienteOrdenASEJ() {
        return tablaASEJ.getItems().stream().mapToInt(HallazgoTipoDtoASEJ::ordenASEJ).max().orElse(0) + 10;
    }

    private static <E> ComboBox<E> comboASEJ(E[] valoresASEJ, E seleccionASEJ) {
        ComboBox<E> cASEJ = new ComboBox<>();
        cASEJ.getItems().setAll(valoresASEJ);
        cASEJ.setValue(seleccionASEJ);
        return cASEJ;
    }
}
