/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.SimboloHallazgo;
import com.odontogest.dominio.odontograma.TipoHallazgoDef;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.CategoriaProcedimiento;
import com.odontogest.seeder.CatalogoSeeder;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.HallazgoTipoForm;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ProcedimientoForm;
import com.odontogest.servicio.dto.ReglaSugerenciaDto;
import com.odontogest.servicio.dto.ReglaSugerenciaForm;
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

/**
 * Catálogo inicial (CatalogoSeeder), tarifario, tipos de hallazgo y reglas de sugerencia (RF-CAT-01 a 03).
 */
class CatalogosTest extends PruebaIntegracion {

    @Autowired private CatalogoHallazgoService catalogoService;
    @Autowired private ProcedimientoService procedimientoService;
    @Autowired private CatalogoSeeder catalogoSeeder;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void sesion() {
        iniciarSesion("admin");
    }

    private HallazgoTipoDto tipo(String codigo) {
        return catalogoService.listarTipos(true).stream().filter(t -> t.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static HallazgoTipoForm form(HallazgoTipoDto t, String nombre, Representacion representacion) {
        return new HallazgoTipoForm(t.id(), t.codigo(), nombre, t.sigla(), t.siglasOpciones(), representacion, t.simbolo(),
                t.reglaColor(), t.grupoExclusion(), t.ocultaSuperficies(), t.conteoCpod(), t.conteoCeod(), t.activo(), t.orden());
    }

    // ------------------------------------------------------------------ seeder

    @Test
    void elSeederCargaElCatalogoDelPlan() {
        assertThat(catalogoService.listarTipos(true)).hasSize(40);
        assertThat(procedimientoService.listar(true)).hasSize(20);
        assertThat(catalogoService.listarReglas()).hasSizeGreaterThanOrEqualTo(16);

        HallazgoTipoDto ausente = tipo("PIEZA_AUSENTE_DEX");
        assertThat(ausente.ocultaSuperficies()).isTrue();
        assertThat(ausente.conteoCpod()).isEqualTo(ConteoIndice.P);
        assertThat(ausente.conteoCeod()).isEqualTo(ConteoIndice.E);
        assertThat(ausente.reglaColor()).isEqualTo(ReglaColor.SIEMPRE_AZUL);
        assertThat(ausente.sigla()).isEqualTo("DEX");
        assertThat(tipo("PIEZA_AUSENTE_DNE").conteoCpod()).as("no erupcionada no cuenta").isEqualTo(ConteoIndice.NINGUNO);
        assertThat(tipo("RESTAURACION_DEFINITIVA").siglasOpciones()).containsExactly("AM", "R", "IV", "IM", "IE", "C");
        assertThat(tipo("CARIES").siglasOpciones()).containsExactly("MB", "CE", "CD", "CDP");
        assertThat(tipo("SELLANTE").sigla()).isEqualTo("S");
        assertThat(tipo("SELLANTE").simbolo()).isEqualTo(SimboloHallazgo.TRAZO_FOSAS);
    }

    @Test
    void elCatalogoRespetaColoresYDibujosDeLaNts188() {
        // 6.1.6, 6.1.13, 6.1.24, 6.1.25: dibujos en azul
        for (String codigo : List.of("DIASTEMA", "GIROVERSION", "PIEZA_EXTRUIDA", "PIEZA_INTRUIDA", "FUSION",
                "GEMINACION", "TRANSPOSICION", "PIEZA_SUPERNUMERARIA", "MACRODONCIA", "MICRODONCIA", "IMPACTACION")) {
            assertThat(tipo(codigo).reglaColor()).as(codigo).isEqualTo(ReglaColor.SIEMPRE_AZUL);
        }
        // 6.1.3 y 6.1.4: cuadrado que bordea la corona; la temporal en rojo con sigla CT
        assertThat(tipo("CORONA").simbolo()).isEqualTo(SimboloHallazgo.CUADRADO_CORONA);
        assertThat(tipo("CORONA_TEMPORAL").sigla()).isEqualTo("CT");
        assertThat(tipo("CORONA_TEMPORAL").reglaColor()).isEqualTo(ReglaColor.SIEMPRE_ROJO);
        // 6.1.32: remanente radicular solo con la sigla RR en rojo
        assertThat(tipo("REMANENTE_RADICULAR").simbolo()).isEqualTo(SimboloHallazgo.SOLO_SIGLA);
        assertThat(tipo("REMANENTE_RADICULAR").reglaColor()).isEqualTo(ReglaColor.SIEMPRE_ROJO);
        // 6.1.30 y 6.1.31: prótesis completa y removible con dos líneas paralelas
        assertThat(tipo("PROTESIS_REMOVIBLE").simbolo()).isEqualTo(SimboloHallazgo.LINEAS_PARALELAS);
        assertThat(tipo("PROTESIS_COMPLETA").simbolo()).isEqualTo(SimboloHallazgo.LINEAS_PARALELAS);
        // 6.1.37 y 6.1.27: conductos (TC/PC) y pulpotomía (PP) comparten la capa pulpar
        assertThat(tipo("TRATAMIENTO_CONDUCTOS").siglasOpciones()).containsExactly("TC", "PC");
        assertThat(tipo("PULPOTOMIA").sigla()).isEqualTo("PP");
        assertThat(tipo("PULPOTOMIA").grupoExclusion()).isEqualTo(tipo("TRATAMIENTO_CONDUCTOS").grupoExclusion());
        // 6.1.7: edéntulo total, rango que oculta superficies y cuenta como perdida
        HallazgoTipoDto edentulo = tipo("EDENTULO_TOTAL");
        assertThat(edentulo.representacion()).isEqualTo(Representacion.RANGO_PIEZAS);
        assertThat(edentulo.ocultaSuperficies()).isTrue();
        assertThat(edentulo.conteoCpod()).isEqualTo(ConteoIndice.P);
        // Los 26 símbolos del enum están permitidos por el CHECK del esquema (se insertan al sembrar)
        assertThat(catalogoService.listarTipos(true)).extracting(HallazgoTipoDto::simbolo)
                .containsAll(List.of(SimboloHallazgo.values()));

        assertThat(jdbc.queryForObject("SELECT precio_cent FROM procedimiento WHERE codigo = 'RES-S'", Long.class)).isEqualTo(12000L);
        assertThat(jdbc.queryForObject("SELECT forma_cobro FROM procedimiento WHERE codigo = 'FERU'", String.class)).isEqualTo("ARCADA");

        List<TipoHallazgoDef> definiciones = catalogoService.definiciones();
        assertThat(definiciones).hasSize(40).extracting(TipoHallazgoDef::codigo).startsWith("CARIES", "RESTAURACION_DEFINITIVA");
    }

    @Test
    @Transactional
    void elSeederEsIdempotenteYNoRecreaReglasBorradas() throws Exception {
        ReglaSugerenciaDto primera = catalogoService.listarReglas().get(0);
        catalogoService.eliminarRegla(primera.id());
        int reglas = catalogoService.listarReglas().size();

        catalogoSeeder.run(new DefaultApplicationArguments());

        assertThat(catalogoService.listarTipos(true)).hasSize(40);
        assertThat(procedimientoService.listar(true)).hasSize(20);
        assertThat(catalogoService.listarReglas()).hasSize(reglas);
    }

    // ------------------------------------------------------------------ procedimientos

    @Test
    @Transactional
    void guardaProcedimientoNormalizandoElCodigo() {
        String codigo = "t-" + sufijo().substring(0, 5);
        long id = procedimientoService.guardar(new ProcedimientoForm(null, codigo, "Blanqueamiento", CategoriaProcedimiento.ESTETICA,
                new BigDecimal("350.5"), FormaCobro.BOCA, 60, true));
        ProcedimientoDto p = procedimientoService.listar(false).stream().filter(x -> x.id() == id).findFirst().orElseThrow();
        assertThat(p.codigo()).isEqualTo(codigo.toUpperCase());
        assertThat(p.precio()).isEqualByComparingTo("350.50");

        assertThatThrownBy(() -> procedimientoService.guardar(new ProcedimientoForm(null, codigo.toUpperCase(), "Otro",
                CategoriaProcedimiento.ESTETICA, BigDecimal.TEN, FormaCobro.BOCA, 60, true)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Ya existe");

        procedimientoService.guardar(new ProcedimientoForm(id, codigo, "Blanqueamiento", CategoriaProcedimiento.ESTETICA,
                new BigDecimal("350.50"), FormaCobro.BOCA, 60, false));
        assertThat(procedimientoService.listar(false)).extracting(ProcedimientoDto::id).doesNotContain(id);
        assertThat(procedimientoService.listar(true)).extracting(ProcedimientoDto::id).contains(id);
    }

    @Test
    void validaPrecioDuracionYCodigo() {
        assertThatThrownBy(() -> procedimientoService.guardar(new ProcedimientoForm(null, "X Y", "", null,
                new BigDecimal("-1.005"), null, 2, true)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("código").hasMessageContaining("nombre").hasMessageContaining("categoría")
                .hasMessageContaining("forma de cobro").hasMessageContaining("entre 0").hasMessageContaining("2 decimales")
                .hasMessageContaining("duración");
    }

    // ------------------------------------------------------------------ tipos de hallazgo

    @Test
    @Transactional
    void creaTipoYValidaSiglasYConteos() {
        String codigo = "PRUEBA_" + sufijo().toUpperCase();
        long id = catalogoService.guardarTipo(new HallazgoTipoForm(null, codigo, "Mancha blanca", null, List.of("mb", "MB", "x1"),
                Representacion.SUPERFICIE, SimboloHallazgo.CONTORNO_SUPERFICIE, ReglaColor.SIEMPRE_ROJO, "superficie",
                false, ConteoIndice.NINGUNO, null, true, 500));
        HallazgoTipoDto t = tipo(codigo);
        assertThat(t.id()).isEqualTo(id);
        assertThat(t.siglasOpciones()).containsExactly("MB", "X1");
        assertThat(t.grupoExclusion()).isEqualTo("SUPERFICIE");
        assertThat(t.conteoCeod()).isEqualTo(ConteoIndice.NINGUNO);
        assertThat(t.enUso()).isFalse();

        assertThatThrownBy(() -> catalogoService.guardarTipo(new HallazgoTipoForm(null, "OTRO_" + sufijo().toUpperCase(), "X", "S",
                List.of("A"), Representacion.PIEZA, SimboloHallazgo.ASPA, ReglaColor.SIEMPRE_AZUL, "C", false,
                ConteoIndice.E, ConteoIndice.P, true, 1)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no ambas").hasMessageContaining("CPO-D").hasMessageContaining("ceo-d");
        assertThatThrownBy(() -> catalogoService.guardarTipo(new HallazgoTipoForm(null, codigo, "Duplicado", null, List.of(),
                Representacion.PIEZA, SimboloHallazgo.ASPA, ReglaColor.SIEMPRE_AZUL, "CAPA", false, null, null, true, 1)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Ya existe");
    }

    @Test
    @Transactional
    void tipoEnUsoNoPermiteCambiarSuEstructura() {
        HallazgoTipoDto caries = tipo("CARIES");
        registrarHallazgo(caries.id());

        HallazgoTipoDto enUso = tipo("CARIES");
        assertThat(enUso.enUso()).isTrue();
        assertThatThrownBy(() -> catalogoService.guardarTipo(form(enUso, enUso.nombre(), Representacion.PIEZA)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya tiene hallazgos registrados");

        catalogoService.guardarTipo(form(enUso, "Caries dental", Representacion.SUPERFICIE));
        assertThat(tipo("CARIES").nombre()).isEqualTo("Caries dental");
    }

    /** Inserta paciente → atención → odontograma → hallazgo con SQL directo (los servicios llegan en la Fase 3). */
    private void registrarHallazgo(long tipoId) {
        long admin = jdbc.queryForObject("SELECT id FROM usuario WHERE username = 'admin'", Long.class);
        long odontologo = jdbc.queryForObject("SELECT MIN(id) FROM odontologo", Long.class);
        String ahora = "2026-03-02T09:00:00";
        jdbc.update("""
                INSERT INTO paciente (numero_historia, tipo_documento, numero_documento, nombres, apellido_paterno,
                    fecha_nacimiento, sexo, creado_en, actualizado_en)
                VALUES ('HC-T' || abs(random() % 100000000), 'SIN_DOCUMENTO', NULL, 'Prueba', 'Hallazgo', '1990-01-01', 'F', ?, ?)""",
                ahora, ahora);
        long paciente = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        jdbc.update("INSERT INTO atencion (paciente_id, odontologo_id, fecha, motivo_consulta) VALUES (?, ?, ?, 'Control')",
                paciente, odontologo, ahora);
        long atencion = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        jdbc.update("INSERT INTO odontograma (paciente_id, atencion_id, tipo, denticion, creado_en) VALUES (?, ?, 'INICIAL', 'PERMANENTE', ?)",
                paciente, atencion, ahora);
        long odontograma = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        jdbc.update("""
                INSERT INTO hallazgo (odontograma_id, hallazgo_tipo_id, pieza_fdi, superficies, registrado_por, registrado_en)
                VALUES (?, ?, 36, 1, ?, ?)""", odontograma, tipoId, admin, ahora);
    }

    // ------------------------------------------------------------------ reglas

    @Test
    @Transactional
    void guardaYEliminaReglasConValidaciones() {
        HallazgoTipoDto fractura = tipo("FRACTURA");
        HallazgoTipoDto pulpar = tipo("TRATAMIENTO_CONDUCTOS");
        long endo = procedimientoService.listar(true).stream().filter(p -> p.codigo().equals("ENDO-M")).findFirst().orElseThrow().id();

        assertThatThrownBy(() -> catalogoService.guardarRegla(new ReglaSugerenciaForm(null, null, EstadoHallazgo.MALO, null,
                0, 5, endo, 1, 1, true)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("regla general");
        assertThatThrownBy(() -> catalogoService.guardarRegla(new ReglaSugerenciaForm(null, fractura.id(), EstadoHallazgo.MALO,
                null, 0, 5, endo, 1, 1, true)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no usa estado");
        assertThatThrownBy(() -> catalogoService.guardarRegla(new ReglaSugerenciaForm(null, pulpar.id(), null, null,
                3, 2, endo, 5, -1, true)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("superficies").hasMessageContaining("fase")
                .hasMessageContaining("prioridad");

        long id = catalogoService.guardarRegla(new ReglaSugerenciaForm(null, pulpar.id(), EstadoHallazgo.MALO, null,
                0, 5, endo, 1, 7, true));
        ReglaSugerenciaDto regla = catalogoService.listarReglas().stream().filter(r -> r.id() == id).findFirst().orElseThrow();
        assertThat(regla.hallazgoTipoNombre()).isEqualTo("Tratamiento de conductos");
        assertThat(regla.procedimientoNombre()).startsWith("ENDO-M");

        catalogoService.eliminarRegla(id);
        assertThat(catalogoService.listarReglas()).extracting(ReglaSugerenciaDto::id).doesNotContain(id);
        assertThatThrownBy(() -> catalogoService.eliminarRegla(id)).isInstanceOf(NegocioException.class);
    }

    @Test
    void soloElAdministradorEditaCatalogos() {
        iniciarSesion("odontologo");
        HallazgoTipoDto caries = tipo("CARIES");
        assertThatThrownBy(() -> catalogoService.guardarTipo(form(caries, "X", caries.representacion())))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> procedimientoService.guardar(new ProcedimientoForm(null, "XX", "X",
                CategoriaProcedimiento.ESTETICA, BigDecimal.ONE, FormaCobro.BOCA, 30, true)))
                .isInstanceOf(PermisoDenegadoException.class);
    }
}
