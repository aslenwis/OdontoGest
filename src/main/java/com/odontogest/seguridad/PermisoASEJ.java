package com.odontogest.seguridad;

import com.odontogest.modelo.RolASEJ;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.odontogest.modelo.RolASEJ.ADMIN;
import static com.odontogest.modelo.RolASEJ.ODONTOLOGO;
import static com.odontogest.modelo.RolASEJ.RECEPCION;

public enum PermisoASEJ {
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

    public final String descripcionASEJ;
    private final Set<RolASEJ> rolesASEJ;

    PermisoASEJ(String descripcionASEJ, RolASEJ... rolesASEJ) {
        this.descripcionASEJ = descripcionASEJ;
        this.rolesASEJ = EnumSet.copyOf(List.of(rolesASEJ));
    }

    public boolean permiteASEJ(RolASEJ rolASEJ) {
        return rolASEJ != null && rolesASEJ.contains(rolASEJ);
    }
}
