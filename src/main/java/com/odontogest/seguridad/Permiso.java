/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seguridad;

import com.odontogest.modelo.Rol;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.odontogest.modelo.Rol.ADMIN;
import static com.odontogest.modelo.Rol.ODONTOLOGO;
import static com.odontogest.modelo.Rol.RECEPCION;

/**
 * Matriz de permisos por rol (PLAN §5.6). Los servicios la exigen en cada operación que escribe; la UI
 * además oculta o deshabilita los controles.
 */
public enum Permiso {
    PACIENTE_VER("ver pacientes", ADMIN, ODONTOLOGO, RECEPCION),
    PACIENTE_EDITAR("registrar o editar pacientes", ADMIN, ODONTOLOGO, RECEPCION),
    ANAMNESIS_EDITAR("editar la anamnesis", ADMIN, ODONTOLOGO),
    ATENCION_GESTIONAR("gestionar atenciones", ADMIN, ODONTOLOGO),
    ODONTOGRAMA_VER("ver odontogramas", ADMIN, ODONTOLOGO, RECEPCION),
    ODONTOGRAMA_EDITAR("editar odontogramas", ADMIN, ODONTOLOGO),
    PLAN_EDITAR("editar planes de tratamiento", ADMIN, ODONTOLOGO),
    PAGO_REGISTRAR("registrar pagos", ADMIN, RECEPCION),
    PAGO_ANULAR("anular pagos", ADMIN),
    AGENDA_GESTIONAR("gestionar la agenda", ADMIN, ODONTOLOGO, RECEPCION),
    REPORTES_VER("ver reportes", ADMIN, ODONTOLOGO, RECEPCION),
    CATALOGOS_EDITAR("editar catálogos", ADMIN),
    USUARIOS("administrar usuarios", ADMIN),
    CONFIGURACION("cambiar la configuración", ADMIN),
    RESPALDO("crear o restaurar respaldos", ADMIN);

    /** Texto para el mensaje "No tiene permiso para …". */
    public final String descripcion;
    private final Set<Rol> roles;

    Permiso(String descripcion, Rol... roles) {
        this.descripcion = descripcion;
        this.roles = EnumSet.copyOf(List.of(roles));
    }

    public boolean permite(Rol rol) {
        return rol != null && roles.contains(rol);
    }
}
