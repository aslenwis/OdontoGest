/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.ConectividadCambiada;
import com.odontogest.modelo.TipoConfiguracion;
import com.odontogest.servicio.ConfiguracionService;
import com.odontogest.servicio.IntegracionService;
import com.odontogest.servicio.RespaldoService;
import com.odontogest.servicio.dto.ConfiguracionDto;
import com.odontogest.servicio.dto.RespaldoDto;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.HiloUi;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
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

/**
 * Configuración (RF-CFG-01 a RF-CFG-03): datos y logo de la clínica, agenda, recordatorios, correo (con correo de
 * prueba), API de DNI (con "Probar conexión") y demás parámetros. Los paneles se generan desde la tabla
 * {@code configuracion}; los secretos se muestran como campos de contraseña vacíos y solo se reemplazan si se escribe
 * un valor nuevo.
 */
@Component
@RequiredArgsConstructor
public class ConfiguracionController implements Refrescable {

    /** Orden y título de los grupos; los que no están aquí no se muestran (el tema se cambia desde la barra lateral). */
    private static final Map<String, String> GRUPOS = orden();

    private final ConfiguracionService configuracionService;
    private final IntegracionService integracionService;
    private final RespaldoService respaldoService;
    private final Exportador exportador;
    private final TareaUi tareaUi;
    private final Notificador notificador;

    @FXML private VBox grupos;
    @FXML private Label lblConexion;
    @FXML private Label lblCambios;
    @FXML private Button btnDescartar;
    @FXML private Button btnGuardar;

    /** Valor original de cada clave no secreta (para enviar solo lo que cambió). */
    private final Map<String, String> originales = new HashMap<>();
    private final Map<String, Supplier<String>> lectores = new LinkedHashMap<>();
    /** Campos de secretos y secretos marcados para borrar. */
    private final Map<String, PasswordField> secretos = new HashMap<>();
    private final Map<String, Boolean> borrarSecreto = new HashMap<>();

    /** Etiquetas más claras que la descripción guardada para algunas claves, y su texto de ayuda. */
    private static final Map<String, String[]> ETIQUETAS = Map.of(
            "api.dni.url_plantilla", new String[]{"URL de la API (con {numero})", "https://<proveedor>/v1/dni/{numero}"},
            "api.dni.mapeo_json", new String[]{"Campos del JSON (mapeo)", null},
            "api.dni.prefijo_token", new String[]{"Prefijo del token", "Bearer "});

