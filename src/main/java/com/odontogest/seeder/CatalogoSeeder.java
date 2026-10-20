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
 * El catálogo de hallazgos sigue el numeral 6.1 de la NTS N° 188-MINSA/DGIESP-2022: siglas, colores y forma de
 * dibujo. Decisiones propias: la pieza ausente se separa en DEX/DNE/DAO para que solo la extraída por caries cuente
 * en el CPO-D/ceo-d, y el edéntulo total cuenta como pieza perdida en cada pieza que cubre.
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

    /** NTS N° 188-MINSA/DGIESP-2022, numeral 6.1, en el orden de la paleta (al final de cada fila, el numeral). */
    static final List<TipoDemo> TIPOS = List.of(
            new TipoDemo("CARIES", "Lesión de caries dental", null, "[\"MB\",\"CE\",\"CD\",\"CDP\"]", SUPERFICIE, SimboloHallazgo.RELLENO, SIEMPRE_ROJO, "SUPERFICIE", false, C, C),   // 6.1.16
            new TipoDemo("RESTAURACION_DEFINITIVA", "Restauración definitiva", null, "[\"AM\",\"R\",\"IV\",\"IM\",\"IE\",\"C\"]", SUPERFICIE, SimboloHallazgo.RELLENO, POR_ESTADO, "SUPERFICIE", false, O, O),   // 6.1.33
            new TipoDemo("RESTAURACION_TEMPORAL", "Restauración temporal", null, null, SUPERFICIE, SimboloHallazgo.CONTORNO_SUPERFICIE, SIEMPRE_ROJO, "SUPERFICIE", false, O, O),   // 6.1.34
            new TipoDemo("SELLANTE", "Sellante", "S", null, SUPERFICIE, SimboloHallazgo.TRAZO_FOSAS, POR_ESTADO, "SUPERFICIE_SELLANTE", false, NINGUNO, NINGUNO),   // 6.1.35
            new TipoDemo("SUPERFICIE_DESGASTADA", "Superficie desgastada", "DES", null, SUPERFICIE, SimboloHallazgo.LINEA_DESGASTE, SIEMPRE_ROJO, "DESGASTE", false, NINGUNO, NINGUNO),   // 6.1.36
            new TipoDemo("FOSAS_FISURAS_PROFUNDAS", "Fosas y fisuras profundas", "FFP", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_AZUL, "FOSAS_FISURAS", false, NINGUNO, NINGUNO),   // 6.1.9
            new TipoDemo("DEFECTO_ESMALTE", "Defecto de desarrollo del esmalte (DDE)", null, "[\"O\",\"PE\"]", PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_ROJO, "ESMALTE", false, NINGUNO, NINGUNO),   // 6.1.5
            new TipoDemo("PIEZA_AUSENTE_DEX", "Pieza ausente por caries (DEX)", "DEX", null, PIEZA, SimboloHallazgo.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, P, E),   // 6.1.20
            new TipoDemo("PIEZA_AUSENTE_DNE", "Pieza no erupcionada (DNE)", "DNE", null, PIEZA, SimboloHallazgo.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, NINGUNO, NINGUNO),   // 6.1.20
            new TipoDemo("PIEZA_AUSENTE_DAO", "Pieza ausente por otras razones (DAO)", "DAO", null, PIEZA, SimboloHallazgo.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, NINGUNO, NINGUNO),   // 6.1.20
            new TipoDemo("REMANENTE_RADICULAR", "Remanente radicular", "RR", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_ROJO, "ESTADO_PIEZA", false, C, E),   // 6.1.32
            new TipoDemo("IMPLANTE", "Implante dental", "IMP", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, POR_ESTADO, "ESTADO_PIEZA", false, P, NINGUNO),   // 6.1.15
            new TipoDemo("PIEZA_EN_ERUPCION", "Pieza dentaria en erupción", null, null, PIEZA, SimboloHallazgo.FLECHA_ZIGZAG, SIEMPRE_AZUL, "ESTADO_PIEZA", false, NINGUNO, NINGUNO),   // 6.1.23
            new TipoDemo("CORONA", "Corona", null, "[\"CM\",\"CF\",\"CMC\",\"CV\",\"CLM\"]", PIEZA, SimboloHallazgo.CUADRADO_CORONA, POR_ESTADO, "CORONA", false, O, O),   // 6.1.3
            new TipoDemo("CORONA_TEMPORAL", "Corona temporal", "CT", null, PIEZA, SimboloHallazgo.CUADRADO_CORONA, SIEMPRE_ROJO, "CORONA", false, O, O),   // 6.1.4
            new TipoDemo("ESPIGO_MUNON", "Espigo-muñón", null, null, PIEZA, SimboloHallazgo.ESPIGO_MUNON, POR_ESTADO, "ESPIGO", false, NINGUNO, NINGUNO),   // 6.1.8
            new TipoDemo("TRATAMIENTO_CONDUCTOS", "Tratamiento de conductos", null, "[\"TC\",\"PC\"]", PIEZA, SimboloHallazgo.LINEA_RAIZ, POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO),   // 6.1.37
            new TipoDemo("PULPOTOMIA", "Pulpotomía", "PP", null, PIEZA, SimboloHallazgo.PULPA_CORONAL, POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO),   // 6.1.27
            new TipoDemo("FRACTURA", "Fractura dental", null, null, PIEZA, SimboloHallazgo.LINEA_FRACTURA, SIEMPRE_ROJO, "FRACTURA", false, NINGUNO, NINGUNO),   // 6.1.10
            new TipoDemo("MOVILIDAD", "Movilidad patológica", null, "[\"M1\",\"M2\",\"M3\"]", PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_ROJO, "MOVILIDAD", false, NINGUNO, NINGUNO),   // 6.1.19
            new TipoDemo("PIEZA_EXTRUIDA", "Pieza dentaria extruida", null, null, PIEZA, SimboloHallazgo.FLECHA_EXTRUSION, SIEMPRE_AZUL, "POSICION_VERTICAL", false, NINGUNO, NINGUNO),   // 6.1.24
            new TipoDemo("PIEZA_INTRUIDA", "Pieza dentaria intruida", null, null, PIEZA, SimboloHallazgo.FLECHA_INTRUSION, SIEMPRE_AZUL, "POSICION_VERTICAL", false, NINGUNO, NINGUNO),   // 6.1.25
            new TipoDemo("GIROVERSION", "Giroversión", null, null, PIEZA, SimboloHallazgo.FLECHA_GIRO, SIEMPRE_AZUL, "GIRO", false, NINGUNO, NINGUNO),   // 6.1.13
            new TipoDemo("POSICION_ANORMAL", "Posición anormal dentaria", null, "[\"M\",\"D\",\"V\",\"P\",\"L\"]", PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_AZUL, "POSICION", false, NINGUNO, NINGUNO),   // 6.1.28
            new TipoDemo("PIEZA_ECTOPICA", "Pieza dentaria ectópica", "E", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_AZUL, "ECTOPICA", false, NINGUNO, NINGUNO),   // 6.1.22
            new TipoDemo("IMPACTACION", "Impactación", "I", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_AZUL, "IMPACTACION", false, NINGUNO, NINGUNO),   // 6.1.14
            new TipoDemo("MACRODONCIA", "Macrodoncia", "MAC", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_AZUL, "TAMANO", false, NINGUNO, NINGUNO),   // 6.1.17
            new TipoDemo("MICRODONCIA", "Microdoncia", "MIC", null, PIEZA, SimboloHallazgo.SOLO_SIGLA, SIEMPRE_AZUL, "TAMANO", false, NINGUNO, NINGUNO),   // 6.1.18
            new TipoDemo("PIEZA_CLAVIJA", "Pieza dentaria en clavija", null, null, PIEZA, SimboloHallazgo.TRIANGULO_RAIZ, SIEMPRE_AZUL, "CLAVIJA", false, NINGUNO, NINGUNO),   // 6.1.21
            new TipoDemo("GEMINACION", "Geminación", null, null, PIEZA, SimboloHallazgo.CIRCULO_NUMERO, SIEMPRE_AZUL, "GEMINACION", false, NINGUNO, NINGUNO),   // 6.1.12
            new TipoDemo("DIASTEMA", "Diastema", null, null, ENTRE_PIEZAS, SimboloHallazgo.PARENTESIS, SIEMPRE_AZUL, "DIASTEMA", false, NINGUNO, NINGUNO),   // 6.1.6
            new TipoDemo("FUSION", "Fusión", null, null, ENTRE_PIEZAS, SimboloHallazgo.CIRCULOS_NUMEROS, SIEMPRE_AZUL, "FUSION", false, NINGUNO, NINGUNO),   // 6.1.11
            new TipoDemo("TRANSPOSICION", "Transposición dentaria", null, null, ENTRE_PIEZAS, SimboloHallazgo.FLECHAS_CRUZADAS, SIEMPRE_AZUL, "TRANSPOSICION", false, NINGUNO, NINGUNO),   // 6.1.38
            new TipoDemo("PIEZA_SUPERNUMERARIA", "Pieza dentaria supernumeraria", null, null, ENTRE_PIEZAS, SimboloHallazgo.CIRCULO_S, SIEMPRE_AZUL, "SUPERNUMERARIA", false, NINGUNO, NINGUNO),   // 6.1.26
            new TipoDemo("PROTESIS_FIJA", "Prótesis dental parcial fija", null, null, RANGO_PIEZAS, SimboloHallazgo.PUENTE, POR_ESTADO, "PROTESIS_FIJA", false, NINGUNO, NINGUNO),   // 6.1.29
            new TipoDemo("PROTESIS_REMOVIBLE", "Prótesis dental parcial removible", null, null, RANGO_PIEZAS, SimboloHallazgo.LINEAS_PARALELAS, POR_ESTADO, "PROTESIS_REMOVIBLE", false, NINGUNO, NINGUNO),   // 6.1.31
            new TipoDemo("PROTESIS_COMPLETA", "Prótesis dental completa", null, null, RANGO_PIEZAS, SimboloHallazgo.LINEAS_PARALELAS, POR_ESTADO, "PROTESIS_COMPLETA", false, NINGUNO, NINGUNO),   // 6.1.30
            new TipoDemo("APARATO_ORTODONTICO_FIJO", "Aparato ortodóntico fijo", null, null, RANGO_PIEZAS, SimboloHallazgo.CUADROS_APICES, POR_ESTADO, "ORTODONCIA_FIJA", false, NINGUNO, NINGUNO),   // 6.1.1
            new TipoDemo("APARATO_ORTODONTICO_REMOVIBLE", "Aparato ortodóntico removible", null, null, RANGO_PIEZAS, SimboloHallazgo.ZIGZAG_APICES, POR_ESTADO, "ORTODONCIA_REMOVIBLE", false, NINGUNO, NINGUNO),   // 6.1.2
            new TipoDemo("EDENTULO_TOTAL", "Edéntulo total", null, null, RANGO_PIEZAS, SimboloHallazgo.LINEA_CORONAS, SIEMPRE_AZUL, "EDENTULO", true, P, E));   // 6.1.7

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
            new ReglaDemo("TRATAMIENTO_CONDUCTOS", EstadoHallazgo.MALO, TipoRadicular.UNIRRADICULAR, 0, 5, "ENDO-U", 1, 40),
            new ReglaDemo("TRATAMIENTO_CONDUCTOS", EstadoHallazgo.MALO, TipoRadicular.BIRRADICULAR, 0, 5, "ENDO-B", 1, 41),
            new ReglaDemo("TRATAMIENTO_CONDUCTOS", EstadoHallazgo.MALO, TipoRadicular.MULTIRRADICULAR, 0, 5, "ENDO-M", 1, 42),
            new ReglaDemo("CORONA_TEMPORAL", null, null, 0, 5, "CORO-MC", 3, 50),
            new ReglaDemo("CORONA", EstadoHallazgo.MALO, null, 0, 5, "CORO-MC", 3, 51),
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
