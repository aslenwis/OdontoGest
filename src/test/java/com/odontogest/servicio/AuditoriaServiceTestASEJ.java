package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.AuditoriaASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.persistencia.repositorio.AuditoriaRepositoryASEJ;
import com.odontogest.servicio.dto.AuditoriaDtoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditoriaServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private AuditoriaServiceASEJ auditoriaServiceASEJ;
    @Autowired private AuditoriaRepositoryASEJ auditoriaRepositoryASEJ;
    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private ConfiguracionServiceASEJ configuracionServiceASEJ;
    @Autowired private AutenticacionServiceASEJ autenticacionServiceASEJ;

    private PacienteFormASEJ pacienteASEJ(String dniASEJ) {
        return new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniASEJ, "Paciente", "Auditado" + sufijoASEJ(), null,
                LocalDate.now().minusYears(40), SexoASEJ.F, null, null, null, null, null, null, null, null, null, true);
    }

    @Test
    void registraLaAccionConUsuarioYDetalleASEJ() {
        UsuarioSesionASEJ recepcionASEJ = iniciarSesionASEJ("recepcion");
        long idASEJ = pacienteServiceASEJ.guardarASEJ(pacienteASEJ(dniAleatorioASEJ()));
        pacienteServiceASEJ.cambiarEstadoASEJ(idASEJ, false);

        List<AuditoriaASEJ> registrosASEJ = auditoriaRepositoryASEJ.findByEntidadASEJAndEntidadIdASEJOrderByIdASEJAsc("paciente", idASEJ);
        assertThat(registrosASEJ).extracting(AuditoriaASEJ::getAccionASEJ).containsExactly("PACIENTE_CREAR", "PACIENTE_DESACTIVAR");
        AuditoriaASEJ creacionASEJ = registrosASEJ.get(0);
        assertThat(creacionASEJ.getUsuarioIdASEJ()).isEqualTo(recepcionASEJ.idASEJ());
        assertThat(creacionASEJ.getDetalleASEJ()).startsWith("{\"historia\":\"HC-");
        assertThat(creacionASEJ.getFechaASEJ()).isNotNull();
        assertThat(creacionASEJ.getFechaASEJ().getNano()).isZero();
    }

    @Test
    void siLaOperacionFalla_noQuedaRegistroASEJ() {
        iniciarSesionASEJ("recepcion");
        String dniASEJ = dniAleatorioASEJ();
        pacienteServiceASEJ.guardarASEJ(pacienteASEJ(dniASEJ));
        long antesASEJ = auditoriaRepositoryASEJ.count();
        assertThatThrownBy(() -> pacienteServiceASEJ.guardarASEJ(pacienteASEJ(dniASEJ))).isInstanceOf(NegocioExceptionASEJ.class);
        assertThat(auditoriaRepositoryASEJ.count()).isEqualTo(antesASEJ);
    }

    @Test
    void registrar_exigeUnaTransaccionEnCursoASEJ() {
        assertThatThrownBy(() -> auditoriaServiceASEJ.registrarASEJ("PRUEBA", "prueba", 1L, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void configuracion_registraLasClavesPeroNuncaLosValoresASEJ() {
        iniciarSesionASEJ("admin");
        configuracionServiceASEJ.guardarASEJ(Map.of("email.smtp.password", "secreto-de-prueba-123"));
        AuditoriaASEJ ultimaASEJ = auditoriaRepositoryASEJ.buscarASEJ(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(),
                "CONFIGURACION_GUARDAR", org.springframework.data.domain.PageRequest.of(0, 1)).get(0);
        assertThat(ultimaASEJ.getDetalleASEJ()).contains("email.smtp.password").doesNotContain("secreto-de-prueba-123");
    }

    @Test
    void loginFallido_quedaRegistradoAunqueLanceExcepcionASEJ() {
        long antesASEJ = auditoriaRepositoryASEJ.count();
        assertThatThrownBy(() -> autenticacionServiceASEJ.iniciarSesionASEJ("recepcion", "ClaveIncorrecta1"))
                .isInstanceOf(NegocioExceptionASEJ.class);
        assertThat(auditoriaRepositoryASEJ.count()).isEqualTo(antesASEJ + 1);
        iniciarSesionASEJ("admin");
        assertThat(auditoriaServiceASEJ.buscarASEJ(LocalDate.now(), LocalDate.now(), "LOGIN_FALLIDO"))
                .first().extracting(AuditoriaDtoASEJ::usuarioASEJ, AuditoriaDtoASEJ::entidadASEJ)
                .containsExactly("—", "usuario");
        autenticacionServiceASEJ.iniciarSesionASEJ("recepcion", "Recep123*");
    }

    @Test
    void consulta_soloAdministradorASEJ() {
        iniciarSesionASEJ("admin");
        assertThat(auditoriaServiceASEJ.accionesASEJ()).contains("PACIENTE_CREAR");
        assertThat(auditoriaServiceASEJ.buscarASEJ(LocalDate.now(), LocalDate.now(), null)).isNotEmpty()
                .allMatch(aASEJ -> !aASEJ.fechaASEJ().toLocalDate().isBefore(LocalDate.now()));
        iniciarSesionASEJ("recepcion");
        assertThatThrownBy(() -> auditoriaServiceASEJ.buscarASEJ(LocalDate.now(), LocalDate.now(), null))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }
}
