/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;

/**
 * Guarda documentos generados (PDF, Excel) donde elige el usuario y los abre con la aplicación del sistema.
 * La generación y la escritura del archivo corren en segundo plano con {@link TareaUi}.
 */
@Component
@RequiredArgsConstructor
public class Exportador {

    private final TareaUi tareaUi;
    private final Notificador notificador;
    private HostServices hostServices;
    private File ultimaCarpeta;

    /** Lo fija {@code JavaFxApplication.start()}. */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    /**
     * Pide el destino con un diálogo, genera el PDF en segundo plano, lo guarda y lo abre.
     *
     * @param origen control que se deshabilita mientras se genera
     */
    public void guardarPdf(Node origen, String nombreSugerido, Callable<byte[]> generar) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        if (ultimaCarpeta != null && ultimaCarpeta.isDirectory()) {
            selector.setInitialDirectory(ultimaCarpeta);
        }
        File destino = selector.showSaveDialog(origen.getScene().getWindow());
        if (destino == null) {
            return;
        }
        File archivo = destino.getName().toLowerCase().endsWith(".pdf") ? destino
                : new File(destino.getParentFile(), destino.getName() + ".pdf");
        ultimaCarpeta = archivo.getParentFile();
        tareaUi.ejecutar(origen, () -> {
            Files.write(archivo.toPath(), generar.call());
            return archivo;
        }, guardado -> {
            notificador.exito("Documento guardado: " + guardado.getName());
            abrir(guardado);
        });
    }

    public void abrir(File archivo) {
        if (hostServices != null) {
            hostServices.showDocument(archivo.toURI().toString());
        }
    }
}
