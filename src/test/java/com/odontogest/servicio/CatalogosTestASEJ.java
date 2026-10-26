package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;
import com.odontogest.dominio.odontograma.TipoHallazgoDefASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.CategoriaProcedimientoASEJ;
import com.odontogest.seeder.CatalogoSeederASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoFormASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ProcedimientoFormASEJ;
import com.odontogest.servicio.dto.ReglaSugerenciaDtoASEJ;
import com.odontogest.servicio.dto.ReglaSugerenciaFormASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogosTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    @Autowired private ProcedimientoServiceASEJ procedimientoServiceASEJ;
    @Autowired private CatalogoSeederASEJ catalogoSeederASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    @BeforeEach
    void sesionASEJ() {
        iniciarSesionASEJ("admin");
    }

    private HallazgoTipoDtoASEJ tipoASEJ(String codigoASEJ) {
        return catalogoServiceASEJ.listarTiposASEJ(true).stream().filter(tASEJ -> tASEJ.codigoASEJ().equals(codigoASEJ)).findFirst().orElseThrow();
    }

    private static HallazgoTipoFormASEJ formASEJ(HallazgoTipoDtoASEJ tASEJ, String nombreASEJ, RepresentacionASEJ representacionASEJ) {
        return new HallazgoTipoFormASEJ(tASEJ.idASEJ(), tASEJ.codigoASEJ(), nombreASEJ, tASEJ.siglaASEJ(), tASEJ.siglasOpcionesASEJ(), representacionASEJ, tASEJ.simboloASEJ(),
                tASEJ.reglaColorASEJ(), tASEJ.grupoExclusionASEJ(), tASEJ.ocultaSuperficiesASEJ(), tASEJ.conteoCpodASEJ(), tASEJ.conteoCeodASEJ(), tASEJ.activoASEJ(), tASEJ.ordenASEJ());
    }

    @Test
    void elSeederCargaElCatalogoDelPlanASEJ() {
        assertThat(catalogoServiceASEJ.listarTiposASEJ(true)).hasSize(40);
        assertThat(procedimientoServiceASEJ.listarASEJ(true)).hasSize(20);
        assertThat(catalogoServiceASEJ.listarReglasASEJ()).hasSizeGreaterThanOrEqualTo(16);

        HallazgoTipoDtoASEJ ausenteASEJ = tipoASEJ("PIEZA_AUSENTE_DEX");
        assertThat(ausenteASEJ.ocultaSuperficiesASEJ()).isTrue();
        assertThat(ausenteASEJ.conteoCpodASEJ()).isEqualTo(ConteoIndiceASEJ.P);
        assertThat(ausenteASEJ.conteoCeodASEJ()).isEqualTo(ConteoIndiceASEJ.E);
        assertThat(ausenteASEJ.reglaColorASEJ()).isEqualTo(ReglaColorASEJ.SIEMPRE_AZUL);
        assertThat(ausenteASEJ.siglaASEJ()).isEqualTo("DEX");
        assertThat(tipoASEJ("PIEZA_AUSENTE_DNE").conteoCpodASEJ()).as("no erupcionada no cuenta").isEqualTo(ConteoIndiceASEJ.NINGUNO);
        assertThat(tipoASEJ("RESTAURACION_DEFINITIVA").siglasOpcionesASEJ()).containsExactly("AM", "R", "IV", "IM", "IE", "C");
        assertThat(tipoASEJ("CARIES").siglasOpcionesASEJ()).containsExactly("MB", "CE", "CD", "CDP");
        assertThat(tipoASEJ("SELLANTE").siglaASEJ()).isEqualTo("S");
        assertThat(tipoASEJ("SELLANTE").simboloASEJ()).isEqualTo(SimboloHallazgoASEJ.TRAZO_FOSAS);
    }

    @Test
    void elCatalogoRespetaColoresYDibujosDeLaNts188ASEJ() {
        for (String codigoASEJ : List.of("DIASTEMA", "GIROVERSION", "PIEZA_EXTRUIDA", "PIEZA_INTRUIDA", "FUSION",
                "GEMINACION", "TRANSPOSICION", "PIEZA_SUPERNUMERARIA", "MACRODONCIA", "MICRODONCIA", "IMPACTACION")) {
            assertThat(tipoASEJ(codigoASEJ).reglaColorASEJ()).as(codigoASEJ).isEqualTo(ReglaColorASEJ.SIEMPRE_AZUL);
        }
        assertThat(tipoASEJ("CORONA").simboloASEJ()).isEqualTo(SimboloHallazgoASEJ.CUADRADO_CORONA);
        assertThat(tipoASEJ("CORONA_TEMPORAL").siglaASEJ()).isEqualTo("CT");
        assertThat(tipoASEJ("CORONA_TEMPORAL").reglaColorASEJ()).isEqualTo(ReglaColorASEJ.SIEMPRE_ROJO);
        assertThat(tipoASEJ("REMANENTE_RADICULAR").simboloASEJ()).isEqualTo(SimboloHallazgoASEJ.SOLO_SIGLA);
        assertThat(tipoASEJ("REMANENTE_RADICULAR").reglaColorASEJ()).isEqualTo(ReglaColorASEJ.SIEMPRE_ROJO);
        assertThat(tipoASEJ("PROTESIS_REMOVIBLE").simboloASEJ()).isEqualTo(SimboloHallazgoASEJ.LINEAS_PARALELAS);
        assertThat(tipoASEJ("PROTESIS_COMPLETA").simboloASEJ()).isEqualTo(SimboloHallazgoASEJ.LINEAS_PARALELAS);
        assertThat(tipoASEJ("TRATAMIENTO_CONDUCTOS").siglasOpcionesASEJ()).containsExactly("TC", "PC");
        assertThat(tipoASEJ("PULPOTOMIA").siglaASEJ()).isEqualTo("PP");
        assertThat(tipoASEJ("PULPOTOMIA").grupoExclusionASEJ()).isEqualTo(tipoASEJ("TRATAMIENTO_CONDUCTOS").grupoExclusionASEJ());
        HallazgoTipoDtoASEJ edentuloASEJ = tipoASEJ("EDENTULO_TOTAL");
        assertThat(edentuloASEJ.representacionASEJ()).isEqualTo(RepresentacionASEJ.RANGO_PIEZAS);
        assertThat(edentuloASEJ.ocultaSuperficiesASEJ()).isTrue();
        assertThat(edentuloASEJ.conteoCpodASEJ()).isEqualTo(ConteoIndiceASEJ.P);
        assertThat(catalogoServiceASEJ.listarTiposASEJ(true)).extracting(HallazgoTipoDtoASEJ::simboloASEJ)
                .containsAll(List.of(SimboloHallazgoASEJ.values()));

        assertThat(jdbcASEJ.queryForObject("SELECT precio_cent FROM procedimiento WHERE codigo = 'RES-S'", Long.class)).isEqualTo(12000L);
        assertThat(jdbcASEJ.queryForObject("SELECT forma_cobro FROM procedimiento WHERE codigo = 'FERU'", String.class)).isEqualTo("ARCADA");

        List<TipoHallazgoDefASEJ> definicionesASEJ = catalogoServiceASEJ.definicionesASEJ();
        assertThat(definicionesASEJ).hasSize(40).extracting(TipoHallazgoDefASEJ::codigoASEJ).startsWith("CARIES", "RESTAURACION_DEFINITIVA");
    }

    @Test
    @Transactional
    void elSeederEsIdempotenteYNoRecreaReglasBorradasASEJ() throws Exception {
        ReglaSugerenciaDtoASEJ primeraASEJ = catalogoServiceASEJ.listarReglasASEJ().get(0);
        catalogoServiceASEJ.eliminarReglaASEJ(primeraASEJ.idASEJ());
        int reglasASEJ = catalogoServiceASEJ.listarReglasASEJ().size();

        catalogoSeederASEJ.run(new DefaultApplicationArguments());

        assertThat(catalogoServiceASEJ.listarTiposASEJ(true)).hasSize(40);
        assertThat(procedimientoServiceASEJ.listarASEJ(true)).hasSize(20);
        assertThat(catalogoServiceASEJ.listarReglasASEJ()).hasSize(reglasASEJ);
    }

    @Test
    @Transactional
    void guardaProcedimientoNormalizandoElCodigoASEJ() {
        String codigoASEJ = "t-" + sufijoASEJ().substring(0, 5);
        long idASEJ = procedimientoServiceASEJ.guardarASEJ(new ProcedimientoFormASEJ(null, codigoASEJ, "Blanqueamiento", CategoriaProcedimientoASEJ.ESTETICA,
                new BigDecimal("350.5"), FormaCobroASEJ.BOCA, 60, true));
        ProcedimientoDtoASEJ pASEJ = procedimientoServiceASEJ.listarASEJ(false).stream().filter(xASEJ -> xASEJ.idASEJ() == idASEJ).findFirst().orElseThrow();
        assertThat(pASEJ.codigoASEJ()).isEqualTo(codigoASEJ.toUpperCase());
        assertThat(pASEJ.precioASEJ()).isEqualByComparingTo("350.50");

        assertThatThrownBy(() -> procedimientoServiceASEJ.guardarASEJ(new ProcedimientoFormASEJ(null, codigoASEJ.toUpperCase(), "Otro",
                CategoriaProcedimientoASEJ.ESTETICA, BigDecimal.TEN, FormaCobroASEJ.BOCA, 60, true)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("Ya existe");

        procedimientoServiceASEJ.guardarASEJ(new ProcedimientoFormASEJ(idASEJ, codigoASEJ, "Blanqueamiento", CategoriaProcedimientoASEJ.ESTETICA,
                new BigDecimal("350.50"), FormaCobroASEJ.BOCA, 60, false));
        assertThat(procedimientoServiceASEJ.listarASEJ(false)).extracting(ProcedimientoDtoASEJ::idASEJ).doesNotContain(idASEJ);
        assertThat(procedimientoServiceASEJ.listarASEJ(true)).extracting(ProcedimientoDtoASEJ::idASEJ).contains(idASEJ);
    }

    @Test
    void validaPrecioDuracionYCodigoASEJ() {
        assertThatThrownBy(() -> procedimientoServiceASEJ.guardarASEJ(new ProcedimientoFormASEJ(null, "X Y", "", null,
                new BigDecimal("-1.005"), null, 2, true)))
                .isInstanceOf(NegocioExceptionASEJ.class)
                .hasMessageContaining("código").hasMessageContaining("nombre").hasMessageContaining("categoría")
                .hasMessageContaining("forma de cobro").hasMessageContaining("entre 0").hasMessageContaining("2 decimales")
                .hasMessageContaining("duración");
    }

    @Test
    @Transactional
    void creaTipoYValidaSiglasYConteosASEJ() {
        String codigoASEJ = "PRUEBA_" + sufijoASEJ().toUpperCase();
        long idASEJ = catalogoServiceASEJ.guardarTipoASEJ(new HallazgoTipoFormASEJ(null, codigoASEJ, "Mancha blanca", null, List.of("mb", "MB", "x1"),
                RepresentacionASEJ.SUPERFICIE, SimboloHallazgoASEJ.CONTORNO_SUPERFICIE, ReglaColorASEJ.SIEMPRE_ROJO, "superficie",
                false, ConteoIndiceASEJ.NINGUNO, null, true, 500));
        HallazgoTipoDtoASEJ tASEJ = tipoASEJ(codigoASEJ);
        assertThat(tASEJ.idASEJ()).isEqualTo(idASEJ);
        assertThat(tASEJ.siglasOpcionesASEJ()).containsExactly("MB", "X1");
        assertThat(tASEJ.grupoExclusionASEJ()).isEqualTo("SUPERFICIE");
        assertThat(tASEJ.conteoCeodASEJ()).isEqualTo(ConteoIndiceASEJ.NINGUNO);
        assertThat(tASEJ.enUsoASEJ()).isFalse();

        assertThatThrownBy(() -> catalogoServiceASEJ.guardarTipoASEJ(new HallazgoTipoFormASEJ(null, "OTRO_" + sufijoASEJ().toUpperCase(), "X", "S",
                List.of("A"), RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA, ReglaColorASEJ.SIEMPRE_AZUL, "C", false,
                ConteoIndiceASEJ.E, ConteoIndiceASEJ.P, true, 1)))
                .isInstanceOf(NegocioExceptionASEJ.class)
                .hasMessageContaining("no ambas").hasMessageContaining("CPO-D").hasMessageContaining("ceo-d");
        assertThatThrownBy(() -> catalogoServiceASEJ.guardarTipoASEJ(new HallazgoTipoFormASEJ(null, codigoASEJ, "Duplicado", null, List.of(),
                RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA, ReglaColorASEJ.SIEMPRE_AZUL, "CAPA", false, null, null, true, 1)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("Ya existe");
    }

    @Test
    @Transactional
    void tipoEnUsoNoPermiteCambiarSuEstructuraASEJ() {
        HallazgoTipoDtoASEJ cariesASEJ = tipoASEJ("CARIES");
        registrarHallazgoASEJ(cariesASEJ.idASEJ());

        HallazgoTipoDtoASEJ enUsoASEJ = tipoASEJ("CARIES");
        assertThat(enUsoASEJ.enUsoASEJ()).isTrue();
        assertThatThrownBy(() -> catalogoServiceASEJ.guardarTipoASEJ(formASEJ(enUsoASEJ, enUsoASEJ.nombreASEJ(), RepresentacionASEJ.PIEZA)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ya tiene hallazgos registrados");

        catalogoServiceASEJ.guardarTipoASEJ(formASEJ(enUsoASEJ, "Caries dental", RepresentacionASEJ.SUPERFICIE));
        assertThat(tipoASEJ("CARIES").nombreASEJ()).isEqualTo("Caries dental");
    }

    private void registrarHallazgoASEJ(long tipoIdASEJ) {
        long adminASEJ = jdbcASEJ.queryForObject("SELECT id FROM usuario WHERE username = 'admin'", Long.class);
        long odontologoASEJ = jdbcASEJ.queryForObject("SELECT MIN(id) FROM odontologo", Long.class);
        String ahoraASEJ = "2026-03-02T09:00:00";
        jdbcASEJ.update("""
                INSERT INTO paciente (numero_historia, tipo_documento, numero_documento, nombres, apellido_paterno,
                    fecha_nacimiento, sexo, creado_en, actualizado_en)
                VALUES ('HC-T' || abs(random() % 100000000), 'SIN_DOCUMENTO', NULL, 'Prueba', 'Hallazgo', '1990-01-01', 'F', ?, ?)""",
                ahoraASEJ, ahoraASEJ);
        long pacienteASEJ = jdbcASEJ.queryForObject("SELECT last_insert_rowid()", Long.class);
        jdbcASEJ.update("INSERT INTO atencion (paciente_id, odontologo_id, fecha, motivo_consulta) VALUES (?, ?, ?, 'Control')",
                pacienteASEJ, odontologoASEJ, ahoraASEJ);
        long atencionASEJ = jdbcASEJ.queryForObject("SELECT last_insert_rowid()", Long.class);
        jdbcASEJ.update("INSERT INTO odontograma (paciente_id, atencion_id, tipo, denticion, creado_en) VALUES (?, ?, 'INICIAL', 'PERMANENTE', ?)",
                pacienteASEJ, atencionASEJ, ahoraASEJ);
        long odontogramaASEJ = jdbcASEJ.queryForObject("SELECT last_insert_rowid()", Long.class);
        jdbcASEJ.update("""
                INSERT INTO hallazgo (odontograma_id, hallazgo_tipo_id, pieza_fdi, superficies, registrado_por, registrado_en)
                VALUES (?, ?, 36, 1, ?, ?)""", odontogramaASEJ, tipoIdASEJ, adminASEJ, ahoraASEJ);
    }

    @Test
    @Transactional
    void guardaYEliminaReglasConValidacionesASEJ() {
        HallazgoTipoDtoASEJ fracturaASEJ = tipoASEJ("FRACTURA");
        HallazgoTipoDtoASEJ pulparASEJ = tipoASEJ("TRATAMIENTO_CONDUCTOS");
        long endoASEJ = procedimientoServiceASEJ.listarASEJ(true).stream().filter(pASEJ -> pASEJ.codigoASEJ().equals("ENDO-M")).findFirst().orElseThrow().idASEJ();

        assertThatThrownBy(() -> catalogoServiceASEJ.guardarReglaASEJ(new ReglaSugerenciaFormASEJ(null, null, EstadoHallazgoASEJ.MALO, null,
                0, 5, endoASEJ, 1, 1, true)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("regla general");
        assertThatThrownBy(() -> catalogoServiceASEJ.guardarReglaASEJ(new ReglaSugerenciaFormASEJ(null, fracturaASEJ.idASEJ(), EstadoHallazgoASEJ.MALO,
                null, 0, 5, endoASEJ, 1, 1, true)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("no usa estado");
        assertThatThrownBy(() -> catalogoServiceASEJ.guardarReglaASEJ(new ReglaSugerenciaFormASEJ(null, pulparASEJ.idASEJ(), null, null,
                3, 2, endoASEJ, 5, -1, true)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("superficies").hasMessageContaining("fase")
                .hasMessageContaining("prioridad");

        long idASEJ = catalogoServiceASEJ.guardarReglaASEJ(new ReglaSugerenciaFormASEJ(null, pulparASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, null,
                0, 5, endoASEJ, 1, 7, true));
        ReglaSugerenciaDtoASEJ reglaASEJ = catalogoServiceASEJ.listarReglasASEJ().stream().filter(rASEJ -> rASEJ.idASEJ() == idASEJ).findFirst().orElseThrow();
        assertThat(reglaASEJ.hallazgoTipoNombreASEJ()).isEqualTo("Tratamiento de conductos");
        assertThat(reglaASEJ.procedimientoNombreASEJ()).startsWith("ENDO-M");

        catalogoServiceASEJ.eliminarReglaASEJ(idASEJ);
        assertThat(catalogoServiceASEJ.listarReglasASEJ()).extracting(ReglaSugerenciaDtoASEJ::idASEJ).doesNotContain(idASEJ);
        assertThatThrownBy(() -> catalogoServiceASEJ.eliminarReglaASEJ(idASEJ)).isInstanceOf(NegocioExceptionASEJ.class);
    }

    @Test
    void soloElAdministradorEditaCatalogosASEJ() {
        iniciarSesionASEJ("odontologo");
        HallazgoTipoDtoASEJ cariesASEJ = tipoASEJ("CARIES");
        assertThatThrownBy(() -> catalogoServiceASEJ.guardarTipoASEJ(formASEJ(cariesASEJ, "X", cariesASEJ.representacionASEJ())))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> procedimientoServiceASEJ.guardarASEJ(new ProcedimientoFormASEJ(null, "XX", "X",
                CategoriaProcedimientoASEJ.ESTETICA, BigDecimal.ONE, FormaCobroASEJ.BOCA, 30, true)))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }
}
