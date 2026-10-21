/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest;

import com.odontogest.config.RestauracionPendiente;
import com.odontogest.integracion.ConectividadService;
import com.odontogest.integracion.RecordatorioScheduler;
import com.odontogest.servicio.ConfiguracionService;
import com.odontogest.servicio.RespaldoService;
import com.odontogest.ui.soporte.CargadorVistas;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Tema;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Ciclo de vida JavaFX: crea el contexto de Spring en {@link #init()} (fuera del hilo de UI)
 * y entrega la ventana principal a {@link CargadorVistas}.
 */
@Slf4j
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;
    private Tema temaInicial = Tema.CLARO;
    private Throwable errorArranque;

    @Override
    public void init() {
        try {
            context = new SpringApplicationBuilder(OdontoGestApplication.class)
                    .web(WebApplicationType.NONE)
                    .headless(false)
                    .run(getParameters().getRaw().toArray(String[]::new));
            // init() corre en el hilo del launcher: aquí sí se puede leer la BD.
            temaInicial = Tema.desde(context.getBean(ConfiguracionService.class).texto("ui.tema"));
        } catch (Exception e) {
            log.error("No se pudo iniciar OdontoGest", e);
            errorArranque = e;
        }
    }

    @Override
    public void start(Stage stage) {
        if (errorArranque != null) {
            Alert alerta = new Alert(Alert.AlertType.ERROR,
                    "No se pudo iniciar la aplicación.\n\n" + errorArranque.getMessage(), ButtonType.OK);
            alerta.setTitle("OdontoGest");
            alerta.setHeaderText("Error al iniciar");
            alerta.showAndWait();
            Platform.exit();
            return;
        }
        temaInicial.aplicar();
        if (Boolean.getBoolean(RestauracionPendiente.PROPIEDAD_APLICADA)) {
            log.warn("Se aplicó un respaldo programado al iniciar; la base anterior quedó en respaldos/antes-de-restaurar-*");
            Alert aviso = new Alert(Alert.AlertType.INFORMATION, "Se restauró el respaldo programado. Los datos que había "
                    + "antes quedaron guardados en la carpeta de respaldos (antes-de-restaurar-…).", ButtonType.OK);
            aviso.setTitle("OdontoGest");
            aviso.setHeaderText("Respaldo restaurado");
            aviso.showAndWait();
        }
        context.getBean(Exportador.class).setHostServices(getHostServices());
        context.getBean(CargadorVistas.class).mostrarLogin(stage);
        // Fase 7: aviso de conectividad y recordatorios en hilos de fondo (no bloquean la interfaz).
        context.getBean(ConectividadService.class).iniciarMonitoreo();
        context.getBean(RecordatorioScheduler.class).iniciar();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.getBean(RecordatorioScheduler.class).detener();
            context.getBean(ConectividadService.class).detener();
            // Con los hilos de fondo detenidos: respaldo automático si está habilitado (PLAN §5.4).
            context.getBean(RespaldoService.class).respaldoAutomaticoAlCerrar();
            context.close();
        }
        Platform.exit();
    }
}
