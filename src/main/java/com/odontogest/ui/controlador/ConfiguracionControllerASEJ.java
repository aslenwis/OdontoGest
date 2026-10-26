package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.ConectividadCambiadaASEJ;
import com.odontogest.modelo.TipoConfiguracionASEJ;
import com.odontogest.servicio.ConfiguracionServiceASEJ;
import com.odontogest.servicio.IntegracionServiceASEJ;
import com.odontogest.servicio.RespaldoServiceASEJ;
import com.odontogest.servicio.dto.ConfiguracionDtoASEJ;
import com.odontogest.servicio.dto.RespaldoDtoASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConfiguracionControllerASEJ implements RefrescableASEJ {

    private static final Map<String, String> GRUPOSASEJ = ordenASEJ();

    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final IntegracionServiceASEJ integracionServiceASEJ;
    private final RespaldoServiceASEJ respaldoServiceASEJ;
    private final ExportadorASEJ exportadorASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;

    @FXML private VBox gruposASEJ;
    @FXML private Label lblConexionASEJ;
    @FXML private Label lblCambiosASEJ;
    @FXML private Button btnDescartarASEJ;
    @FXML private Button btnGuardarASEJ;

    private final Map<String, String> originalesASEJ = new HashMap<>();
    private final Map<String, Supplier<String>> lectoresASEJ = new LinkedHashMap<>();
    private final Map<String, PasswordField> secretosASEJ = new HashMap<>();
    private final Map<String, Boolean> borrarSecretoASEJ = new HashMap<>();

    private static final Map<String, String[]> ETIQUETASASEJ = Map.of(
            "api.dni.url_plantilla", new String[]{"URL de la API (con {numero})", "https://<proveedor>/v1/dni/{numero}"},
            "api.dni.mapeo_json", new String[]{"Campos del JSON (mapeo)", null},
            "api.dni.prefijo_token", new String[]{"Prefijo del token", "Bearer "});

    private static Map<String, String> ordenASEJ() {
        Map<String, String> mASEJ = new LinkedHashMap<>();
        mASEJ.put("clinica", "Clínica (aparece en todos los documentos PDF)");
        mASEJ.put("agenda", "Agenda");
        mASEJ.put("recordatorio", "Recordatorios de citas por correo");
        mASEJ.put("email", "Correo (SMTP)");
        mASEJ.put("api_dni", "API de consulta de DNI");
        mASEJ.put("odontograma", "Odontograma");
        mASEJ.put("respaldo", "Respaldo");
        return mASEJ;
    }

    @FXML
    public void initialize() {
        mostrarConexionASEJ(integracionServiceASEJ.enLineaASEJ());
    }

    @Override
    public void refrescarASEJ() {
        cargarASEJ();
    }

    @EventListener
    public void alCambiarConectividadASEJ(ConectividadCambiadaASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> {
            if (lblConexionASEJ != null) {
                mostrarConexionASEJ(eASEJ.enLineaASEJ());
            }
        });
    }

    private void mostrarConexionASEJ(boolean enLineaASEJ) {
        lblConexionASEJ.setText(enLineaASEJ ? "● Conectado a internet" : "● Sin conexión a internet: DNI y correo no disponibles");
        lblConexionASEJ.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING);
        lblConexionASEJ.getStyleClass().add(enLineaASEJ ? Styles.SUCCESS : Styles.WARNING);
    }

    private void cargarASEJ() {
        tareaUiASEJ.ejecutarASEJ(configuracionServiceASEJ::listarASEJ, this::construirASEJ);
    }

    private void construirASEJ(List<ConfiguracionDtoASEJ> clavesASEJ) {
        gruposASEJ.getChildren().clear();
        originalesASEJ.clear();
        lectoresASEJ.clear();
        secretosASEJ.clear();
        borrarSecretoASEJ.clear();
        Map<String, List<ConfiguracionDtoASEJ>> porGrupoASEJ = clavesASEJ.stream()
                .collect(Collectors.groupingBy(ConfiguracionDtoASEJ::grupoASEJ, LinkedHashMap::new, Collectors.toList()));
        GRUPOSASEJ.forEach((grupoASEJ, tituloASEJ) -> {
            List<ConfiguracionDtoASEJ> listaASEJ = porGrupoASEJ.get(grupoASEJ);
            if (listaASEJ == null) {
                return;
            }
            GridPane gASEJ = FormulariosASEJ.grillaASEJ();
            gASEJ.getColumnConstraints().get(0).setMinWidth(250);
            gASEJ.getColumnConstraints().get(0).setMaxWidth(250);
            for (ConfiguracionDtoASEJ cASEJ : listaASEJ) {
                agregarCampoASEJ(gASEJ, cASEJ);
            }
            VBox contenidoASEJ = new VBox(12, gASEJ);
            Node extraASEJ = extraASEJ(grupoASEJ);
            if (extraASEJ != null) {
                contenidoASEJ.getChildren().add(extraASEJ);
            }
            TitledPane panelASEJ = new TitledPane(tituloASEJ, contenidoASEJ);
            panelASEJ.setExpanded(!grupoASEJ.equals("odontograma") && !grupoASEJ.equals("respaldo"));
            gruposASEJ.getChildren().add(panelASEJ);
        });
        actualizarCambiosASEJ();
    }

    private void agregarCampoASEJ(GridPane gASEJ, ConfiguracionDtoASEJ cASEJ) {
        String[] propiaASEJ = ETIQUETASASEJ.get(cASEJ.claveASEJ());
        String etiquetaASEJ = propiaASEJ != null ? propiaASEJ[0] : cASEJ.descripcionASEJ() == null ? cASEJ.claveASEJ() : cASEJ.descripcionASEJ();
        String ayudaASEJ = propiaASEJ == null ? null : propiaASEJ[1];
        String valorASEJ = cASEJ.valorASEJ() == null ? "" : cASEJ.valorASEJ();
        switch (cASEJ.tipoASEJ()) {
            case BOOLEANO -> {
                CheckBox chkASEJ = new CheckBox(etiquetaASEJ);
                chkASEJ.setSelected(Boolean.parseBoolean(valorASEJ));
                chkASEJ.selectedProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarCambiosASEJ());
                registrarASEJ(cASEJ.claveASEJ(), valorASEJ, () -> String.valueOf(chkASEJ.isSelected()));
                FormulariosASEJ.filaASEJ(gASEJ, "", chkASEJ);
            }
            case SECRETO -> {
                PasswordField campoASEJ = new PasswordField();
                campoASEJ.setPromptText(cASEJ.secretoConfiguradoASEJ() ? "•••••• guardado (escriba para reemplazarlo)" : "Sin configurar");
                campoASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarCambiosASEJ());
                Button borrarASEJ = FormulariosASEJ.botonASEJ("Borrar", Feather.TRASH_2, Styles.FLAT);
                borrarASEJ.setDisable(!cASEJ.secretoConfiguradoASEJ());
                borrarASEJ.setOnAction(eASEJ -> {
                    borrarSecretoASEJ.put(cASEJ.claveASEJ(), true);
                    campoASEJ.clear();
                    campoASEJ.setPromptText("Se borrará al guardar");
                    borrarASEJ.setDisable(true);
                    actualizarCambiosASEJ();
                });
                secretosASEJ.put(cASEJ.claveASEJ(), campoASEJ);
                HBox filaASEJ = new HBox(8, campoASEJ, borrarASEJ);
                HBox.setHgrow(campoASEJ, Priority.ALWAYS);
                FormulariosASEJ.filaASEJ(gASEJ, etiquetaASEJ, filaASEJ);
            }
            case JSON -> {
                TextArea areaASEJ = new TextArea(valorASEJ);
                areaASEJ.setPrefRowCount(3);
                areaASEJ.setWrapText(true);
                areaASEJ.setStyle("-fx-font-family: 'Consolas', monospace;");
                escucharASEJ(areaASEJ);
                registrarASEJ(cASEJ.claveASEJ(), valorASEJ, areaASEJ::getText);
                FormulariosASEJ.filaASEJ(gASEJ, etiquetaASEJ, areaASEJ);
            }
            default -> {
                if (cASEJ.claveASEJ().equals("clinica.logo_ruta")) {
                    FormulariosASEJ.filaASEJ(gASEJ, "Logo", campoLogoASEJ(valorASEJ));
                    return;
                }
                TextField campoASEJ = new TextField(valorASEJ);
                campoASEJ.setPromptText(ayudaASEJ);
                escucharASEJ(campoASEJ);
                registrarASEJ(cASEJ.claveASEJ(), valorASEJ, campoASEJ::getText);
                FormulariosASEJ.filaASEJ(gASEJ, etiquetaASEJ, campoASEJ);
                if (cASEJ.tipoASEJ() == TipoConfiguracionASEJ.ENTERO) {
                    campoASEJ.setMaxWidth(120);
                }
            }
        }
    }

    private void registrarASEJ(String claveASEJ, String originalASEJ, Supplier<String> lectorASEJ) {
        originalesASEJ.put(claveASEJ, originalASEJ);
        lectoresASEJ.put(claveASEJ, lectorASEJ);
    }

    private void escucharASEJ(TextInputControl campoASEJ) {
        campoASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarCambiosASEJ());
    }

    private Node campoLogoASEJ(String rutaASEJ) {
        ImageView vistaASEJ = new ImageView();
        vistaASEJ.setFitHeight(48);
        vistaASEJ.setFitWidth(160);
        vistaASEJ.setPreserveRatio(true);
        Label sinLogoASEJ = new Label("Sin logo");
        sinLogoASEJ.getStyleClass().add(Styles.TEXT_MUTED);
        if (!rutaASEJ.isBlank() && new File(rutaASEJ).isFile()) {
            vistaASEJ.setImage(new Image(new File(rutaASEJ).toURI().toString(), true));
            sinLogoASEJ.setVisible(false);
            sinLogoASEJ.setManaged(false);
        }
        Button elegirASEJ = FormulariosASEJ.botonASEJ("Elegir imagen…", Feather.IMAGE);
        elegirASEJ.setOnAction(eASEJ -> {
            FileChooser selectorASEJ = new FileChooser();
            selectorASEJ.setTitle("Logo de la clínica");
            selectorASEJ.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG o JPG", "*.png", "*.jpg", "*.jpeg"));
            File archivoASEJ = selectorASEJ.showOpenDialog(gruposASEJ.getScene().getWindow());
            if (archivoASEJ != null) {
                tareaUiASEJ.ejecutarASEJ(elegirASEJ, () -> configuracionServiceASEJ.guardarLogoASEJ(archivoASEJ.toPath()), rASEJ -> {
                    notificadorASEJ.exitoASEJ("Logo actualizado: aparecerá en los PDF.");
                    cargarASEJ();
                });
            }
        });
        Button quitarASEJ = FormulariosASEJ.botonASEJ("Quitar", Feather.X, Styles.FLAT);
        quitarASEJ.setDisable(rutaASEJ.isBlank());
        quitarASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(quitarASEJ, () -> {
            configuracionServiceASEJ.guardarASEJ(Map.of("clinica.logo_ruta", ""));
            return null;
        }, rASEJ -> {
            notificadorASEJ.exitoASEJ("Logo quitado.");
            cargarASEJ();
        }));
        HBox filaASEJ = new HBox(10, vistaASEJ, sinLogoASEJ, elegirASEJ, quitarASEJ);
        filaASEJ.setAlignment(Pos.CENTER_LEFT);
        return filaASEJ;
    }

    private Node extraASEJ(String grupoASEJ) {
        return switch (grupoASEJ) {
            case "email" -> pruebaCorreoASEJ();
            case "api_dni" -> pruebaDniASEJ();
            case "recordatorio" -> notaASEJ("Se envían cada 15 minutos a los pacientes con correo que aceptan recordatorios, "
                    + "si hay internet y el correo SMTP está configurado.");
            case "agenda" -> notaASEJ("Cambiar el intervalo no mueve las citas ya programadas; las nuevas se ajustan a la grilla.");
            case "respaldo" -> respaldosASEJ();
            default -> null;
        };
    }

    private static Label notaASEJ(String textoASEJ) {
        Label lASEJ = new Label(textoASEJ);
        lASEJ.setWrapText(true);
        lASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        return lASEJ;
    }

    private Node respaldosASEJ() {
        VBox cajaASEJ = new VBox(8);
        Label avisoASEJ = new Label();
        avisoASEJ.setWrapText(true);
        avisoASEJ.getStyleClass().add("etiqueta-aviso");
        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar restauración", Feather.X);
        Button cerrarASEJ = FormulariosASEJ.botonASEJ("Cerrar OdontoGest ahora", Feather.POWER, Styles.ACCENT);
        cerrarASEJ.setOnAction(eASEJ -> Platform.exit());
        HBox pendienteASEJ = new HBox(8, avisoASEJ, cancelarASEJ, cerrarASEJ);
        pendienteASEJ.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(avisoASEJ, Priority.ALWAYS);

        TableView<RespaldoDtoASEJ> tablaASEJ = new TableView<>();
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Fecha", rASEJ -> FormulariosASEJ.fechaHoraASEJ(rASEJ.fechaASEJ()), 130),
                FormulariosASEJ.columnaASEJ("Tipo", rASEJ -> rASEJ.automaticoASEJ() ? "Automático (al cerrar)" : "Manual", 150),
                FormulariosASEJ.columnaASEJ("Tamaño", RespaldoDtoASEJ::tamanioASEJ, 80),
                FormulariosASEJ.columnaASEJ("Archivo", RespaldoDtoASEJ::nombreASEJ, 260));
        tablaASEJ.setPrefHeight(190);
        tablaASEJ.setPlaceholder(new Label("Aún no hay respaldos."));

        Button crearASEJ = FormulariosASEJ.botonASEJ("Crear respaldo ahora", Feather.SAVE, Styles.ACCENT);
        Button restaurarSeleccionadoASEJ = FormulariosASEJ.botonASEJ("Restaurar el seleccionado…", Feather.ROTATE_CCW);
        Button restaurarArchivoASEJ = FormulariosASEJ.botonASEJ("Restaurar desde archivo…", Feather.UPLOAD);
        Button carpetaASEJ = FormulariosASEJ.botonASEJ("Abrir carpeta", Feather.FOLDER);
        restaurarSeleccionadoASEJ.disableProperty().bind(tablaASEJ.getSelectionModel().selectedItemProperty().isNull());

        Runnable cargarASEJ = () -> tareaUiASEJ.ejecutarASEJ(() -> new EstadoRespaldosASEJ(respaldoServiceASEJ.listarASEJ(),
                respaldoServiceASEJ.hayRestauracionPendienteASEJ()), estadoASEJ -> {
            tablaASEJ.getItems().setAll(estadoASEJ.respaldosASEJ());
            avisoASEJ.setText("Hay una restauración programada: se aplicará la próxima vez que abra OdontoGest. Los datos "
                    + "actuales se guardarán en la carpeta de respaldos (antes-de-restaurar-…).");
            pendienteASEJ.setVisible(estadoASEJ.restauracionPendienteASEJ());
            pendienteASEJ.setManaged(estadoASEJ.restauracionPendienteASEJ());
        });
        crearASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(crearASEJ, respaldoServiceASEJ::crearAhoraASEJ, rASEJ -> {
            notificadorASEJ.exitoASEJ("Respaldo creado: " + rASEJ.nombreASEJ() + " (" + rASEJ.tamanioASEJ() + ").");
            cargarASEJ.run();
        }));
        restaurarSeleccionadoASEJ.setOnAction(eASEJ -> {
            RespaldoDtoASEJ rASEJ = tablaASEJ.getSelectionModel().getSelectedItem();
            restaurarASEJ(restaurarSeleccionadoASEJ, Path.of(rASEJ.rutaASEJ()), "del " + FormulariosASEJ.fechaHoraASEJ(rASEJ.fechaASEJ()), cargarASEJ);
        });
        restaurarArchivoASEJ.setOnAction(eASEJ -> {
            FileChooser selectorASEJ = new FileChooser();
            selectorASEJ.setTitle("Elegir respaldo de OdontoGest");
            selectorASEJ.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo de OdontoGest (*.zip)", "*.zip"));
            File inicialASEJ = new File(respaldoServiceASEJ.carpetaRespaldosASEJ());
            if (inicialASEJ.isDirectory()) {
                selectorASEJ.setInitialDirectory(inicialASEJ);
            }
            File archivoASEJ = selectorASEJ.showOpenDialog(restaurarArchivoASEJ.getScene().getWindow());
            if (archivoASEJ != null) {
                restaurarASEJ(restaurarArchivoASEJ, archivoASEJ.toPath(), "«" + archivoASEJ.getName() + "»", cargarASEJ);
            }
        });
        carpetaASEJ.setOnAction(eASEJ -> exportadorASEJ.abrirASEJ(new File(respaldoServiceASEJ.carpetaRespaldosASEJ())));
        cancelarASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(cancelarASEJ, () -> {
            respaldoServiceASEJ.cancelarRestauracionASEJ();
            return null;
        }, rASEJ -> {
            notificadorASEJ.exitoASEJ("Restauración cancelada: se seguirá usando la base de datos actual.");
            cargarASEJ.run();
        }));

        FlowPane botonesASEJ = new FlowPane(8, 8, crearASEJ, restaurarSeleccionadoASEJ, restaurarArchivoASEJ, carpetaASEJ);
        cajaASEJ.getChildren().addAll(pendienteASEJ, botonesASEJ, tablaASEJ, notaASEJ("El respaldo es un archivo .zip con la base de datos, "
                + "los adjuntos y la clave de cifrado: guarde copias fuera de esta computadora (USB o nube). Los respaldos "
                + "automáticos se crean al cerrar la aplicación; los manuales no se borran solos."));
        cargarASEJ.run();
        return cajaASEJ;
    }

    private record EstadoRespaldosASEJ(List<RespaldoDtoASEJ> respaldosASEJ, boolean restauracionPendienteASEJ) {
    }

    private void restaurarASEJ(Button origenASEJ, Path zipASEJ, String descripcionASEJ, Runnable recargarASEJ) {
        if (!notificadorASEJ.confirmarASEJ("Restaurar respaldo", "Se reemplazarán TODOS los datos actuales por los del respaldo "
                + descripcionASEJ + ".\n\nLos datos actuales no se pierden: se guardan en la carpeta de respaldos. La "
                + "restauración se aplica al volver a abrir OdontoGest.\n\n¿Continuar?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(origenASEJ, () -> {
            respaldoServiceASEJ.programarRestauracionASEJ(zipASEJ);
            return null;
        }, rASEJ -> {
            recargarASEJ.run();
            if (notificadorASEJ.confirmarASEJ("Reiniciar", "Respaldo verificado. Para aplicarlo, OdontoGest debe cerrarse y volver "
                    + "a abrirse.\n\n¿Cerrar la aplicación ahora?")) {
                Platform.exit();
            }
        });
    }

    private Node pruebaCorreoASEJ() {
        TextField destinoASEJ = new TextField();
        destinoASEJ.setPromptText("Correo de destino para la prueba");
        Button enviarASEJ = FormulariosASEJ.botonASEJ("Enviar correo de prueba", Feather.SEND);
        enviarASEJ.setOnAction(eASEJ -> conCambiosGuardadosASEJ(() -> {
            String dASEJ = destinoASEJ.getText();
            tareaUiASEJ.ejecutarASEJ(enviarASEJ, () -> {
                integracionServiceASEJ.enviarCorreoPruebaASEJ(dASEJ);
                return null;
            }, rASEJ -> notificadorASEJ.exitoASEJ("Correo de prueba enviado a " + dASEJ.trim() + ". Revise la bandeja de entrada (y spam)."));
        }));
        HBox filaASEJ = new HBox(8, destinoASEJ, enviarASEJ);
        HBox.setHgrow(destinoASEJ, Priority.ALWAYS);
        return new VBox(6, filaASEJ, notaASEJ("En Gmail active la verificación en dos pasos y use una «contraseña de aplicación»."));
    }

    private Node pruebaDniASEJ() {
        TextField dniASEJ = new TextField();
        dniASEJ.setPromptText("DNI de prueba (8 dígitos)");
        dniASEJ.setMaxWidth(200);
        Label resultadoASEJ = new Label();
        resultadoASEJ.setWrapText(true);
        Button probarASEJ = FormulariosASEJ.botonASEJ("Probar conexión", Feather.ZAP);
        probarASEJ.setOnAction(eASEJ -> conCambiosGuardadosASEJ(() -> {
            String numeroASEJ = dniASEJ.getText();
            resultadoASEJ.setText("");
            tareaUiASEJ.ejecutarASEJ(probarASEJ, () -> integracionServiceASEJ.probarDniASEJ(numeroASEJ), rASEJ -> {
                resultadoASEJ.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING);
                if (rASEJ.isPresent()) {
                    resultadoASEJ.setText("✔ Conexión correcta: " + rASEJ.get().nombresASEJ() + " " + rASEJ.get().apellidoPaternoASEJ()
                            + (rASEJ.get().apellidoMaternoASEJ() == null ? "" : " " + rASEJ.get().apellidoMaternoASEJ()));
                    resultadoASEJ.getStyleClass().add(Styles.SUCCESS);
                } else {
                    resultadoASEJ.setText("La API respondió, pero no encontró ese DNI.");
                    resultadoASEJ.getStyleClass().add(Styles.WARNING);
                }
            });
        }));
        HBox filaASEJ = new HBox(8, dniASEJ, probarASEJ);
        return new VBox(6, filaASEJ, resultadoASEJ, notaASEJ("El proveedor no se fija en el sistema: la URL usa {numero}, la cabecera y el "
                + "prefijo arman la autenticación y el mapeo indica qué campos del JSON leer (admite rutas como data.nombres)."));
    }

    private void conCambiosGuardadosASEJ(Runnable accionASEJ) {
        Map<String, String> cambiosASEJ = cambiosASEJ();
        if (cambiosASEJ.isEmpty()) {
            accionASEJ.run();
            return;
        }
        if (!notificadorASEJ.confirmarASEJ("Guardar cambios", "La prueba usa la configuración guardada. ¿Guardar los cambios antes de probar?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnGuardarASEJ, () -> {
            configuracionServiceASEJ.guardarASEJ(cambiosASEJ);
            return null;
        }, rASEJ -> {
            notificadorASEJ.exitoASEJ("Configuración guardada.");
            limpiarSecretosEscritosASEJ();
            cargarASEJ();
            accionASEJ.run();
        });
    }

    private Map<String, String> cambiosASEJ() {
        Map<String, String> cambiosASEJ = new LinkedHashMap<>();
        lectoresASEJ.forEach((claveASEJ, lectorASEJ) -> {
            String vASEJ = lectorASEJ.get() == null ? "" : lectorASEJ.get().trim();
            if (!vASEJ.equals(originalesASEJ.getOrDefault(claveASEJ, "").trim())) {
                cambiosASEJ.put(claveASEJ, vASEJ);
            }
        });
        secretosASEJ.forEach((claveASEJ, campoASEJ) -> {
            if (!campoASEJ.getText().isEmpty()) {
                cambiosASEJ.put(claveASEJ, campoASEJ.getText());
            } else if (borrarSecretoASEJ.getOrDefault(claveASEJ, false)) {
                cambiosASEJ.put(claveASEJ, "");
            }
        });
        return cambiosASEJ;
    }

    private void actualizarCambiosASEJ() {
        int nASEJ = cambiosASEJ().size();
        lblCambiosASEJ.setText(nASEJ == 0 ? "Sin cambios" : nASEJ + " cambio(s) sin guardar");
        btnGuardarASEJ.setDisable(nASEJ == 0);
        btnDescartarASEJ.setDisable(nASEJ == 0);
    }

    private void limpiarSecretosEscritosASEJ() {
        secretosASEJ.values().forEach(TextInputControl::clear);
    }

    @FXML
    private void onGuardarASEJ() {
        Map<String, String> cambiosASEJ = cambiosASEJ();
        if (cambiosASEJ.isEmpty()) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnGuardarASEJ, () -> {
            configuracionServiceASEJ.guardarASEJ(cambiosASEJ);
            return null;
        }, rASEJ -> {
            notificadorASEJ.exitoASEJ("Configuración guardada (" + cambiosASEJ.size() + " cambio(s)).");
            limpiarSecretosEscritosASEJ();
            cargarASEJ();
        });
    }

    @FXML
    private void onDescartarASEJ() {
        cargarASEJ();
    }
}
