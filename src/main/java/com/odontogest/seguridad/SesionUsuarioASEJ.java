package com.odontogest.seguridad;

import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SesionUsuarioASEJ {

    private volatile UsuarioSesionASEJ actualASEJ;

    public void iniciarASEJ(UsuarioSesionASEJ usuarioASEJ) {
        this.actualASEJ = usuarioASEJ;
    }

    public void cerrarASEJ() {
        this.actualASEJ = null;
    }

    public boolean estaAutenticadaASEJ() {
        return actualASEJ != null;
    }

    public Optional<UsuarioSesionASEJ> usuarioASEJ() {
        return Optional.ofNullable(actualASEJ);
    }

    public UsuarioSesionASEJ exigirUsuarioASEJ() {
        UsuarioSesionASEJ uASEJ = actualASEJ;
        if (uASEJ == null) {
            throw new PermisoDenegadoExceptionASEJ("No hay una sesión activa. Inicie sesión nuevamente.");
        }
        return uASEJ;
    }

    public boolean tieneASEJ(PermisoASEJ permisoASEJ) {
        UsuarioSesionASEJ uASEJ = actualASEJ;
        return uASEJ != null && permisoASEJ.permiteASEJ(RolASEJ.valueOf(uASEJ.rolASEJ()));
    }

    public UsuarioSesionASEJ exigirASEJ(PermisoASEJ permisoASEJ) {
        UsuarioSesionASEJ uASEJ = exigirUsuarioASEJ();
        if (!permisoASEJ.permiteASEJ(RolASEJ.valueOf(uASEJ.rolASEJ()))) {
            throw new PermisoDenegadoExceptionASEJ("No tiene permiso para " + permisoASEJ.descripcionASEJ + ".");
        }
        return uASEJ;
    }
}
