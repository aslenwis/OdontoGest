package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutenticacionServiceTestASEJ extends PruebaIntegracionASEJ {

    private static final String CLAVEASEJ = "Clave123*";

    @Autowired private AutenticacionServiceASEJ autenticacionServiceASEJ;
    @Autowired private UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    @Autowired private PasswordEncoder passwordEncoderASEJ;
    @Autowired private SesionUsuarioASEJ sesionUsuarioASEJ;

    @Test
    void adminDemoIniciaSesionASEJ() {
        UsuarioSesionASEJ sesionASEJ = autenticacionServiceASEJ.iniciarSesionASEJ("admin", "Admin123*");

        assertThat(sesionASEJ.rolASEJ()).isEqualTo("ADMIN");
        assertThat(sesionUsuarioASEJ.usuarioASEJ()).contains(sesionASEJ);
        assertThat(usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase("admin").orElseThrow().getUltimoAccesoASEJ()).isNotNull();
    }

    @Test
    void usuarioNoDistingueMayusculasASEJ() {
        String usernameASEJ = crearUsuarioASEJ();
        assertThat(autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ.toUpperCase(), CLAVEASEJ).usernameASEJ()).isEqualTo(usernameASEJ);
    }

    @Test
    void credencialesInvalidasNoRevelanSiExisteElUsuarioASEJ() {
        String usernameASEJ = crearUsuarioASEJ();
        assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, "otra"))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessage("Usuario o contraseña incorrectos.");
        assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ("no.existe", "otra"))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessage("Usuario o contraseña incorrectos.");
        assertThat(usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).orElseThrow().getIntentosFallidosASEJ()).isEqualTo(1);
        assertThat(sesionUsuarioASEJ.estaAutenticadaASEJ()).isFalse();
    }

    @Test
    void bloqueaTrasCincoIntentosFallidosYDesbloqueaAlVencerASEJ() {
        String usernameASEJ = crearUsuarioASEJ();
        for (int iASEJ = 1; iASEJ < AutenticacionServiceASEJ.MAX_INTENTOSASEJ; iASEJ++) {
            assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, "mala"))
                    .hasMessage("Usuario o contraseña incorrectos.");
        }
        assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, "mala"))
                .hasMessageContaining("bloqueado por 5 minutos");

        assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, CLAVEASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("Usuario bloqueado");

        UsuarioASEJ uASEJ = usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).orElseThrow();
        assertThat(uASEJ.getBloqueadoHastaASEJ()).isAfter(LocalDateTime.now().plusMinutes(4));
        uASEJ.setBloqueadoHastaASEJ(LocalDateTime.now().minusSeconds(1));
        usuarioRepositoryASEJ.save(uASEJ);

        autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, CLAVEASEJ);
        UsuarioASEJ desbloqueadoASEJ = usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).orElseThrow();
        assertThat(desbloqueadoASEJ.getBloqueadoHastaASEJ()).isNull();
        assertThat(desbloqueadoASEJ.getIntentosFallidosASEJ()).isZero();
    }

    @Test
    void usuarioInactivoNoIngresaASEJ() {
        String usernameASEJ = crearUsuarioASEJ();
        UsuarioASEJ uASEJ = usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).orElseThrow();
        uASEJ.setActivoASEJ(false);
        usuarioRepositoryASEJ.save(uASEJ);

        assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, CLAVEASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("desactivado");
    }

    @Test
    void cerrarSesionLimpiaElUsuarioActualASEJ() {
        autenticacionServiceASEJ.iniciarSesionASEJ("admin", "Admin123*");
        autenticacionServiceASEJ.cerrarSesionASEJ();
        assertThat(sesionUsuarioASEJ.estaAutenticadaASEJ()).isFalse();
    }

    private String crearUsuarioASEJ() {
        String usernameASEJ = "u" + UUID.randomUUID().toString().substring(0, 8);
        UsuarioASEJ uASEJ = new UsuarioASEJ();
        uASEJ.setUsernameASEJ(usernameASEJ);
        uASEJ.setPasswordHashASEJ(passwordEncoderASEJ.encode(CLAVEASEJ));
        uASEJ.setNombreCompletoASEJ("Usuario de prueba");
        uASEJ.setRolASEJ(RolASEJ.RECEPCION);
        uASEJ.setCreadoEnASEJ(LocalDateTime.now());
        usuarioRepositoryASEJ.save(uASEJ);
        return usernameASEJ;
    }
}
