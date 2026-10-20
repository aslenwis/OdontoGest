/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.Pagina;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PacienteResumen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pacientes: HC correlativa, validaciones, apoderado de menores, búsqueda paginada y permisos (RF-PAC-01 a 03).
 */
class PacienteServiceTest extends PruebaIntegracion {

    @Autowired private PacienteService pacienteService;
    @Autowired private SesionUsuario sesionUsuario;

    @BeforeEach
    void sesion() {
        iniciarSesion("recepcion");
    }

    static PacienteForm adulto(String dni, String nombres, String apellido) {
        return new PacienteForm(null, TipoDocumento.DNI, dni, nombres, apellido, "Quispe",
                LocalDate.now().minusYears(35), Sexo.F, "987654321", "paciente@correo.pe", "Jr. Lima 123", "Docente",
                null, null, null, null, null, true);
    }

    @Test
    void registraConHistoriaClinicaCorrelativa() {
        long a = pacienteService.guardar(adulto(dniAleatorio(), "Ana", "Pérez" + sufijo()));
        long b = pacienteService.guardar(adulto(dniAleatorio(), "Luis", "Pérez" + sufijo()));

        String hcA = pacienteService.obtener(a).numeroHistoria();
        String hcB = pacienteService.obtener(b).numeroHistoria();
        assertThat(hcA).matches("HC-\\d{6}");
        assertThat(Integer.parseInt(hcB.substring(3))).isEqualTo(Integer.parseInt(hcA.substring(3)) + 1);
    }

    @Test
    void editarNoCambiaLaHistoriaClinica() {
        long id = pacienteService.guardar(adulto(dniAleatorio(), "Rosa", "Mamani" + sufijo()));
        PacienteDetalle antes = pacienteService.obtener(id);
        PacienteForm f = adulto(antes.numeroDocumento(), "Rosa María", antes.apellidoPaterno());
        pacienteService.guardar(new PacienteForm(id, f.tipoDocumento(), f.numeroDocumento(), f.nombres(),
                f.apellidoPaterno(), f.apellidoMaterno(), f.fechaNacimiento(), f.sexo(), f.telefono(), f.email(),
                f.direccion(), f.ocupacion(), null, null, null, null, null, false));

        PacienteDetalle despues = pacienteService.obtener(id);
        assertThat(despues.numeroHistoria()).isEqualTo(antes.numeroHistoria());
        assertThat(despues.nombres()).isEqualTo("Rosa María");
        assertThat(despues.aceptaRecordatorios()).isFalse();
    }

    @Test
    void validaCamposObligatoriosYFormatosEnUnSoloMensaje() {
        PacienteForm f = new PacienteForm(null, TipoDocumento.DNI, "1234567", " ", "", null,
                LocalDate.now().plusDays(1), null, "12", "no-es-correo", null, null, null, null, null, null, null, true);
        assertThatThrownBy(() -> pacienteService.guardar(f))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("los nombres")
                .hasMessageContaining("apellido paterno")
                .hasMessageContaining("8 dígitos")
                .hasMessageContaining("no puede ser futura")
                .hasMessageContaining("sexo")
                .hasMessageContaining("correo")
                .hasMessageContaining("teléfono");
    }

    @Test
    void rechazaDocumentoDuplicado() {
        String dni = dniAleatorio();
        pacienteService.guardar(adulto(dni, "Juan", "Huanca" + sufijo()));
        assertThatThrownBy(() -> pacienteService.guardar(adulto(dni, "Otro", "Nombre" + sufijo())))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Ya existe un paciente con DNI " + dni);
    }

    @Test
    void menorDeEdadRequiereApoderadoYAdultoLoDescarta() {
        LocalDate diezAnios = LocalDate.now().minusYears(10);
        PacienteForm sinApoderado = new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Mateo", "Ccori" + sufijo(),
                null, diezAnios, Sexo.M, null, null, null, null, null, null, null, null, null, true);
        assertThatThrownBy(() -> pacienteService.guardar(sinApoderado))
                .isInstanceOf(NegocioException.class).hasMessageContaining("apoderado");

