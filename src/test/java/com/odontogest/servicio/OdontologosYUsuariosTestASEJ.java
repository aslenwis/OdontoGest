package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.servicio.dto.HorarioDtoASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.OdontologoFormASEJ;
import com.odontogest.servicio.dto.UsuarioDtoASEJ;
import com.odontogest.servicio.dto.UsuarioFormASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OdontologosYUsuariosTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private OdontologoServiceASEJ odontologoServiceASEJ;
    @Autowired private UsuarioServiceASEJ usuarioServiceASEJ;
    @Autowired private AutenticacionServiceASEJ autenticacionServiceASEJ;
    @Autowired private UsuarioRepositoryASEJ usuarioRepositoryASEJ;

    private static final HorarioDtoASEJ LUNES_MANANAASEJ = new HorarioDtoASEJ(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

    @BeforeEach
    void sesionASEJ() {
        iniciarSesionASEJ("admin");
    }

    private static String copASEJ() {
        return String.valueOf(100_000 + (int) (Math.random() * 800_000));
    }

    private OdontologoFormASEJ odontologoASEJ(String copASEJ, List<HorarioDtoASEJ> horariosASEJ) {
        return new OdontologoFormASEJ(null, "Ana", "Prueba " + sufijoASEJ(), copASEJ, null, null, "987654321", "ana@clinica.pe",
                "#16a34a", true, horariosASEJ);
    }

    @Test
    void elSeederCreaTresOdontologosConHorarioASEJ() {
        List<OdontologoDtoASEJ> demoASEJ = odontologoServiceASEJ.listarASEJ(true).stream().filter(oASEJ -> oASEJ.copASEJ().startsWith("9010")).toList();
        assertThat(demoASEJ).hasSize(3);
        assertThat(demoASEJ).allSatisfy(oASEJ -> assertThat(oASEJ.horariosASEJ()).hasSize(11));
    }

    @Test
    void guardaOdontologoYReemplazaSuHorarioASEJ() {
        long idASEJ = odontologoServiceASEJ.guardarASEJ(odontologoASEJ(copASEJ(), List.of(LUNES_MANANAASEJ)));
        OdontologoDtoASEJ oASEJ = odontologoServiceASEJ.obtenerASEJ(idASEJ);
        assertThat(oASEJ.colorAgendaASEJ()).isEqualTo("#16A34A");
        assertThat(oASEJ.especialidadASEJ()).isEqualTo("Odontología general");
        assertThat(oASEJ.horariosASEJ()).containsExactly(LUNES_MANANAASEJ);

        HorarioDtoASEJ sabadoASEJ = new HorarioDtoASEJ(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        odontologoServiceASEJ.guardarASEJ(new OdontologoFormASEJ(idASEJ, oASEJ.nombresASEJ(), oASEJ.apellidosASEJ(), oASEJ.copASEJ(), "R-1", "Periodoncia", null, null,
                oASEJ.colorAgendaASEJ(), true, List.of(sabadoASEJ)));
        assertThat(odontologoServiceASEJ.obtenerASEJ(idASEJ).horariosASEJ()).containsExactly(sabadoASEJ);
    }

    @Test
    void validaHorarioColorYCopASEJ() {
        HorarioDtoASEJ solapadoASEJ = new HorarioDtoASEJ(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0));
        HorarioDtoASEJ invertidoASEJ = new HorarioDtoASEJ(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(10, 0));
        OdontologoFormASEJ fASEJ = new OdontologoFormASEJ(null, "", "Apellido", "COP-1", null, null, null, "mal", "rojo", true,
                List.of(LUNES_MANANAASEJ, solapadoASEJ, invertidoASEJ));
        assertThatThrownBy(() -> odontologoServiceASEJ.guardarASEJ(fASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class)
                .hasMessageContaining("nombres").hasMessageContaining("COP").hasMessageContaining("#RRGGBB")
                .hasMessageContaining("correo").hasMessageContaining("se superponen").hasMessageContaining("posterior");

        String copASEJ = copASEJ();
        odontologoServiceASEJ.guardarASEJ(odontologoASEJ(copASEJ, List.of()));
        assertThatThrownBy(() -> odontologoServiceASEJ.guardarASEJ(odontologoASEJ(copASEJ, List.of())))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ya está registrado");
    }

    @Test
    void elSeederCreaLosTresUsuariosDemoASEJ() {
        List<UsuarioDtoASEJ> usuariosASEJ = usuarioServiceASEJ.listarASEJ();
        assertThat(usuariosASEJ).extracting(UsuarioDtoASEJ::usernameASEJ).contains("admin", "odontologo", "recepcion");
        UsuarioDtoASEJ odontologoASEJ = usuariosASEJ.stream().filter(uASEJ -> uASEJ.usernameASEJ().equals("odontologo")).findFirst().orElseThrow();
        assertThat(odontologoASEJ.rolASEJ()).isEqualTo(RolASEJ.ODONTOLOGO);
        assertThat(odontologoASEJ.odontologoNombreASEJ()).isEqualTo("Carlos Alberto Quispe Mamani");
        assertThat(autenticacionServiceASEJ.iniciarSesionASEJ("recepcion", "Recep123*").rolASEJ()).isEqualTo("RECEPCION");
        assertThat(autenticacionServiceASEJ.iniciarSesionASEJ("odontologo", "Odonto123*").odontologoIdASEJ()).isNotNull();
    }

    @Test
    void creaUsuariosYValidaRolOdontologoASEJ() {
        String usernameASEJ = "u." + sufijoASEJ();
        assertThatThrownBy(() -> usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, usernameASEJ, "Dra. Prueba", RolASEJ.ODONTOLOGO,
                null, true, "Clave1234")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("vinculado a un odontólogo");
        assertThatThrownBy(() -> usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, "a b", "", null, null, true, "corta")))
                .isInstanceOf(NegocioExceptionASEJ.class)
                .hasMessageContaining("nombre completo").hasMessageContaining("rol").hasMessageContaining("usuario admite")
                .hasMessageContaining("8 caracteres");

        long odontologoIdASEJ = odontologoServiceASEJ.guardarASEJ(odontologoASEJ(copASEJ(), List.of()));
        long idASEJ = usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, usernameASEJ, "Dra. Prueba", RolASEJ.ODONTOLOGO, odontologoIdASEJ, true, "Clave1234"));
        UsuarioDtoASEJ uASEJ = usuarioServiceASEJ.listarASEJ().stream().filter(xASEJ -> xASEJ.idASEJ() == idASEJ).findFirst().orElseThrow();
        assertThat(uASEJ.odontologoIdASEJ()).isEqualTo(odontologoIdASEJ);

        assertThatThrownBy(() -> usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, usernameASEJ.toUpperCase(), "Otro", RolASEJ.RECEPCION,
                null, true, "Clave1234")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ya existe");

        usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(idASEJ, null, "Dra. Prueba", RolASEJ.RECEPCION, odontologoIdASEJ, true, null));
        assertThat(usuarioServiceASEJ.listarASEJ().stream().filter(xASEJ -> xASEJ.idASEJ() == idASEJ).findFirst().orElseThrow().odontologoIdASEJ())
                .as("el vínculo solo aplica al rol Odontólogo").isNull();
    }

    @Test
    void protegeAlUltimoAdministradorYAlPropioUsuarioASEJ() {
        UsuarioSesionASEJ adminASEJ = iniciarSesionASEJ("admin");
        assertThatThrownBy(() -> usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(adminASEJ.idASEJ(), null, adminASEJ.nombreCompletoASEJ(), RolASEJ.RECEPCION,
                null, true, null)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("al menos un administrador");

        long otroAdminASEJ = usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, "adm." + sufijoASEJ(), "Otro admin", RolASEJ.ADMIN, null, true, "Clave1234"));
        assertThatThrownBy(() -> usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(adminASEJ.idASEJ(), null, adminASEJ.nombreCompletoASEJ(), RolASEJ.ADMIN,
                null, false, null)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("propio usuario");
        usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(otroAdminASEJ, null, "Otro admin", RolASEJ.ADMIN, null, false, null));
    }

    @Test
    void restablecerDesbloqueaYCambiarPasswordExigeLaActualASEJ() {
        String usernameASEJ = "r." + sufijoASEJ();
        long idASEJ = usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, usernameASEJ, "Recepción 2", RolASEJ.RECEPCION, null, true, "Clave1234"));
        var uASEJ = usuarioRepositoryASEJ.findById(idASEJ).orElseThrow();
        uASEJ.setBloqueadoHastaASEJ(LocalDateTime.now().plusMinutes(5));
        uASEJ.setIntentosFallidosASEJ(3);
        usuarioRepositoryASEJ.save(uASEJ);
        assertThat(usuarioServiceASEJ.listarASEJ().stream().filter(xASEJ -> xASEJ.idASEJ() == idASEJ).findFirst().orElseThrow().bloqueadoASEJ()).isTrue();

        usuarioServiceASEJ.restablecerPasswordASEJ(idASEJ, "Nueva2026");
        assertThat(autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, "Nueva2026").usernameASEJ()).isEqualTo(usernameASEJ);

        assertThatThrownBy(() -> usuarioServiceASEJ.cambiarPasswordASEJ("mala", "Otra2026x", "Otra2026x"))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("actual no es correcta");
        assertThatThrownBy(() -> usuarioServiceASEJ.cambiarPasswordASEJ("Nueva2026", "Otra2026x", "distinta1"))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("no coincide");
        usuarioServiceASEJ.cambiarPasswordASEJ("Nueva2026", "Otra2026x", "Otra2026x");
        assertThat(autenticacionServiceASEJ.iniciarSesionASEJ(usernameASEJ, "Otra2026x").usernameASEJ()).isEqualTo(usernameASEJ);
    }

    @Test
    void soloElAdministradorGestionaUsuariosYOdontologosASEJ() {
        iniciarSesionASEJ("recepcion");
        assertThatThrownBy(() -> usuarioServiceASEJ.guardarASEJ(new UsuarioFormASEJ(null, "x" + sufijoASEJ(), "X", RolASEJ.RECEPCION, null, true, "Clave1234")))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class).hasMessageContaining("administrar usuarios");
        assertThatThrownBy(() -> odontologoServiceASEJ.guardarASEJ(odontologoASEJ(copASEJ(), List.of())))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> usuarioServiceASEJ.restablecerPasswordASEJ(1, "Clave1234"))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }
}
