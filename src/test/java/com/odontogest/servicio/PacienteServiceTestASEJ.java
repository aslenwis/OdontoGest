package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.PaginaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PacienteResumenASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PacienteServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private SesionUsuarioASEJ sesionUsuarioASEJ;

    @BeforeEach
    void sesionASEJ() {
        iniciarSesionASEJ("recepcion");
    }

    static PacienteFormASEJ adultoASEJ(String dniASEJ, String nombresASEJ, String apellidoASEJ) {
        return new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniASEJ, nombresASEJ, apellidoASEJ, "Quispe",
                LocalDate.now().minusYears(35), SexoASEJ.F, "987654321", "paciente@correo.pe", "Jr. Lima 123", "Docente",
                null, null, null, null, null, true);
    }

    @Test
    void registraConHistoriaClinicaCorrelativaASEJ() {
        long aASEJ = pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniAleatorioASEJ(), "Ana", "Pérez" + sufijoASEJ()));
        long bASEJ = pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniAleatorioASEJ(), "Luis", "Pérez" + sufijoASEJ()));

        String hcAASEJ = pacienteServiceASEJ.obtenerASEJ(aASEJ).numeroHistoriaASEJ();
        String hcBASEJ = pacienteServiceASEJ.obtenerASEJ(bASEJ).numeroHistoriaASEJ();
        assertThat(hcAASEJ).matches("HC-\\d{6}");
        assertThat(Integer.parseInt(hcBASEJ.substring(3))).isEqualTo(Integer.parseInt(hcAASEJ.substring(3)) + 1);
    }

    @Test
    void editarNoCambiaLaHistoriaClinicaASEJ() {
        long idASEJ = pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniAleatorioASEJ(), "Rosa", "Mamani" + sufijoASEJ()));
        PacienteDetalleASEJ antesASEJ = pacienteServiceASEJ.obtenerASEJ(idASEJ);
        PacienteFormASEJ fASEJ = adultoASEJ(antesASEJ.numeroDocumentoASEJ(), "Rosa María", antesASEJ.apellidoPaternoASEJ());
        pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(idASEJ, fASEJ.tipoDocumentoASEJ(), fASEJ.numeroDocumentoASEJ(), fASEJ.nombresASEJ(),
                fASEJ.apellidoPaternoASEJ(), fASEJ.apellidoMaternoASEJ(), fASEJ.fechaNacimientoASEJ(), fASEJ.sexoASEJ(), fASEJ.telefonoASEJ(), fASEJ.emailASEJ(),
                fASEJ.direccionASEJ(), fASEJ.ocupacionASEJ(), null, null, null, null, null, false));

        PacienteDetalleASEJ despuesASEJ = pacienteServiceASEJ.obtenerASEJ(idASEJ);
        assertThat(despuesASEJ.numeroHistoriaASEJ()).isEqualTo(antesASEJ.numeroHistoriaASEJ());
        assertThat(despuesASEJ.nombresASEJ()).isEqualTo("Rosa María");
        assertThat(despuesASEJ.aceptaRecordatoriosASEJ()).isFalse();
    }

    @Test
    void validaCamposObligatoriosYFormatosEnUnSoloMensajeASEJ() {
        PacienteFormASEJ fASEJ = new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, "1234567", " ", "", null,
                LocalDate.now().plusDays(1), null, "12", "no-es-correo", null, null, null, null, null, null, null, true);
        assertThatThrownBy(() -> pacienteServiceASEJ.guardarASEJ(fASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class)
                .hasMessageContaining("los nombres")
                .hasMessageContaining("apellido paterno")
                .hasMessageContaining("8 dígitos")
                .hasMessageContaining("no puede ser futura")
                .hasMessageContaining("sexo")
                .hasMessageContaining("correo")
                .hasMessageContaining("teléfono");
    }

    @Test
    void rechazaDocumentoDuplicadoASEJ() {
        String dniASEJ = dniAleatorioASEJ();
        pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniASEJ, "Juan", "Huanca" + sufijoASEJ()));
        assertThatThrownBy(() -> pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniASEJ, "Otro", "Nombre" + sufijoASEJ())))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("Ya existe un paciente con DNI " + dniASEJ);
    }

    @Test
    void menorDeEdadRequiereApoderadoYAdultoLoDescartaASEJ() {
        LocalDate diezAniosASEJ = LocalDate.now().minusYears(10);
        PacienteFormASEJ sinApoderadoASEJ = new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Mateo", "Ccori" + sufijoASEJ(),
                null, diezAniosASEJ, SexoASEJ.M, null, null, null, null, null, null, null, null, null, true);
        assertThatThrownBy(() -> pacienteServiceASEJ.guardarASEJ(sinApoderadoASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("apoderado");

        PacienteFormASEJ conApoderadoASEJ = new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Mateo", "Ccori" + sufijoASEJ(),
                null, diezAniosASEJ, SexoASEJ.M, null, null, null, null, null, null, "Carmen Ccori", "DNI 40123456", "Madre", true);
        PacienteDetalleASEJ menorASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteServiceASEJ.guardarASEJ(conApoderadoASEJ));
        assertThat(menorASEJ.esMenorDeEdadASEJ()).isTrue();
        assertThat(menorASEJ.apoderadoParentescoASEJ()).isEqualTo("Madre");

        PacienteFormASEJ adultoConApoderadoASEJ = new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Elsa", "Puma" + sufijoASEJ(),
                null, LocalDate.now().minusYears(40), SexoASEJ.F, null, null, null, null, null, null, "X", "Y", "Z", true);
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteServiceASEJ.guardarASEJ(adultoConApoderadoASEJ)).apoderadoNombreASEJ()).isNull();
    }

    @Test
    void sinDocumentoIgnoraElNumeroYNormalizaElPasaporteASEJ() {
        PacienteFormASEJ sinDocASEJ = new PacienteFormASEJ(null, TipoDocumentoASEJ.SIN_DOCUMENTO, "123", "Bebé", "Sin" + sufijoASEJ(), null,
                LocalDate.now().minusYears(30), SexoASEJ.F, null, null, null, null, null, null, null, null, null, true);
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteServiceASEJ.guardarASEJ(sinDocASEJ)).numeroDocumentoASEJ()).isNull();

        PacienteFormASEJ pasaporteASEJ = new PacienteFormASEJ(null, TipoDocumentoASEJ.PASAPORTE, " ab 123456 ", "John", "Smith" + sufijoASEJ(),
                null, LocalDate.now().minusYears(30), SexoASEJ.M, null, " John@Mail.COM ", null, null, null, null, null, null, null, true);
        PacienteDetalleASEJ pASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteServiceASEJ.guardarASEJ(pasaporteASEJ));
        assertThat(pASEJ.numeroDocumentoASEJ()).isEqualTo("AB123456");
        assertThat(pASEJ.emailASEJ()).isEqualTo("john@mail.com");
        assertThat(pASEJ.documentoASEJ()).isEqualTo("PASAPORTE AB123456");
    }

    @Test
    void buscaPorPalabrasEnCualquierOrdenYPaginaASEJ() {
        String apellidoASEJ = "Zapata" + sufijoASEJ();
        for (int iASEJ = 0; iASEJ < 17; iASEJ++) {
            pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniAleatorioASEJ(), "Nombre" + (char) ('a' + iASEJ), apellidoASEJ));
        }
        PaginaASEJ<PacienteResumenASEJ> p1ASEJ = pacienteServiceASEJ.buscarASEJ(apellidoASEJ, false, 0);
        assertThat(p1ASEJ.totalASEJ()).isEqualTo(17);
        assertThat(p1ASEJ.contenidoASEJ()).hasSize(PacienteServiceASEJ.TAMANIO_PAGINAASEJ);
        assertThat(p1ASEJ.totalPaginasASEJ()).isEqualTo(2);
        assertThat(pacienteServiceASEJ.buscarASEJ(apellidoASEJ, false, 1).contenidoASEJ()).hasSize(2);

        assertThat(pacienteServiceASEJ.buscarASEJ("nombrec " + apellidoASEJ.toUpperCase(), false, 0).contenidoASEJ())
                .extracting(PacienteResumenASEJ::nombreCompletoASEJ).containsExactly(apellidoASEJ + " Quispe, Nombrec");
        assertThat(pacienteServiceASEJ.buscarASEJ(apellidoASEJ + " noexiste", false, 0).totalASEJ()).isZero();
    }

    @Test
    void buscaPorDocumentoYPorHistoriaClinicaASEJ() {
        String dniASEJ = dniAleatorioASEJ();
        long idASEJ = pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniASEJ, "Teresa", "Choque" + sufijoASEJ()));
        String hcASEJ = pacienteServiceASEJ.obtenerASEJ(idASEJ).numeroHistoriaASEJ();
        assertThat(pacienteServiceASEJ.buscarASEJ(dniASEJ, false, 0).contenidoASEJ()).extracting(PacienteResumenASEJ::idASEJ).containsExactly(idASEJ);
        assertThat(pacienteServiceASEJ.buscarASEJ(hcASEJ.toLowerCase(), false, 0).contenidoASEJ()).extracting(PacienteResumenASEJ::idASEJ).contains(idASEJ);
        assertThat(pacienteServiceASEJ.buscarASEJ("100%_", false, 0).totalASEJ()).as("comodines escapados").isZero();
    }

    @Test
    void losInactivosSoloAparecenSiSePidenASEJ() {
        String apellidoASEJ = "Inactivo" + sufijoASEJ();
        long idASEJ = pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniAleatorioASEJ(), "Pedro", apellidoASEJ));
        pacienteServiceASEJ.cambiarEstadoASEJ(idASEJ, false);
        assertThat(pacienteServiceASEJ.buscarASEJ(apellidoASEJ, false, 0).totalASEJ()).isZero();
        assertThat(pacienteServiceASEJ.buscarASEJ(apellidoASEJ, true, 0).contenidoASEJ()).extracting(PacienteResumenASEJ::activoASEJ).containsExactly(false);
    }

    @Test
    void sinSesionNoSePuedeRegistrarASEJ() {
        sesionUsuarioASEJ.cerrarASEJ();
        assertThatThrownBy(() -> pacienteServiceASEJ.guardarASEJ(adultoASEJ(dniAleatorioASEJ(), "X", "Y" + sufijoASEJ())))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }

    @Test
    void pacienteInexistenteASEJ() {
        assertThatThrownBy(() -> pacienteServiceASEJ.obtenerASEJ(999_999)).isInstanceOf(NegocioExceptionASEJ.class);
    }
}
