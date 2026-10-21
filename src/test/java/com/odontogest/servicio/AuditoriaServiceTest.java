/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.Auditoria;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.persistencia.repositorio.AuditoriaRepository;
import com.odontogest.servicio.dto.AuditoriaDto;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.UsuarioSesion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Auditoría (PLAN §5.5): los servicios registran sus acciones en la misma transacción, con el usuario de la sesión
 * y un detalle JSON sin secretos; si la operación falla no queda registro.
 */
class AuditoriaServiceTest extends PruebaIntegracion {

    @Autowired private AuditoriaService auditoriaService;
    @Autowired private AuditoriaRepository auditoriaRepository;
    @Autowired private PacienteService pacienteService;
    @Autowired private ConfiguracionService configuracionService;
    @Autowired private AutenticacionService autenticacionService;

    private PacienteForm paciente(String dni) {
        return new PacienteForm(null, TipoDocumento.DNI, dni, "Paciente", "Auditado" + sufijo(), null,
                LocalDate.now().minusYears(40), Sexo.F, null, null, null, null, null, null, null, null, null, true);
    }

    @Test
    void registraLaAccionConUsuarioYDetalle() {
        UsuarioSesion recepcion = iniciarSesion("recepcion");
        long id = pacienteService.guardar(paciente(dniAleatorio()));
        pacienteService.cambiarEstado(id, false);

        List<Auditoria> registros = auditoriaRepository.findByEntidadAndEntidadIdOrderByIdAsc("paciente", id);
        assertThat(registros).extracting(Auditoria::getAccion).containsExactly("PACIENTE_CREAR", "PACIENTE_DESACTIVAR");
        Auditoria creacion = registros.get(0);
        assertThat(creacion.getUsuarioId()).isEqualTo(recepcion.id());
        assertThat(creacion.getDetalle()).startsWith("{\"historia\":\"HC-");
        assertThat(creacion.getFecha()).isNotNull();
        assertThat(creacion.getFecha().getNano()).isZero();
    }

    @Test
    void siLaOperacionFalla_noQuedaRegistro() {
        iniciarSesion("recepcion");
        String dni = dniAleatorio();
        pacienteService.guardar(paciente(dni));
        long antes = auditoriaRepository.count();
        assertThatThrownBy(() -> pacienteService.guardar(paciente(dni))).isInstanceOf(NegocioException.class);
        assertThat(auditoriaRepository.count()).isEqualTo(antes);
    }

    @Test
    void registrar_exigeUnaTransaccionEnCurso() {
        assertThatThrownBy(() -> auditoriaService.registrar("PRUEBA", "prueba", 1L, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void configuracion_registraLasClavesPeroNuncaLosValores() {
        iniciarSesion("admin");
        configuracionService.guardar(Map.of("email.smtp.password", "secreto-de-prueba-123"));
        Auditoria ultima = auditoriaRepository.buscar(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(),
                "CONFIGURACION_GUARDAR", org.springframework.data.domain.PageRequest.of(0, 1)).get(0);
        assertThat(ultima.getDetalle()).contains("email.smtp.password").doesNotContain("secreto-de-prueba-123");
    }

    @Test
    void loginFallido_quedaRegistradoAunqueLanceExcepcion() {
        long antes = auditoriaRepository.count();
        assertThatThrownBy(() -> autenticacionService.iniciarSesion("recepcion", "ClaveIncorrecta1"))
                .isInstanceOf(NegocioException.class);
        assertThat(auditoriaRepository.count()).isEqualTo(antes + 1);
        iniciarSesion("admin");
        assertThat(auditoriaService.buscar(LocalDate.now(), LocalDate.now(), "LOGIN_FALLIDO"))
                .first().extracting(AuditoriaDto::usuario, AuditoriaDto::entidad)
                .containsExactly("—", "usuario");
        // Deja el contador en cero para no bloquear al usuario demo en otras pruebas.
        autenticacionService.iniciarSesion("recepcion", "Recep123*");
    }

    @Test
    void consulta_soloAdministrador() {
        iniciarSesion("admin");
        assertThat(auditoriaService.acciones()).contains("PACIENTE_CREAR");
        assertThat(auditoriaService.buscar(LocalDate.now(), LocalDate.now(), null)).isNotEmpty()
                .allMatch(a -> !a.fecha().toLocalDate().isBefore(LocalDate.now()));
        iniciarSesion("recepcion");
        assertThatThrownBy(() -> auditoriaService.buscar(LocalDate.now(), LocalDate.now(), null))
                .isInstanceOf(PermisoDenegadoException.class);
    }
}