    private static Map<String, String> orden() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("clinica", "Clínica (aparece en todos los documentos PDF)");
        m.put("agenda", "Agenda");
        m.put("recordatorio", "Recordatorios de citas por correo");
        m.put("email", "Correo (SMTP)");
        m.put("api_dni", "API de consulta de DNI");
        m.put("odontograma", "Odontograma");
        m.put("respaldo", "Respaldo");
        return m;
    }

    @FXML
    public void initialize() {
        mostrarConexion(integracionService.enLinea());
    }

    @Override
    public void refrescar() {
        cargar();
    }

    @EventListener
    public void alCambiarConectividad(ConectividadCambiada e) {
        HiloUi.ejecutar(() -> {
            if (lblConexion != null) {
                mostrarConexion(e.enLinea());
            }
        });
    }

    private void mostrarConexion(boolean enLinea) {
        lblConexion.setText(enLinea ? "● Conectado a internet" : "● Sin conexión a internet: DNI y correo no disponibles");
        lblConexion.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING);
        lblConexion.getStyleClass().add(enLinea ? Styles.SUCCESS : Styles.WARNING);
    }

    private void cargar() {
        tareaUi.ejecutar(configuracionService::listar, this::construir);
    }

    // ------------------------------------------------------------------ construcción

    private void construir(List<ConfiguracionDto> claves) {
        grupos.getChildren().clear();
        originales.clear();
        lectores.clear();
        secretos.clear();
        borrarSecreto.clear();
        Map<String, List<ConfiguracionDto>> porGrupo = claves.stream()
                .collect(Collectors.groupingBy(ConfiguracionDto::grupo, LinkedHashMap::new, Collectors.toList()));
        GRUPOS.forEach((grupo, titulo) -> {
            List<ConfiguracionDto> lista = porGrupo.get(grupo);
            if (lista == null) {
                return;
            }
            GridPane g = Formularios.grilla();
            g.getColumnConstraints().get(0).setMinWidth(250);
            g.getColumnConstraints().get(0).setMaxWidth(250);       // misma columna de etiquetas en todos los grupos
            for (ConfiguracionDto c : lista) {
                agregarCampo(g, c);
            }
            VBox contenido = new VBox(12, g);
            Node extra = extra(grupo);
            if (extra != null) {
                contenido.getChildren().add(extra);
            }
            TitledPane panel = new TitledPane(titulo, contenido);
            panel.setExpanded(!grupo.equals("odontograma") && !grupo.equals("respaldo"));
            grupos.getChildren().add(panel);
        });
        actualizarCambios();
    }

    private void agregarCampo(GridPane g, ConfiguracionDto c) {
        String[] propia = ETIQUETAS.get(c.clave());
        String etiqueta = propia != null ? propia[0] : c.descripcion() == null ? c.clave() : c.descripcion();
        String ayuda = propia == null ? null : propia[1];
        String valor = c.valor() == null ? "" : c.valor();
        switch (c.tipo()) {
            case BOOLEANO -> {
                CheckBox chk = new CheckBox(etiqueta);
                chk.setSelected(Boolean.parseBoolean(valor));
                chk.selectedProperty().addListener((obs, a, b) -> actualizarCambios());
                registrar(c.clave(), valor, () -> String.valueOf(chk.isSelected()));
                Formularios.fila(g, "", chk);
            }
            case SECRETO -> {
                PasswordField campo = new PasswordField();
                campo.setPromptText(c.secretoConfigurado() ? "•••••• guardado (escriba para reemplazarlo)" : "Sin configurar");
                campo.textProperty().addListener((obs, a, b) -> actualizarCambios());
                Button borrar = Formularios.boton("Borrar", Feather.TRASH_2, Styles.FLAT);
                borrar.setDisable(!c.secretoConfigurado());
                borrar.setOnAction(e -> {
                    borrarSecreto.put(c.clave(), true);
                    campo.clear();
                    campo.setPromptText("Se borrará al guardar");
                    borrar.setDisable(true);
                    actualizarCambios();
                });
                secretos.put(c.clave(), campo);
                HBox fila = new HBox(8, campo, borrar);
                HBox.setHgrow(campo, Priority.ALWAYS);
                Formularios.fila(g, etiqueta, fila);
            }
            case JSON -> {
                TextArea area = new TextArea(valor);
                area.setPrefRowCount(3);
                area.setWrapText(true);
                area.setStyle("-fx-font-family: 'Consolas', monospace;");
                escuchar(area);
                registrar(c.clave(), valor, area::getText);
                Formularios.fila(g, etiqueta, area);
            }
            default -> {
                if (c.clave().equals("clinica.logo_ruta")) {
                    Formularios.fila(g, "Logo", campoLogo(valor));
                    return;
                }
                TextField campo = new TextField(valor);
                campo.setPromptText(ayuda);
                escuchar(campo);
                registrar(c.clave(), valor, campo::getText);
                Formularios.fila(g, etiqueta, campo);
                if (c.tipo() == TipoConfiguracion.ENTERO) {
                    campo.setMaxWidth(120);           // fila() lo estira; un número no necesita todo el ancho
                }
            }
        }
    }

    private void registrar(String clave, String original, Supplier<String> lector) {
        originales.put(clave, original);
        lectores.put(clave, lector);
    }

    private void escuchar(TextInputControl campo) {
        campo.textProperty().addListener((obs, a, b) -> actualizarCambios());
    }

    /** Logo: vista previa, elegir (se copia a la carpeta de datos) y quitar. Se guarda al instante. */
    private Node campoLogo(String ruta) {
        ImageView vista = new ImageView();
        vista.setFitHeight(48);
        vista.setFitWidth(160);
        vista.setPreserveRatio(true);
        Label sinLogo = new Label("Sin logo");
        sinLogo.getStyleClass().add(Styles.TEXT_MUTED);
        if (!ruta.isBlank() && new File(ruta).isFile()) {
            vista.setImage(new Image(new File(ruta).toURI().toString(), true));
            sinLogo.setVisible(false);
            sinLogo.setManaged(false);
        }
        Button elegir = Formularios.boton("Elegir imagen…", Feather.IMAGE);
        elegir.setOnAction(e -> {
            FileChooser selector = new FileChooser();
            selector.setTitle("Logo de la clínica");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG o JPG", "*.png", "*.jpg", "*.jpeg"));
            File archivo = selector.showOpenDialog(grupos.getScene().getWindow());
            if (archivo != null) {
                tareaUi.ejecutar(elegir, () -> configuracionService.guardarLogo(archivo.toPath()), r -> {
                    notificador.exito("Logo actualizado: aparecerá en los PDF.");
                    cargar();
                });
            }
        });
        Button quitar = Formularios.boton("Quitar", Feather.X, Styles.FLAT);
        quitar.setDisable(ruta.isBlank());
        quitar.setOnAction(e -> tareaUi.ejecutar(quitar, () -> {
            configuracionService.guardar(Map.of("clinica.logo_ruta", ""));
            return null;
        }, r -> {
            notificador.exito("Logo quitado.");
            cargar();
        }));
        HBox fila = new HBox(10, vista, sinLogo, elegir, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /** Acciones propias de algunos grupos. */
    private Node extra(String grupo) {
        return switch (grupo) {
            case "email" -> pruebaCorreo();
            case "api_dni" -> pruebaDni();
            case "recordatorio" -> nota("Se envían cada 15 minutos a los pacientes con correo que aceptan recordatorios, "
                    + "si hay internet y el correo SMTP está configurado.");
            case "agenda" -> nota("Cambiar el intervalo no mueve las citas ya programadas; las nuevas se ajustan a la grilla.");
            case "respaldo" -> respaldos();
            default -> null;
        };
    }

    private static Label nota(String texto) {
        Label l = new Label(texto);
        l.setWrapText(true);
        l.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        return l;
    }

    // ------------------------------------------------------------------ respaldos (PLAN §5.4)

    private Node respaldos() {
        VBox caja = new VBox(8);
        Label aviso = new Label();
        aviso.setWrapText(true);
        aviso.getStyleClass().add("etiqueta-aviso");
        Button cancelar = Formularios.boton("Cancelar restauración", Feather.X);
        Button cerrar = Formularios.boton("Cerrar OdontoGest ahora", Feather.POWER, Styles.ACCENT);
        cerrar.setOnAction(e -> Platform.exit());
        HBox pendiente = new HBox(8, aviso, cancelar, cerrar);
        pendiente.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(aviso, Priority.ALWAYS);

        TableView<RespaldoDto> tabla = new TableView<>();
        Formularios.columnas(tabla,
                Formularios.columna("Fecha", r -> Formularios.fechaHora(r.fecha()), 130),
                Formularios.columna("Tipo", r -> r.automatico() ? "Automático (al cerrar)" : "Manual", 150),
                Formularios.columna("Tamaño", RespaldoDto::tamanio, 80),
                Formularios.columna("Archivo", RespaldoDto::nombre, 260));
        tabla.setPrefHeight(190);
        tabla.setPlaceholder(new Label("Aún no hay respaldos."));

        Button crear = Formularios.boton("Crear respaldo ahora", Feather.SAVE, Styles.ACCENT);
        Button restaurarSeleccionado = Formularios.boton("Restaurar el seleccionado…", Feather.ROTATE_CCW);
        Button restaurarArchivo = Formularios.boton("Restaurar desde archivo…", Feather.UPLOAD);
        Button carpeta = Formularios.boton("Abrir carpeta", Feather.FOLDER);
        restaurarSeleccionado.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        Runnable cargar = () -> tareaUi.ejecutar(() -> new EstadoRespaldos(respaldoService.listar(),
                respaldoService.hayRestauracionPendiente()), estado -> {
            tabla.getItems().setAll(estado.respaldos());
            aviso.setText("Hay una restauración programada: se aplicará la próxima vez que abra OdontoGest. Los datos "
                    + "actuales se guardarán en la carpeta de respaldos (antes-de-restaurar-…).");
            pendiente.setVisible(estado.restauracionPendiente());
            pendiente.setManaged(estado.restauracionPendiente());
        });
        crear.setOnAction(e -> tareaUi.ejecutar(crear, respaldoService::crearAhora, r -> {
            notificador.exito("Respaldo creado: " + r.nombre() + " (" + r.tamanio() + ").");
            cargar.run();
        }));
        restaurarSeleccionado.setOnAction(e -> {
            RespaldoDto r = tabla.getSelectionModel().getSelectedItem();
            restaurar(restaurarSeleccionado, Path.of(r.ruta()), "del " + Formularios.fechaHora(r.fecha()), cargar);
        });
        restaurarArchivo.setOnAction(e -> {
            FileChooser selector = new FileChooser();
            selector.setTitle("Elegir respaldo de OdontoGest");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo de OdontoGest (*.zip)", "*.zip"));
            File inicial = new File(respaldoService.carpetaRespaldos());
            if (inicial.isDirectory()) {
                selector.setInitialDirectory(inicial);
            }
            File archivo = selector.showOpenDialog(restaurarArchivo.getScene().getWindow());
            if (archivo != null) {
                restaurar(restaurarArchivo, archivo.toPath(), "«" + archivo.getName() + "»", cargar);
            }
        });
        carpeta.setOnAction(e -> exportador.abrir(new File(respaldoService.carpetaRespaldos())));
        cancelar.setOnAction(e -> tareaUi.ejecutar(cancelar, () -> {
            respaldoService.cancelarRestauracion();
            return null;
        }, r -> {
            notificador.exito("Restauración cancelada: se seguirá usando la base de datos actual.");
            cargar.run();
        }));

        FlowPane botones = new FlowPane(8, 8, crear, restaurarSeleccionado, restaurarArchivo, carpeta);
        caja.getChildren().addAll(pendiente, botones, tabla, nota("El respaldo es un archivo .zip con la base de datos, "
                + "los adjuntos y la clave de cifrado: guarde copias fuera de esta computadora (USB o nube). Los respaldos "
                + "automáticos se crean al cerrar la aplicación; los manuales no se borran solos."));
        cargar.run();
        return caja;
    }

    private record EstadoRespaldos(List<RespaldoDto> respaldos, boolean restauracionPendiente) {
    }

    private void restaurar(Button origen, Path zip, String descripcion, Runnable recargar) {
        if (!notificador.confirmar("Restaurar respaldo", "Se reemplazarán TODOS los datos actuales por los del respaldo "
                + descripcion + ".\n\nLos datos actuales no se pierden: se guardan en la carpeta de respaldos. La "
                + "restauración se aplica al volver a abrir OdontoGest.\n\n¿Continuar?")) {
            return;
        }
        tareaUi.ejecutar(origen, () -> {
            respaldoService.programarRestauracion(zip);
            return null;
        }, r -> {
            recargar.run();
            if (notificador.confirmar("Reiniciar", "Respaldo verificado. Para aplicarlo, OdontoGest debe cerrarse y volver "
                    + "a abrirse.\n\n¿Cerrar la aplicación ahora?")) {
                Platform.exit();
            }
        });
    }

    private Node pruebaCorreo() {
        TextField destino = new TextField();
        destino.setPromptText("Correo de destino para la prueba");
        Button enviar = Formularios.boton("Enviar correo de prueba", Feather.SEND);
        enviar.setOnAction(e -> conCambiosGuardados(() -> {
            String d = destino.getText();
            tareaUi.ejecutar(enviar, () -> {
                integracionService.enviarCorreoPrueba(d);
                return null;
            }, r -> notificador.exito("Correo de prueba enviado a " + d.trim() + ". Revise la bandeja de entrada (y spam)."));
        }));
        HBox fila = new HBox(8, destino, enviar);
        HBox.setHgrow(destino, Priority.ALWAYS);
        return new VBox(6, fila, nota("En Gmail active la verificación en dos pasos y use una «contraseña de aplicación»."));
    }

    private Node pruebaDni() {
        TextField dni = new TextField();
        dni.setPromptText("DNI de prueba (8 dígitos)");
        dni.setMaxWidth(200);
        Label resultado = new Label();
        resultado.setWrapText(true);
        Button probar = Formularios.boton("Probar conexión", Feather.ZAP);
        probar.setOnAction(e -> conCambiosGuardados(() -> {
            String numero = dni.getText();
            resultado.setText("");
            tareaUi.ejecutar(probar, () -> integracionService.probarDni(numero), r -> {
                resultado.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING);
                if (r.isPresent()) {
                    resultado.setText("✔ Conexión correcta: " + r.get().nombres() + " " + r.get().apellidoPaterno()
                            + (r.get().apellidoMaterno() == null ? "" : " " + r.get().apellidoMaterno()));
                    resultado.getStyleClass().add(Styles.SUCCESS);
                } else {
                    resultado.setText("La API respondió, pero no encontró ese DNI.");
                    resultado.getStyleClass().add(Styles.WARNING);
                }
            });
        }));
        HBox fila = new HBox(8, dni, probar);
        return new VBox(6, fila, resultado, nota("El proveedor no se fija en el sistema: la URL usa {numero}, la cabecera y el "
                + "prefijo arman la autenticación y el mapeo indica qué campos del JSON leer (admite rutas como data.nombres)."));
    }

    /** Las pruebas usan la configuración guardada: si hay cambios, se guardan antes con permiso del usuario. */
    private void conCambiosGuardados(Runnable accion) {
        Map<String, String> cambios = cambios();
        if (cambios.isEmpty()) {
            accion.run();
            return;
        }
        if (!notificador.confirmar("Guardar cambios", "La prueba usa la configuración guardada. ¿Guardar los cambios antes de probar?")) {
            return;
        }
        tareaUi.ejecutar(btnGuardar, () -> {
            configuracionService.guardar(cambios);
            return null;
        }, r -> {
            notificador.exito("Configuración guardada.");
            limpiarSecretosEscritos();
            cargar();
            accion.run();
        });
    }

    // ------------------------------------------------------------------ guardar

    private Map<String, String> cambios() {
        Map<String, String> cambios = new LinkedHashMap<>();
        lectores.forEach((clave, lector) -> {
            String v = lector.get() == null ? "" : lector.get().trim();
            if (!v.equals(originales.getOrDefault(clave, "").trim())) {
                cambios.put(clave, v);
            }
        });
        secretos.forEach((clave, campo) -> {
            if (!campo.getText().isEmpty()) {
                cambios.put(clave, campo.getText());
            } else if (borrarSecreto.getOrDefault(clave, false)) {
                cambios.put(clave, "");
            }
        });
        return cambios;
    }

    private void actualizarCambios() {
        int n = cambios().size();
        lblCambios.setText(n == 0 ? "Sin cambios" : n + " cambio(s) sin guardar");
        btnGuardar.setDisable(n == 0);
        btnDescartar.setDisable(n == 0);
    }

    private void limpiarSecretosEscritos() {
        secretos.values().forEach(TextInputControl::clear);
    }

    @FXML
    private void onGuardar() {
        Map<String, String> cambios = cambios();
        if (cambios.isEmpty()) {
            return;
        }
        tareaUi.ejecutar(btnGuardar, () -> {
            configuracionService.guardar(cambios);
            return null;
        }, r -> {
            notificador.exito("Configuración guardada (" + cambios.size() + " cambio(s)).");
            limpiarSecretosEscritos();
            cargar();
        });
    }

    @FXML
    private void onDescartar() {
        cargar();
    }
}
