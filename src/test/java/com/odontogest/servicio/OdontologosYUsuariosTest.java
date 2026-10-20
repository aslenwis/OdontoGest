/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.Rol;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.servicio.dto.HorarioDto;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.OdontologoForm;
import com.odontogest.servicio.dto.UsuarioDto;
import com.odontogest.servicio.dto.UsuarioForm;
import com.odontogest.servicio.dto.UsuarioSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Odontólogos con horario (RF-CAT-04), administración de usuarios (RF-SEG-03) y cambio de contraseña (RF-SEG-04).
 */
class OdontologosYUsuariosTest extends PruebaIntegracion {

    @Autowired private OdontologoService odontologoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private AutenticacionService autenticacionService;
    @Autowired private UsuarioRepository usuarioRepository;

    private static final HorarioDto LUNES_MANANA = new HorarioDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

    @BeforeEach
    void sesion() {
        iniciarSesion("admin");
    }

    private static String cop() {
        return String.valueOf(100_000 + (int) (Math.random() * 800_000));
    }

    private OdontologoForm odontologo(String cop, List<HorarioDto> horarios) {
        return new OdontologoForm(null, "Ana", "Prueba " + sufijo(), cop, null, null, "987654321", "ana@clinica.pe",
                "#16a34a", true, horarios);
    }

    // ------------------------------------------------------------------ odontólogos

    @Test
    void elSeederCreaTresOdontologosConHorario() {
        List<OdontologoDto> demo = odontologoService.listar(true).stream().filter(o -> o.cop().startsWith("9010")).toList();
        assertThat(demo).hasSize(3);
        assertThat(demo).allSatisfy(o -> assertThat(o.horarios()).hasSize(11));
    }

    @Test
    void guardaOdontologoYReemplazaSuHorario() {
        long id = odontologoService.guardar(odontologo(cop(), List.of(LUNES_MANANA)));
        OdontologoDto o = odontologoService.obtener(id);
        assertThat(o.colorAgenda()).isEqualTo("#16A34A");
        assertThat(o.especialidad()).isEqualTo("Odontología general");
        assertThat(o.horarios()).containsExactly(LUNES_MANANA);

        HorarioDto sabado = new HorarioDto(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        odontologoService.guardar(new OdontologoForm(id, o.nombres(), o.apellidos(), o.cop(), "R-1", "Periodoncia", null, null,
                o.colorAgenda(), true, List.of(sabado)));
        assertThat(odontologoService.obtener(id).horarios()).containsExactly(sabado);
    }

    @Test
    void validaHorarioColorYCop() {
        HorarioDto solapado = new HorarioDto(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0));
        HorarioDto invertido = new HorarioDto(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(10, 0));
        OdontologoForm f = new OdontologoForm(null, "", "Apellido", "COP-1", null, null, null, "mal", "rojo", true,
                List.of(LUNES_MANANA, solapado, invertido));
        assertThatThrownBy(() -> odontologoService.guardar(f))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("nombres").hasMessageContaining("COP").hasMessageContaining("#RRGGBB")
                .hasMessageContaining("correo").hasMessageContaining("se superponen").hasMessageContaining("posterior");

        String cop = cop();
        odontologoService.guardar(odontologo(cop, List.of()));
        assertThatThrownBy(() -> odontologoService.guardar(odontologo(cop, List.of())))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya está registrado");
    }

    // ------------------------------------------------------------------ usuarios

    @Test
    void elSeederCreaLosTresUsuariosDemo() {
        List<UsuarioDto> usuarios = usuarioService.listar();
        assertThat(usuarios).extracting(UsuarioDto::username).contains("admin", "odontologo", "recepcion");
        UsuarioDto odontologo = usuarios.stream().filter(u -> u.username().equals("odontologo")).findFirst().orElseThrow();
        assertThat(odontologo.rol()).isEqualTo(Rol.ODONTOLOGO);
        assertThat(odontologo.odontologoNombre()).isEqualTo("Carlos Alberto Quispe Mamani");
        assertThat(autenticacionService.iniciarSesion("recepcion", "Recep123*").rol()).isEqualTo("RECEPCION");
        assertThat(autenticacionService.iniciarSesion("odontologo", "Odonto123*").odontologoId()).isNotNull();
    }

