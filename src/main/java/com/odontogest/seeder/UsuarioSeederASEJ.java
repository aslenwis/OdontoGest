package com.odontogest.seeder;

import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class UsuarioSeederASEJ {

    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final PasswordEncoder passwordEncoderASEJ;
    private final Clock relojASEJ;

    public void sembrarASEJ(OdontologoASEJ odontologoGeneralASEJ) {
        crearASEJ("admin", "Admin123*", "Administrador del sistema", RolASEJ.ADMIN, null);
        crearASEJ("odontologo", "Odonto123*", odontologoGeneralASEJ.getNombreCompletoASEJ(), RolASEJ.ODONTOLOGO, odontologoGeneralASEJ);
        crearASEJ("recepcion", "Recep123*", "Recepción", RolASEJ.RECEPCION, null);
    }

    private void crearASEJ(String usernameASEJ, String passwordASEJ, String nombreCompletoASEJ, RolASEJ rolASEJ, OdontologoASEJ odontologoASEJ) {
        UsuarioASEJ uASEJ = new UsuarioASEJ();
        uASEJ.setUsernameASEJ(usernameASEJ);
        uASEJ.setPasswordHashASEJ(passwordEncoderASEJ.encode(passwordASEJ));
        uASEJ.setNombreCompletoASEJ(nombreCompletoASEJ);
        uASEJ.setRolASEJ(rolASEJ);
        uASEJ.setOdontologoASEJ(odontologoASEJ);
        uASEJ.setActivoASEJ(true);
        uASEJ.setCreadoEnASEJ(LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS));
        usuarioRepositoryASEJ.save(uASEJ);
    }
}
