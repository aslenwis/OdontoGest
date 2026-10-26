package com.odontogest.seguridad;

import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermisoTestASEJ {

    @ParameterizedTest
    @CsvSource({
            "PACIENTE_EDITAR,    true,  true,  true",
            "ANAMNESIS_EDITAR,   true,  true,  false",
            "ODONTOGRAMA_VER,    true,  true,  true",
            "ODONTOGRAMA_EDITAR, true,  true,  false",
            "PLAN_EDITAR,        true,  true,  false",
            "PAGO_REGISTRAR,     true,  false, true",
            "PAGO_ANULAR,        true,  false, false",
            "AGENDA_GESTIONAR,   true,  true,  true",
            "CATALOGOS_EDITAR,   true,  false, false",
            "USUARIOS,           true,  false, false",
            "CONFIGURACION,      true,  false, false",
            "RESPALDO,           true,  false, false"
    })
    void matrizDePermisosASEJ(PermisoASEJ permisoASEJ, boolean adminASEJ, boolean odontologoASEJ, boolean recepcionASEJ) {
        assertThat(permisoASEJ.permiteASEJ(RolASEJ.ADMIN)).isEqualTo(adminASEJ);
        assertThat(permisoASEJ.permiteASEJ(RolASEJ.ODONTOLOGO)).isEqualTo(odontologoASEJ);
        assertThat(permisoASEJ.permiteASEJ(RolASEJ.RECEPCION)).isEqualTo(recepcionASEJ);
        assertThat(permisoASEJ.permiteASEJ(null)).isFalse();
    }

    @Test
    void sesionExigePermisoASEJ() {
        SesionUsuarioASEJ sesionASEJ = new SesionUsuarioASEJ();
        assertThat(sesionASEJ.tieneASEJ(PermisoASEJ.PACIENTE_VER)).isFalse();
        assertThatThrownBy(() -> sesionASEJ.exigirASEJ(PermisoASEJ.PACIENTE_VER)).isInstanceOf(PermisoDenegadoExceptionASEJ.class)
                .hasMessageContaining("sesión");

        sesionASEJ.iniciarASEJ(new UsuarioSesionASEJ(1, "recepcion", "Recepción", "RECEPCION", null));
        assertThat(sesionASEJ.tieneASEJ(PermisoASEJ.PAGO_REGISTRAR)).isTrue();
        assertThat(sesionASEJ.exigirASEJ(PermisoASEJ.PACIENTE_EDITAR).usernameASEJ()).isEqualTo("recepcion");
        assertThatThrownBy(() -> sesionASEJ.exigirASEJ(PermisoASEJ.PAGO_ANULAR)).isInstanceOf(PermisoDenegadoExceptionASEJ.class)
                .hasMessage("No tiene permiso para anular pagos.");
    }
}
