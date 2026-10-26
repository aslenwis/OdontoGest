package com.odontogest;

import com.odontogest.config.DirectorioDatosASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest(properties = "app.seeder.datos-demo=false")
public abstract class PruebaIntegracionASEJ {

    protected static final Path HOME_PRUEBASASEJ;

    static {
        try {
            Path temporalASEJ = Files.createTempDirectory("odontogest-test-");
            System.setProperty(DirectorioDatosASEJ.PROPIEDADASEJ, temporalASEJ.toString());
            HOME_PRUEBASASEJ = DirectorioDatosASEJ.inicializarASEJ();
        } catch (IOException eASEJ) {
            throw new UncheckedIOException(eASEJ);
        }
    }

    @Autowired private SesionUsuarioASEJ sesionDePruebaASEJ;
    @Autowired private UsuarioRepositoryASEJ usuariosDePruebaASEJ;

    protected UsuarioSesionASEJ iniciarSesionASEJ(String usernameASEJ) {
        UsuarioASEJ uASEJ = usuariosDePruebaASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).orElseThrow();
        UsuarioSesionASEJ sASEJ = new UsuarioSesionASEJ(uASEJ.getIdASEJ(), uASEJ.getUsernameASEJ(), uASEJ.getNombreCompletoASEJ(), uASEJ.getRolASEJ().name(),
                uASEJ.getOdontologoASEJ() == null ? null : uASEJ.getOdontologoASEJ().getIdASEJ());
        sesionDePruebaASEJ.iniciarASEJ(sASEJ);
        return sASEJ;
    }

    @AfterEach
    void cerrarSesionDePruebaASEJ() {
        sesionDePruebaASEJ.cerrarASEJ();
    }

    protected static String dniAleatorioASEJ() {
        return "7" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    protected static String sufijoASEJ() {
        StringBuilder sbASEJ = new StringBuilder();
        for (int iASEJ = 0; iASEJ < 8; iASEJ++) {
            sbASEJ.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return sbASEJ.toString();
    }
}
