/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.SimboloHallazgo;
import com.odontogest.dominio.odontograma.TipoRadicular;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.modelo.CategoriaProcedimiento;
import com.odontogest.modelo.HallazgoTipo;
import com.odontogest.modelo.Procedimiento;
import com.odontogest.modelo.ReglaSugerencia;
import com.odontogest.persistencia.repositorio.HallazgoTipoRepository;
import com.odontogest.persistencia.repositorio.ProcedimientoRepository;
import com.odontogest.persistencia.repositorio.ReglaSugerenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.odontogest.dominio.odontograma.ConteoIndice.C;
import static com.odontogest.dominio.odontograma.ConteoIndice.E;
import static com.odontogest.dominio.odontograma.ConteoIndice.NINGUNO;
import static com.odontogest.dominio.odontograma.ConteoIndice.O;
import static com.odontogest.dominio.odontograma.ConteoIndice.P;
import static com.odontogest.dominio.odontograma.ReglaColor.POR_ESTADO;
import static com.odontogest.dominio.odontograma.ReglaColor.SIEMPRE_AZUL;
import static com.odontogest.dominio.odontograma.ReglaColor.SIEMPRE_ROJO;
import static com.odontogest.dominio.odontograma.Representacion.ENTRE_PIEZAS;
import static com.odontogest.dominio.odontograma.Representacion.PIEZA;
import static com.odontogest.dominio.odontograma.Representacion.RANGO_PIEZAS;
import static com.odontogest.dominio.odontograma.Representacion.SUPERFICIE;
import static com.odontogest.dominio.tratamiento.FormaCobro.ARCADA;
import static com.odontogest.dominio.tratamiento.FormaCobro.BOCA;
import static com.odontogest.modelo.CategoriaProcedimiento.CIRUGIA;
import static com.odontogest.modelo.CategoriaProcedimiento.DIAGNOSTICO;
import static com.odontogest.modelo.CategoriaProcedimiento.ENDODONCIA;
import static com.odontogest.modelo.CategoriaProcedimiento.IMPLANTOLOGIA;
import static com.odontogest.modelo.CategoriaProcedimiento.OPERATORIA;
import static com.odontogest.modelo.CategoriaProcedimiento.PERIODONCIA;
import static com.odontogest.modelo.CategoriaProcedimiento.PREVENTIVO;
import static com.odontogest.modelo.CategoriaProcedimiento.PROTESIS;

