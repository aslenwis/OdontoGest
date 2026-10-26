package com.odontogest.ui.soporte;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;
import com.odontogest.excepcion.IntegracionExceptionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TareaUiASEJ {

    private static final String GRAFICO_ORIGINALASEJ = "tareaUi.graficoOriginal";

    private final ExecutorService ejecutorASEJ;
    private final NotificadorASEJ notificadorASEJ;

    public TareaUiASEJ(@Qualifier("ejecutorUi") ExecutorService ejecutorASEJ, NotificadorASEJ notificadorASEJ) {
        this.ejecutorASEJ = ejecutorASEJ;
        this.notificadorASEJ = notificadorASEJ;
    }

    public <T> Task<T> ejecutarASEJ(Callable<T> trabajoASEJ, Consumer<T> alExitoASEJ) {
        return ejecutarASEJ(null, trabajoASEJ, alExitoASEJ, this::manejarErrorASEJ);
    }

    public <T> Task<T> ejecutarASEJ(Callable<T> trabajoASEJ, Consumer<T> alExitoASEJ, Consumer<Throwable> alFallarASEJ) {
        return ejecutarASEJ(null, trabajoASEJ, alExitoASEJ, alFallarASEJ);
    }

    public <T> Task<T> ejecutarASEJ(Node origenASEJ, Callable<T> trabajoASEJ, Consumer<T> alExitoASEJ) {
        return ejecutarASEJ(origenASEJ, trabajoASEJ, alExitoASEJ, this::manejarErrorASEJ);
    }

    public <T> Task<T> ejecutarASEJ(Node origenASEJ, Callable<T> trabajoASEJ, Consumer<T> alExitoASEJ, Consumer<Throwable> alFallarASEJ) {
        Task<T> tareaASEJ = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajoASEJ.call();
            }
        };
        marcarOcupadoASEJ(origenASEJ);
        tareaASEJ.setOnSucceeded(eASEJ -> {
            liberarASEJ(origenASEJ);
            alExitoASEJ.accept(tareaASEJ.getValue());
        });
        tareaASEJ.setOnFailed(eASEJ -> {
            liberarASEJ(origenASEJ);
            alFallarASEJ.accept(tareaASEJ.getException());
        });
        ejecutorASEJ.execute(tareaASEJ);
        return tareaASEJ;
    }

    public void manejarErrorASEJ(Throwable errorASEJ) {
        Throwable causaASEJ = causaConocidaASEJ(errorASEJ);
        if (causaASEJ instanceof NegocioExceptionASEJ || causaASEJ instanceof ReglaDominioExceptionASEJ
                || causaASEJ instanceof PermisoDenegadoExceptionASEJ) {
            notificadorASEJ.advertenciaASEJ(causaASEJ.getMessage());
        } else if (causaASEJ instanceof ConstraintViolationException cveASEJ) {
            notificadorASEJ.advertenciaASEJ(cveASEJ.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("\n")));
        } else if (causaASEJ instanceof IntegracionExceptionASEJ) {
            notificadorASEJ.infoASEJ(causaASEJ.getMessage());
        } else {
            logASEJ.error("Error inesperado en tarea de fondo", errorASEJ);
            notificadorASEJ.errorASEJ("Ocurrió un error inesperado. Revise el registro.");
        }
    }

    private static Throwable causaConocidaASEJ(Throwable errorASEJ) {
        for (Throwable tASEJ = errorASEJ; tASEJ != null; tASEJ = tASEJ.getCause()) {
            if (tASEJ instanceof NegocioExceptionASEJ || tASEJ instanceof ReglaDominioExceptionASEJ || tASEJ instanceof PermisoDenegadoExceptionASEJ
                    || tASEJ instanceof IntegracionExceptionASEJ || tASEJ instanceof ConstraintViolationException) {
                return tASEJ;
            }
            if (tASEJ.getCause() == tASEJ) {
                break;
            }
        }
        return errorASEJ;
    }

    private static void marcarOcupadoASEJ(Node origenASEJ) {
        if (origenASEJ == null) {
            return;
        }
        origenASEJ.setDisable(true);
        origenASEJ.setCursor(Cursor.WAIT);
        if (origenASEJ instanceof Labeled etiquetadoASEJ) {
            ProgressIndicator progresoASEJ = new ProgressIndicator();
            progresoASEJ.setPrefSize(16, 16);
            progresoASEJ.setMaxSize(16, 16);
            origenASEJ.getProperties().put(GRAFICO_ORIGINALASEJ, etiquetadoASEJ.getGraphic());
            etiquetadoASEJ.setGraphic(progresoASEJ);
        }
    }

    private static void liberarASEJ(Node origenASEJ) {
        if (origenASEJ == null) {
            return;
        }
        origenASEJ.setDisable(false);
        origenASEJ.setCursor(Cursor.DEFAULT);
        if (origenASEJ instanceof Labeled etiquetadoASEJ && origenASEJ.getProperties().containsKey(GRAFICO_ORIGINALASEJ)) {
            etiquetadoASEJ.setGraphic((Node) origenASEJ.getProperties().remove(GRAFICO_ORIGINALASEJ));
        }
    }
}
