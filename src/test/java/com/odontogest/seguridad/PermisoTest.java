/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seguridad;

import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.Rol;
import com.odontogest.servicio.dto.UsuarioSesion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Matriz de permisos por rol (PLAN §5.6).
 */
class PermisoTest {

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
    void matrizDePermisos(Permiso permiso, boolean admin, boolean odontologo, boolean recepcion) {
        assertThat(permiso.permite(Rol.ADMIN)).isEqualTo(admin);
        assertThat(permiso.permite(Rol.ODONTOLOGO)).isEqualTo(odontologo);
        assertThat(permiso.permite(Rol.RECEPCION)).isEqualTo(recepcion);
        assertThat(permiso.permite(null)).isFalse();
    }

    @Test
    void sesionExigePermiso() {
        SesionUsuario sesion = new SesionUsuario();
        assertThat(sesion.tiene(Permiso.PACIENTE_VER)).isFalse();
        assertThatThrownBy(() -> sesion.exigir(Permiso.PACIENTE_VER)).isInstanceOf(PermisoDenegadoException.class)
                .hasMessageContaining("sesión");

        sesion.iniciar(new UsuarioSesion(1, "recepcion", "Recepción", "RECEPCION", null));
        assertThat(sesion.tiene(Permiso.PAGO_REGISTRAR)).isTrue();
        assertThat(sesion.exigir(Permiso.PACIENTE_EDITAR).username()).isEqualTo("recepcion");
        assertThatThrownBy(() -> sesion.exigir(Permiso.PAGO_ANULAR)).isInstanceOf(PermisoDenegadoException.class)
                .hasMessage("No tiene permiso para anular pagos.");
    }
}