        PacienteForm conApoderado = new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Mateo", "Ccori" + sufijo(),
                null, diezAnios, Sexo.M, null, null, null, null, null, null, "Carmen Ccori", "DNI 40123456", "Madre", true);
        PacienteDetalle menor = pacienteService.obtener(pacienteService.guardar(conApoderado));
        assertThat(menor.esMenorDeEdad()).isTrue();
        assertThat(menor.apoderadoParentesco()).isEqualTo("Madre");

        PacienteForm adultoConApoderado = new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Elsa", "Puma" + sufijo(),
                null, LocalDate.now().minusYears(40), Sexo.F, null, null, null, null, null, null, "X", "Y", "Z", true);
        assertThat(pacienteService.obtener(pacienteService.guardar(adultoConApoderado)).apoderadoNombre()).isNull();
    }

    @Test
    void sinDocumentoIgnoraElNumeroYNormalizaElPasaporte() {
        PacienteForm sinDoc = new PacienteForm(null, TipoDocumento.SIN_DOCUMENTO, "123", "Bebé", "Sin" + sufijo(), null,
                LocalDate.now().minusYears(30), Sexo.F, null, null, null, null, null, null, null, null, null, true);
        assertThat(pacienteService.obtener(pacienteService.guardar(sinDoc)).numeroDocumento()).isNull();

        PacienteForm pasaporte = new PacienteForm(null, TipoDocumento.PASAPORTE, " ab 123456 ", "John", "Smith" + sufijo(),
                null, LocalDate.now().minusYears(30), Sexo.M, null, " John@Mail.COM ", null, null, null, null, null, null, null, true);
        PacienteDetalle p = pacienteService.obtener(pacienteService.guardar(pasaporte));
        assertThat(p.numeroDocumento()).isEqualTo("AB123456");
        assertThat(p.email()).isEqualTo("john@mail.com");
        assertThat(p.documento()).isEqualTo("PASAPORTE AB123456");
    }

    @Test
    void buscaPorPalabrasEnCualquierOrdenYPagina() {
        String apellido = "Zapata" + sufijo();
        for (int i = 0; i < 17; i++) {
            pacienteService.guardar(adulto(dniAleatorio(), "Nombre" + (char) ('a' + i), apellido));
        }
        Pagina<PacienteResumen> p1 = pacienteService.buscar(apellido, false, 0);
        assertThat(p1.total()).isEqualTo(17);
        assertThat(p1.contenido()).hasSize(PacienteService.TAMANIO_PAGINA);
        assertThat(p1.totalPaginas()).isEqualTo(2);
        assertThat(pacienteService.buscar(apellido, false, 1).contenido()).hasSize(2);

        assertThat(pacienteService.buscar("nombrec " + apellido.toUpperCase(), false, 0).contenido())
                .extracting(PacienteResumen::nombreCompleto).containsExactly(apellido + " Quispe, Nombrec");
        assertThat(pacienteService.buscar(apellido + " noexiste", false, 0).total()).isZero();
    }

    @Test
    void buscaPorDocumentoYPorHistoriaClinica() {
        String dni = dniAleatorio();
        long id = pacienteService.guardar(adulto(dni, "Teresa", "Choque" + sufijo()));
        String hc = pacienteService.obtener(id).numeroHistoria();
        assertThat(pacienteService.buscar(dni, false, 0).contenido()).extracting(PacienteResumen::id).containsExactly(id);
        assertThat(pacienteService.buscar(hc.toLowerCase(), false, 0).contenido()).extracting(PacienteResumen::id).contains(id);
        assertThat(pacienteService.buscar("100%_", false, 0).total()).as("comodines escapados").isZero();
    }

    @Test
    void losInactivosSoloAparecenSiSePiden() {
        String apellido = "Inactivo" + sufijo();
        long id = pacienteService.guardar(adulto(dniAleatorio(), "Pedro", apellido));
        pacienteService.cambiarEstado(id, false);
        assertThat(pacienteService.buscar(apellido, false, 0).total()).isZero();
        assertThat(pacienteService.buscar(apellido, true, 0).contenido()).extracting(PacienteResumen::activo).containsExactly(false);
    }

    @Test
    void sinSesionNoSePuedeRegistrar() {
        sesionUsuario.cerrar();
        assertThatThrownBy(() -> pacienteService.guardar(adulto(dniAleatorio(), "X", "Y" + sufijo())))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    void pacienteInexistente() {
        assertThatThrownBy(() -> pacienteService.obtener(999_999)).isInstanceOf(NegocioException.class);
    }
}