    @Test
    void creaUsuariosYValidaRolOdontologo() {
        String username = "u." + sufijo();
        assertThatThrownBy(() -> usuarioService.guardar(new UsuarioForm(null, username, "Dra. Prueba", Rol.ODONTOLOGO,
                null, true, "Clave1234")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("vinculado a un odontólogo");
        assertThatThrownBy(() -> usuarioService.guardar(new UsuarioForm(null, "a b", "", null, null, true, "corta")))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("nombre completo").hasMessageContaining("rol").hasMessageContaining("usuario admite")
                .hasMessageContaining("8 caracteres");

        long odontologoId = odontologoService.guardar(odontologo(cop(), List.of()));
        long id = usuarioService.guardar(new UsuarioForm(null, username, "Dra. Prueba", Rol.ODONTOLOGO, odontologoId, true, "Clave1234"));
        UsuarioDto u = usuarioService.listar().stream().filter(x -> x.id() == id).findFirst().orElseThrow();
        assertThat(u.odontologoId()).isEqualTo(odontologoId);

        assertThatThrownBy(() -> usuarioService.guardar(new UsuarioForm(null, username.toUpperCase(), "Otro", Rol.RECEPCION,
                null, true, "Clave1234")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya existe");

        usuarioService.guardar(new UsuarioForm(id, null, "Dra. Prueba", Rol.RECEPCION, odontologoId, true, null));
        assertThat(usuarioService.listar().stream().filter(x -> x.id() == id).findFirst().orElseThrow().odontologoId())
                .as("el vínculo solo aplica al rol Odontólogo").isNull();
    }

    @Test
    void protegeAlUltimoAdministradorYAlPropioUsuario() {
        UsuarioSesion admin = iniciarSesion("admin");
        assertThatThrownBy(() -> usuarioService.guardar(new UsuarioForm(admin.id(), null, admin.nombreCompleto(), Rol.RECEPCION,
                null, true, null)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("al menos un administrador");

        long otroAdmin = usuarioService.guardar(new UsuarioForm(null, "adm." + sufijo(), "Otro admin", Rol.ADMIN, null, true, "Clave1234"));
        assertThatThrownBy(() -> usuarioService.guardar(new UsuarioForm(admin.id(), null, admin.nombreCompleto(), Rol.ADMIN,
                null, false, null)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("propio usuario");
        usuarioService.guardar(new UsuarioForm(otroAdmin, null, "Otro admin", Rol.ADMIN, null, false, null));
    }

    @Test
    void restablecerDesbloqueaYCambiarPasswordExigeLaActual() {
        String username = "r." + sufijo();
        long id = usuarioService.guardar(new UsuarioForm(null, username, "Recepción 2", Rol.RECEPCION, null, true, "Clave1234"));
        var u = usuarioRepository.findById(id).orElseThrow();
        u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(5));
        u.setIntentosFallidos(3);
        usuarioRepository.save(u);
        assertThat(usuarioService.listar().stream().filter(x -> x.id() == id).findFirst().orElseThrow().bloqueado()).isTrue();

        usuarioService.restablecerPassword(id, "Nueva2026");
        assertThat(autenticacionService.iniciarSesion(username, "Nueva2026").username()).isEqualTo(username);

        assertThatThrownBy(() -> usuarioService.cambiarPassword("mala", "Otra2026x", "Otra2026x"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("actual no es correcta");
        assertThatThrownBy(() -> usuarioService.cambiarPassword("Nueva2026", "Otra2026x", "distinta1"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no coincide");
        usuarioService.cambiarPassword("Nueva2026", "Otra2026x", "Otra2026x");
        assertThat(autenticacionService.iniciarSesion(username, "Otra2026x").username()).isEqualTo(username);
    }

    @Test
    void soloElAdministradorGestionaUsuariosYOdontologos() {
        iniciarSesion("recepcion");
        assertThatThrownBy(() -> usuarioService.guardar(new UsuarioForm(null, "x" + sufijo(), "X", Rol.RECEPCION, null, true, "Clave1234")))
                .isInstanceOf(PermisoDenegadoException.class).hasMessageContaining("administrar usuarios");
        assertThatThrownBy(() -> odontologoService.guardar(odontologo(cop(), List.of())))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> usuarioService.restablecerPassword(1, "Clave1234"))
                .isInstanceOf(PermisoDenegadoException.class);
    }
}
