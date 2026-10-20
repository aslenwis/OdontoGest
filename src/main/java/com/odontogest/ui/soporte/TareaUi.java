/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import com.odontogest.excepcion.IntegracionException;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
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

/**
 * Ejecuta trabajo de BD, archivos o red fuera del JavaFX Application Thread (RNF-05).
 * <p>
 * El trabajo corre en el ejecutor {@code ejecutorUi}; los callbacks de éxito y error se invocan en
 * el hilo de UI. Los errores sin callback propio se muestran con {@link Notificador} según su tipo.
 */
@Component
@Slf4j
public class TareaUi {

    private static final String GRAFICO_ORIGINAL = "tareaUi.graficoOriginal";

    private final ExecutorService ejecutor;
    private final Notificador notificador;

    public TareaUi(@Qualifier("ejecutorUi") ExecutorService ejecutor, Notificador notificador) {
        this.ejecutor = ejecutor;
        this.notificador = notificador;
    }

    public <T> Task<T> ejecutar(Callable<T> trabajo, Consumer<T> alExito) {
        return ejecutar(null, trabajo, alExito, this::manejarError);
    }

    public <T> Task<T> ejecutar(Callable<T> trabajo, Consumer<T> alExito, Consumer<Throwable> alFallar) {
        return ejecutar(null, trabajo, alExito, alFallar);
    }

    /** Deshabilita {@code origen} y muestra un indicador de progreso mientras corre. */
    public <T> Task<T> ejecutar(Node origen, Callable<T> trabajo, Consumer<T> alExito) {
        return ejecutar(origen, trabajo, alExito, this::manejarError);
    }

    public <T> Task<T> ejecutar(Node origen, Callable<T> trabajo, Consumer<T> alExito, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        marcarOcupado(origen);
        tarea.setOnSucceeded(e -> {
            liberar(origen);
            alExito.accept(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            liberar(origen);
            alFallar.accept(tarea.getException());
        });
        ejecutor.execute(tarea);
        return tarea;
    }

    /** Tratamiento por defecto de errores; los callbacks propios pueden delegar aquí. */
    public void manejarError(Throwable error) {
        Throwable causa = causaConocida(error);
        if (causa instanceof NegocioException || causa instanceof PermisoDenegadoException) {
            notificador.advertencia(causa.getMessage());
        } else if (causa instanceof ConstraintViolationException cve) {
            notificador.advertencia(cve.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("\n")));
        } else if (causa instanceof IntegracionException) {
            notificador.info(causa.getMessage());
        } else {
            log.error("Error inesperado en tarea de fondo", error);
            notificador.error("Ocurrió un error inesperado. Revise el registro.");
        }
    }

    /** Busca en la cadena de causas una excepción con mensaje para el usuario. */
    private static Throwable causaConocida(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof NegocioException || t instanceof PermisoDenegadoException
                    || t instanceof IntegracionException || t instanceof ConstraintViolationException) {
                return t;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return error;
    }

    private static void marcarOcupado(Node origen) {
        if (origen == null) {
            return;
        }
        origen.setDisable(true);
        origen.setCursor(Cursor.WAIT);
        if (origen instanceof Labeled etiquetado) {
            ProgressIndicator progreso = new ProgressIndicator();
            progreso.setPrefSize(16, 16);
            progreso.setMaxSize(16, 16);
            origen.getProperties().put(GRAFICO_ORIGINAL, etiquetado.getGraphic());
            etiquetado.setGraphic(progreso);
        }
    }

    private static void liberar(Node origen) {
        if (origen == null) {
            return;
        }
        origen.setDisable(false);
        origen.setCursor(Cursor.DEFAULT);
        if (origen instanceof Labeled etiquetado && origen.getProperties().containsKey(GRAFICO_ORIGINAL)) {
            etiquetado.setGraphic((Node) origen.getProperties().remove(GRAFICO_ORIGINAL));
        }
    }
}
