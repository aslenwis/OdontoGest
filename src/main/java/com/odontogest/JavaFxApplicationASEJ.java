package com.odontogest;

import com.odontogest.config.RestauracionPendienteASEJ;
import com.odontogest.integracion.ConectividadServiceASEJ;
import com.odontogest.integracion.RecordatorioSchedulerASEJ;
import com.odontogest.servicio.ConfiguracionServiceASEJ;
import com.odontogest.servicio.RespaldoServiceASEJ;
import com.odontogest.ui.soporte.CargadorVistasASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.TemaASEJ;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class JavaFxApplicationASEJ extends Application {

    private ConfigurableApplicationContext contextASEJ;
    private TemaASEJ temaInicialASEJ = TemaASEJ.CLARO;
    private Throwable errorArranqueASEJ;

    @Override
    public void init() {
        try {
            contextASEJ = new SpringApplicationBuilder(OdontoGestApplicationASEJ.class)
                    .web(WebApplicationType.NONE)
                    .headless(false)
                    .run(getParameters().getRaw().toArray(String[]::new));
            temaInicialASEJ = TemaASEJ.desdeASEJ(contextASEJ.getBean(ConfiguracionServiceASEJ.class).textoASEJ("ui.tema"));
        } catch (Exception eASEJ) {
            logASEJ.error("No se pudo iniciar OdontoGest", eASEJ);
            errorArranqueASEJ = eASEJ;
        }
    }

    @Override
    public void start(Stage stageASEJ) {
        if (errorArranqueASEJ != null) {
            Alert alertaASEJ = new Alert(Alert.AlertType.ERROR,
                    "No se pudo iniciar la aplicación.\n\n" + errorArranqueASEJ.getMessage(), ButtonType.OK);
            alertaASEJ.setTitle("OdontoGest");
            alertaASEJ.setHeaderText("Error al iniciar");
            alertaASEJ.showAndWait();
            Platform.exit();
            return;
        }
        temaInicialASEJ.aplicarASEJ();
        if (Boolean.getBoolean(RestauracionPendienteASEJ.PROPIEDAD_APLICADAASEJ)) {
            logASEJ.warn("Se aplicó un respaldo programado al iniciar; la base anterior quedó en respaldos/antes-de-restaurar-*");
            Alert avisoASEJ = new Alert(Alert.AlertType.INFORMATION, "Se restauró el respaldo programado. Los datos que había "
                    + "antes quedaron guardados en la carpeta de respaldos (antes-de-restaurar-…).", ButtonType.OK);
            avisoASEJ.setTitle("OdontoGest");
            avisoASEJ.setHeaderText("Respaldo restaurado");
            avisoASEJ.showAndWait();
        }
        contextASEJ.getBean(ExportadorASEJ.class).setHostServicesASEJ(getHostServices());
        contextASEJ.getBean(CargadorVistasASEJ.class).mostrarLoginASEJ(stageASEJ);
        contextASEJ.getBean(ConectividadServiceASEJ.class).iniciarMonitoreoASEJ();
        contextASEJ.getBean(RecordatorioSchedulerASEJ.class).iniciarASEJ();
    }

    @Override
    public void stop() {
        if (contextASEJ != null) {
            contextASEJ.getBean(RecordatorioSchedulerASEJ.class).detenerASEJ();
            contextASEJ.getBean(ConectividadServiceASEJ.class).detenerASEJ();
            contextASEJ.getBean(RespaldoServiceASEJ.class).respaldoAutomaticoAlCerrarASEJ();
            contextASEJ.close();
        }
        Platform.exit();
    }
}
