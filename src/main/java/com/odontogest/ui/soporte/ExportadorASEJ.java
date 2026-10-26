package com.odontogest.ui.soporte;

import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@Component
@RequiredArgsConstructor
public class ExportadorASEJ {

    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private HostServices hostServicesASEJ;
    private File ultimaCarpetaASEJ;

    public void setHostServicesASEJ(HostServices hostServicesASEJ) {
        this.hostServicesASEJ = hostServicesASEJ;
    }

    public void guardarPdfASEJ(Node origenASEJ, String nombreSugeridoASEJ, Callable<byte[]> generarASEJ) {
        guardarASEJ(origenASEJ, nombreSugeridoASEJ, "Guardar PDF", "Documento PDF (*.pdf)", ".pdf", generarASEJ);
    }

    public void guardarExcelASEJ(Node origenASEJ, String nombreSugeridoASEJ, Callable<byte[]> generarASEJ) {
        guardarASEJ(origenASEJ, nombreSugeridoASEJ, "Guardar Excel", "Libro de Excel (*.xlsx)", ".xlsx", generarASEJ);
    }

    private void guardarASEJ(Node origenASEJ, String nombreSugeridoASEJ, String tituloASEJ, String filtroASEJ, String extensionASEJ,
                         Callable<byte[]> generarASEJ) {
        FileChooser selectorASEJ = new FileChooser();
        selectorASEJ.setTitle(tituloASEJ);
        selectorASEJ.setInitialFileName(nombreSugeridoASEJ);
        selectorASEJ.getExtensionFilters().add(new FileChooser.ExtensionFilter(filtroASEJ, "*" + extensionASEJ));
        if (ultimaCarpetaASEJ != null && ultimaCarpetaASEJ.isDirectory()) {
            selectorASEJ.setInitialDirectory(ultimaCarpetaASEJ);
        }
        File destinoASEJ = selectorASEJ.showSaveDialog(origenASEJ.getScene().getWindow());
        if (destinoASEJ == null) {
            return;
        }
        File archivoASEJ = destinoASEJ.getName().toLowerCase().endsWith(extensionASEJ) ? destinoASEJ
                : new File(destinoASEJ.getParentFile(), destinoASEJ.getName() + extensionASEJ);
        ultimaCarpetaASEJ = archivoASEJ.getParentFile();
        tareaUiASEJ.ejecutarASEJ(origenASEJ, () -> {
            Files.write(archivoASEJ.toPath(), generarASEJ.call());
            return archivoASEJ;
        }, guardadoASEJ -> {
            notificadorASEJ.exitoASEJ("Documento guardado: " + guardadoASEJ.getName());
            abrirASEJ(guardadoASEJ);
        });
    }

    public void abrirASEJ(File archivoASEJ) {
        if (hostServicesASEJ != null) {
            hostServicesASEJ.showDocument(archivoASEJ.toURI().toString());
        }
    }
}