/**
 * Catálogos iniciales (PLAN §3.5): tipos de hallazgo, tarifario demo y reglas de sugerencia. Se ejecuta
 * siempre y es idempotente: inserta por código solo lo que falta. Las reglas se cargan únicamente en el
 * primer arranque (cuando el catálogo de hallazgos estaba vacío), para no recrear reglas que el usuario borró.
 * <p>
 * ⚠️ El catálogo de hallazgos es representativo: validarlo contra el Anexo de la NTS N° 188-MINSA/DGIESP-2022
 * antes de la Fase 3 y ajustar estas filas.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class CatalogoSeeder implements ApplicationRunner {

    private record TipoDemo(String codigo, String nombre, String sigla, String siglasOpciones, Representacion representacion,
                            SimboloHallazgo simbolo, ReglaColor color, String capa, boolean oculta,
                            ConteoIndice cpod, ConteoIndice ceod) {
    }

    private record ProcedimientoDemo(String codigo, String nombre, CategoriaProcedimiento categoria, String precio,
                                     FormaCobro forma, int duracion) {
    }

    private record ReglaDemo(String hallazgo, EstadoHallazgo estado, TipoRadicular radicular, int min, int max,
                             String procedimiento, int fase, int prioridad) {
    }

    static final List<TipoDemo> TIPOS = List.of(
            new TipoDemo("CARIES", "Lesión de caries dental", null, null, SUPERFICIE, SimboloHallazgo.RELLENO, SIEMPRE_ROJO, "SUPERFICIE", false, C, C),
            new TipoDemo("RESTAURACION_DEFINITIVA", "Restauración definitiva", null, "[\"AM\",\"R\",\"IV\",\"IM\",\"IE\"]", SUPERFICIE, SimboloHallazgo.RELLENO, POR_ESTADO, "SUPERFICIE", false, O, O),
            new TipoDemo("RESTAURACION_TEMPORAL", "Restauración temporal", null, null, SUPERFICIE, SimboloHallazgo.CONTORNO_SUPERFICIE, SIEMPRE_ROJO, "SUPERFICIE", false, O, O),
            new TipoDemo("SELLANTE", "Sellante", "S", null, SUPERFICIE, SimboloHallazgo.SOLO_SIGLA, POR_ESTADO, "SUPERFICIE_SELLANTE", false, NINGUNO, NINGUNO),
            new TipoDemo("PIEZA_AUSENTE", "Pieza dentaria ausente", null, null, PIEZA, SimboloHallazgo.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, P, E),
            new TipoDemo("REMANENTE_RADICULAR", "Remanente radicular", "RR", null, PIEZA, SimboloHallazgo.RAIZ_TRIANGULO, SIEMPRE_ROJO, "ESTADO_PIEZA", false, C, E),
            new TipoDemo("IMPLANTE", "Implante dental", "IMP", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, POR_ESTADO, "ESTADO_PIEZA", false, P, NINGUNO),
            new TipoDemo("PIEZA_EN_ERUPCION", "Pieza dentaria en erupción", null, null, PIEZA, SimboloHallazgo.ZIGZAG, SIEMPRE_AZUL, "ESTADO_PIEZA", false, NINGUNO, NINGUNO),
            new TipoDemo("CORONA_DEFINITIVA", "Corona definitiva", null, "[\"CM\",\"CF\",\"CMC\",\"CV\",\"CLM\"]", PIEZA, SimboloHallazgo.CIRCUNFERENCIA, POR_ESTADO, "CORONA", false, O, O),
            new TipoDemo("CORONA_TEMPORAL", "Corona temporal", null, null, PIEZA, SimboloHallazgo.CIRCUNFERENCIA, SIEMPRE_ROJO, "CORONA", false, O, O),
            new TipoDemo("TRATAMIENTO_PULPAR", "Tratamiento pulpar", null, "[\"TC\",\"PC\",\"PP\"]", PIEZA, SimboloHallazgo.LINEA_RAIZ, POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO),
            new TipoDemo("FRACTURA", "Fractura", null, null, PIEZA, SimboloHallazgo.LINEA_FRACTURA, SIEMPRE_ROJO, "FRACTURA", false, NINGUNO, NINGUNO),
            new TipoDemo("PIEZA_EXTRUIDA", "Pieza extruida", null, null, PIEZA, SimboloHallazgo.FLECHA_ARRIBA, SIEMPRE_ROJO, "POSICION_VERTICAL", false, NINGUNO, NINGUNO),
            new TipoDemo("PIEZA_INTRUIDA", "Pieza intruida", null, null, PIEZA, SimboloHallazgo.FLECHA_ABAJO, SIEMPRE_ROJO, "POSICION_VERTICAL", false, NINGUNO, NINGUNO),
            new TipoDemo("GIROVERSION", "Giroversión", null, null, PIEZA, SimboloHallazgo.FLECHA_GIRO, SIEMPRE_ROJO, "GIRO", false, NINGUNO, NINGUNO),
            new TipoDemo("MOVILIDAD", "Movilidad patológica", null, "[\"M1\",\"M2\",\"M3\"]", PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_ROJO, "MOVILIDAD", false, NINGUNO, NINGUNO),
            new TipoDemo("DIASTEMA", "Diastema", null, null, ENTRE_PIEZAS, SimboloHallazgo.PARENTESIS, SIEMPRE_ROJO, "DIASTEMA", false, NINGUNO, NINGUNO),
            new TipoDemo("PROTESIS_FIJA", "Prótesis fija", null, null, RANGO_PIEZAS, SimboloHallazgo.PUENTE, POR_ESTADO, "PROTESIS_FIJA", false, NINGUNO, NINGUNO),
            new TipoDemo("PROTESIS_REMOVIBLE", "Prótesis removible", null, null, RANGO_PIEZAS, SimboloHallazgo.LLAVE, POR_ESTADO, "PROTESIS_REMOVIBLE", false, NINGUNO, NINGUNO),
            new TipoDemo("ORTODONCIA_FIJA", "Aparato ortodóntico fijo", null, null, RANGO_PIEZAS, SimboloHallazgo.LLAVE, POR_ESTADO, "ORTODONCIA", false, NINGUNO, NINGUNO));

    static final List<ProcedimientoDemo> PROCEDIMIENTOS = List.of(
            new ProcedimientoDemo("CONS", "Consulta y diagnóstico", DIAGNOSTICO, "50.00", BOCA, 30),
            new ProcedimientoDemo("RX-PA", "Radiografía periapical", DIAGNOSTICO, "25.00", FormaCobro.PIEZA, 15),
            new ProcedimientoDemo("PROF", "Profilaxis", PREVENTIVO, "80.00", BOCA, 45),
            new ProcedimientoDemo("DEST", "Destartraje", PERIODONCIA, "120.00", BOCA, 60),
            new ProcedimientoDemo("FLUOR", "Fluorización", PREVENTIVO, "60.00", BOCA, 20),
            new ProcedimientoDemo("SELL", "Sellante", PREVENTIVO, "50.00", FormaCobro.PIEZA, 20),
            new ProcedimientoDemo("RES-S", "Resina simple (1 superficie)", OPERATORIA, "120.00", FormaCobro.PIEZA, 30),
            new ProcedimientoDemo("RES-C", "Resina compuesta (2 o más superficies)", OPERATORIA, "180.00", FormaCobro.PIEZA, 45),
            new ProcedimientoDemo("IONO-T", "Ionómero temporal", OPERATORIA, "70.00", FormaCobro.PIEZA, 20),
            new ProcedimientoDemo("PULPO", "Pulpotomía", ENDODONCIA, "150.00", FormaCobro.PIEZA, 45),
            new ProcedimientoDemo("ENDO-U", "Endodoncia unirradicular", ENDODONCIA, "350.00", FormaCobro.PIEZA, 60),
            new ProcedimientoDemo("ENDO-B", "Endodoncia birradicular", ENDODONCIA, "450.00", FormaCobro.PIEZA, 90),
            new ProcedimientoDemo("ENDO-M", "Endodoncia multirradicular", ENDODONCIA, "600.00", FormaCobro.PIEZA, 120),
            new ProcedimientoDemo("EXO", "Exodoncia simple", CIRUGIA, "100.00", FormaCobro.PIEZA, 30),
            new ProcedimientoDemo("EXO-RR", "Remoción de remanente radicular", CIRUGIA, "120.00", FormaCobro.PIEZA, 45),
            new ProcedimientoDemo("CORO-T", "Corona temporal", PROTESIS, "150.00", FormaCobro.PIEZA, 45),
            new ProcedimientoDemo("CORO-MC", "Corona metal-cerámica", PROTESIS, "900.00", FormaCobro.PIEZA, 60),
            new ProcedimientoDemo("PF-PZ", "Prótesis fija por pieza", PROTESIS, "800.00", FormaCobro.PIEZA, 60),
            new ProcedimientoDemo("IMPL", "Implante", IMPLANTOLOGIA, "3500.00", FormaCobro.PIEZA, 90),
            new ProcedimientoDemo("FERU", "Férula", PROTESIS, "400.00", ARCADA, 30));

    static final List<ReglaDemo> REGLAS = List.of(
            new ReglaDemo(null, null, null, 0, 5, "CONS", 1, 1),
            new ReglaDemo(null, null, null, 0, 5, "PROF", 4, 2),
            new ReglaDemo("REMANENTE_RADICULAR", null, null, 0, 5, "EXO-RR", 1, 10),
            new ReglaDemo("FRACTURA", null, null, 0, 5, "RES-C", 1, 20),
            new ReglaDemo("CARIES", null, null, 1, 1, "RES-S", 2, 30),
            new ReglaDemo("CARIES", null, null, 2, 5, "RES-C", 2, 31),
            new ReglaDemo("RESTAURACION_TEMPORAL", null, null, 1, 1, "RES-S", 2, 32),
            new ReglaDemo("RESTAURACION_TEMPORAL", null, null, 2, 5, "RES-C", 2, 33),
            new ReglaDemo("RESTAURACION_DEFINITIVA", EstadoHallazgo.MALO, null, 1, 1, "RES-S", 2, 34),
            new ReglaDemo("RESTAURACION_DEFINITIVA", EstadoHallazgo.MALO, null, 2, 5, "RES-C", 2, 35),
            new ReglaDemo("TRATAMIENTO_PULPAR", EstadoHallazgo.MALO, TipoRadicular.UNIRRADICULAR, 0, 5, "ENDO-U", 1, 40),
            new ReglaDemo("TRATAMIENTO_PULPAR", EstadoHallazgo.MALO, TipoRadicular.BIRRADICULAR, 0, 5, "ENDO-B", 1, 41),
            new ReglaDemo("TRATAMIENTO_PULPAR", EstadoHallazgo.MALO, TipoRadicular.MULTIRRADICULAR, 0, 5, "ENDO-M", 1, 42),
            new ReglaDemo("CORONA_TEMPORAL", null, null, 0, 5, "CORO-MC", 3, 50),
            new ReglaDemo("CORONA_DEFINITIVA", EstadoHallazgo.MALO, null, 0, 5, "CORO-MC", 3, 51),
            new ReglaDemo("MOVILIDAD", null, null, 0, 5, "DEST", 2, 60));

    private final HallazgoTipoRepository hallazgoTipoRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final ReglaSugerenciaRepository reglaRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean primeraVez = hallazgoTipoRepository.count() == 0;
        int tipos = sembrarTipos();
        int procedimientos = sembrarProcedimientos();
        int reglas = primeraVez ? sembrarReglas() : 0;
        if (tipos + procedimientos + reglas > 0) {
            log.info("Catálogos iniciales: {} tipo(s) de hallazgo, {} procedimiento(s), {} regla(s)", tipos, procedimientos, reglas);
        }
    }

    private int sembrarTipos() {
        int insertados = 0;
        for (int i = 0; i < TIPOS.size(); i++) {
            TipoDemo d = TIPOS.get(i);
            if (hallazgoTipoRepository.findByCodigoIgnoreCase(d.codigo()).isPresent()) {
                continue;
            }
            HallazgoTipo t = new HallazgoTipo();
            t.setCodigo(d.codigo());
            t.setNombre(d.nombre());
            t.setSigla(d.sigla());
            t.setSiglasOpciones(d.siglasOpciones());
            t.setRepresentacion(d.representacion());
            t.setSimbolo(d.simbolo());
            t.setReglaColor(d.color());
            t.setGrupoExclusion(d.capa());
            t.setOcultaSuperficies(d.oculta());
            t.setConteoCpod(d.cpod());
            t.setConteoCeod(d.ceod());
            t.setActivo(true);
            t.setOrden((i + 1) * 10);
            hallazgoTipoRepository.save(t);
            insertados++;
        }
        return insertados;
    }

    private int sembrarProcedimientos() {
        int insertados = 0;
        for (ProcedimientoDemo d : PROCEDIMIENTOS) {
            if (procedimientoRepository.findByCodigoIgnoreCase(d.codigo()).isPresent()) {
                continue;
            }
            Procedimiento p = new Procedimiento();
            p.setCodigo(d.codigo());
            p.setNombre(d.nombre());
            p.setCategoria(d.categoria());
            p.setPrecio(new BigDecimal(d.precio()));
            p.setFormaCobro(d.forma());
            p.setDuracionMin(d.duracion());
            p.setActivo(true);
            procedimientoRepository.save(p);
            insertados++;
        }
        return insertados;
    }

    private int sembrarReglas() {
        for (ReglaDemo d : REGLAS) {
            ReglaSugerencia r = new ReglaSugerencia();
            r.setHallazgoTipo(d.hallazgo() == null ? null
                    : hallazgoTipoRepository.findByCodigoIgnoreCase(d.hallazgo()).orElseThrow());
            r.setEstado(d.estado());
            r.setTipoRadicular(d.radicular());
            r.setMinSuperficies(d.min());
            r.setMaxSuperficies(d.max());
            r.setProcedimiento(procedimientoRepository.findByCodigoIgnoreCase(d.procedimiento()).orElseThrow());
            r.setFase(d.fase());
            r.setPrioridad(d.prioridad());
            r.setActivo(true);
            reglaRepository.save(r);
        }
        return REGLAS.size();
    }
}
