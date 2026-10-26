package com.odontogest.seeder;

import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.modelo.CategoriaProcedimientoASEJ;
import com.odontogest.modelo.HallazgoTipoASEJ;
import com.odontogest.modelo.ProcedimientoASEJ;
import com.odontogest.modelo.ReglaSugerenciaASEJ;
import com.odontogest.persistencia.repositorio.HallazgoTipoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ProcedimientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ReglaSugerenciaRepositoryASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.C;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.E;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.NINGUNO;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.O;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.P;
import static com.odontogest.dominio.odontograma.ReglaColorASEJ.POR_ESTADO;
import static com.odontogest.dominio.odontograma.ReglaColorASEJ.SIEMPRE_AZUL;
import static com.odontogest.dominio.odontograma.ReglaColorASEJ.SIEMPRE_ROJO;
import static com.odontogest.dominio.odontograma.RepresentacionASEJ.ENTRE_PIEZAS;
import static com.odontogest.dominio.odontograma.RepresentacionASEJ.PIEZA;
import static com.odontogest.dominio.odontograma.RepresentacionASEJ.RANGO_PIEZAS;
import static com.odontogest.dominio.odontograma.RepresentacionASEJ.SUPERFICIE;
import static com.odontogest.dominio.tratamiento.FormaCobroASEJ.ARCADA;
import static com.odontogest.dominio.tratamiento.FormaCobroASEJ.BOCA;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.CIRUGIA;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.DIAGNOSTICO;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.ENDODONCIA;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.IMPLANTOLOGIA;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.OPERATORIA;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.PERIODONCIA;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.PREVENTIVO;
import static com.odontogest.modelo.CategoriaProcedimientoASEJ.PROTESIS;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class CatalogoSeederASEJ implements ApplicationRunner {

    private record TipoDemoASEJ(String codigoASEJ, String nombreASEJ, String siglaASEJ, String siglasOpcionesASEJ, RepresentacionASEJ representacionASEJ,
                            SimboloHallazgoASEJ simboloASEJ, ReglaColorASEJ colorASEJ, String capaASEJ, boolean ocultaASEJ,
                            ConteoIndiceASEJ cpodASEJ, ConteoIndiceASEJ ceodASEJ) {
    }

    private record ProcedimientoDemoASEJ(String codigoASEJ, String nombreASEJ, CategoriaProcedimientoASEJ categoriaASEJ, String precioASEJ,
                                     FormaCobroASEJ formaASEJ, int duracionASEJ) {
    }

    private record ReglaDemoASEJ(String hallazgoASEJ, EstadoHallazgoASEJ estadoASEJ, TipoRadicularASEJ radicularASEJ, int minASEJ, int maxASEJ,
                             String procedimientoASEJ, int faseASEJ, int prioridadASEJ) {
    }

    static final List<TipoDemoASEJ> TIPOSASEJ = List.of(
            new TipoDemoASEJ("CARIES", "Lesión de caries dental", null, "[\"MB\",\"CE\",\"CD\",\"CDP\"]", SUPERFICIE, SimboloHallazgoASEJ.RELLENO, SIEMPRE_ROJO, "SUPERFICIE", false, C, C),
            new TipoDemoASEJ("RESTAURACION_DEFINITIVA", "Restauración definitiva", null, "[\"AM\",\"R\",\"IV\",\"IM\",\"IE\",\"C\"]", SUPERFICIE, SimboloHallazgoASEJ.RELLENO, POR_ESTADO, "SUPERFICIE", false, O, O),
            new TipoDemoASEJ("RESTAURACION_TEMPORAL", "Restauración temporal", null, null, SUPERFICIE, SimboloHallazgoASEJ.CONTORNO_SUPERFICIE, SIEMPRE_ROJO, "SUPERFICIE", false, O, O),
            new TipoDemoASEJ("SELLANTE", "Sellante", "S", null, SUPERFICIE, SimboloHallazgoASEJ.TRAZO_FOSAS, POR_ESTADO, "SUPERFICIE_SELLANTE", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("SUPERFICIE_DESGASTADA", "Superficie desgastada", "DES", null, SUPERFICIE, SimboloHallazgoASEJ.LINEA_DESGASTE, SIEMPRE_ROJO, "DESGASTE", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("FOSAS_FISURAS_PROFUNDAS", "Fosas y fisuras profundas", "FFP", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_AZUL, "FOSAS_FISURAS", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("DEFECTO_ESMALTE", "Defecto de desarrollo del esmalte (DDE)", null, "[\"O\",\"PE\"]", PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_ROJO, "ESMALTE", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_AUSENTE_DEX", "Pieza ausente por caries (DEX)", "DEX", null, PIEZA, SimboloHallazgoASEJ.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, P, E),
            new TipoDemoASEJ("PIEZA_AUSENTE_DNE", "Pieza no erupcionada (DNE)", "DNE", null, PIEZA, SimboloHallazgoASEJ.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_AUSENTE_DAO", "Pieza ausente por otras razones (DAO)", "DAO", null, PIEZA, SimboloHallazgoASEJ.ASPA, SIEMPRE_AZUL, "ESTADO_PIEZA", true, NINGUNO, NINGUNO),
            new TipoDemoASEJ("REMANENTE_RADICULAR", "Remanente radicular", "RR", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_ROJO, "ESTADO_PIEZA", false, C, E),
            new TipoDemoASEJ("IMPLANTE", "Implante dental", "IMP", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, POR_ESTADO, "ESTADO_PIEZA", false, P, NINGUNO),
            new TipoDemoASEJ("PIEZA_EN_ERUPCION", "Pieza dentaria en erupción", null, null, PIEZA, SimboloHallazgoASEJ.FLECHA_ZIGZAG, SIEMPRE_AZUL, "ESTADO_PIEZA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("CORONA", "Corona", null, "[\"CM\",\"CF\",\"CMC\",\"CV\",\"CLM\"]", PIEZA, SimboloHallazgoASEJ.CUADRADO_CORONA, POR_ESTADO, "CORONA", false, O, O),
            new TipoDemoASEJ("CORONA_TEMPORAL", "Corona temporal", "CT", null, PIEZA, SimboloHallazgoASEJ.CUADRADO_CORONA, SIEMPRE_ROJO, "CORONA", false, O, O),
            new TipoDemoASEJ("ESPIGO_MUNON", "Espigo-muñón", null, null, PIEZA, SimboloHallazgoASEJ.ESPIGO_MUNON, POR_ESTADO, "ESPIGO", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("TRATAMIENTO_CONDUCTOS", "Tratamiento de conductos", null, "[\"TC\",\"PC\"]", PIEZA, SimboloHallazgoASEJ.LINEA_RAIZ, POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PULPOTOMIA", "Pulpotomía", "PP", null, PIEZA, SimboloHallazgoASEJ.PULPA_CORONAL, POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("FRACTURA", "Fractura dental", null, null, PIEZA, SimboloHallazgoASEJ.LINEA_FRACTURA, SIEMPRE_ROJO, "FRACTURA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("MOVILIDAD", "Movilidad patológica", null, "[\"M1\",\"M2\",\"M3\"]", PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_ROJO, "MOVILIDAD", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_EXTRUIDA", "Pieza dentaria extruida", null, null, PIEZA, SimboloHallazgoASEJ.FLECHA_EXTRUSION, SIEMPRE_AZUL, "POSICION_VERTICAL", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_INTRUIDA", "Pieza dentaria intruida", null, null, PIEZA, SimboloHallazgoASEJ.FLECHA_INTRUSION, SIEMPRE_AZUL, "POSICION_VERTICAL", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("GIROVERSION", "Giroversión", null, null, PIEZA, SimboloHallazgoASEJ.FLECHA_GIRO, SIEMPRE_AZUL, "GIRO", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("POSICION_ANORMAL", "Posición anormal dentaria", null, "[\"M\",\"D\",\"V\",\"P\",\"L\"]", PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_AZUL, "POSICION", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_ECTOPICA", "Pieza dentaria ectópica", "E", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_AZUL, "ECTOPICA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("IMPACTACION", "Impactación", "I", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_AZUL, "IMPACTACION", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("MACRODONCIA", "Macrodoncia", "MAC", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_AZUL, "TAMANO", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("MICRODONCIA", "Microdoncia", "MIC", null, PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA, SIEMPRE_AZUL, "TAMANO", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_CLAVIJA", "Pieza dentaria en clavija", null, null, PIEZA, SimboloHallazgoASEJ.TRIANGULO_RAIZ, SIEMPRE_AZUL, "CLAVIJA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("GEMINACION", "Geminación", null, null, PIEZA, SimboloHallazgoASEJ.CIRCULO_NUMERO, SIEMPRE_AZUL, "GEMINACION", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("DIASTEMA", "Diastema", null, null, ENTRE_PIEZAS, SimboloHallazgoASEJ.PARENTESIS, SIEMPRE_AZUL, "DIASTEMA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("FUSION", "Fusión", null, null, ENTRE_PIEZAS, SimboloHallazgoASEJ.CIRCULOS_NUMEROS, SIEMPRE_AZUL, "FUSION", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("TRANSPOSICION", "Transposición dentaria", null, null, ENTRE_PIEZAS, SimboloHallazgoASEJ.FLECHAS_CRUZADAS, SIEMPRE_AZUL, "TRANSPOSICION", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PIEZA_SUPERNUMERARIA", "Pieza dentaria supernumeraria", null, null, ENTRE_PIEZAS, SimboloHallazgoASEJ.CIRCULO_S, SIEMPRE_AZUL, "SUPERNUMERARIA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PROTESIS_FIJA", "Prótesis dental parcial fija", null, null, RANGO_PIEZAS, SimboloHallazgoASEJ.PUENTE, POR_ESTADO, "PROTESIS_FIJA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PROTESIS_REMOVIBLE", "Prótesis dental parcial removible", null, null, RANGO_PIEZAS, SimboloHallazgoASEJ.LINEAS_PARALELAS, POR_ESTADO, "PROTESIS_REMOVIBLE", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("PROTESIS_COMPLETA", "Prótesis dental completa", null, null, RANGO_PIEZAS, SimboloHallazgoASEJ.LINEAS_PARALELAS, POR_ESTADO, "PROTESIS_COMPLETA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("APARATO_ORTODONTICO_FIJO", "Aparato ortodóntico fijo", null, null, RANGO_PIEZAS, SimboloHallazgoASEJ.CUADROS_APICES, POR_ESTADO, "ORTODONCIA_FIJA", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("APARATO_ORTODONTICO_REMOVIBLE", "Aparato ortodóntico removible", null, null, RANGO_PIEZAS, SimboloHallazgoASEJ.ZIGZAG_APICES, POR_ESTADO, "ORTODONCIA_REMOVIBLE", false, NINGUNO, NINGUNO),
            new TipoDemoASEJ("EDENTULO_TOTAL", "Edéntulo total", null, null, RANGO_PIEZAS, SimboloHallazgoASEJ.LINEA_CORONAS, SIEMPRE_AZUL, "EDENTULO", true, P, E));

    static final List<ProcedimientoDemoASEJ> PROCEDIMIENTOSASEJ = List.of(
            new ProcedimientoDemoASEJ("CONS", "Consulta y diagnóstico", DIAGNOSTICO, "50.00", BOCA, 30),
            new ProcedimientoDemoASEJ("RX-PA", "Radiografía periapical", DIAGNOSTICO, "25.00", FormaCobroASEJ.PIEZA, 15),
            new ProcedimientoDemoASEJ("PROF", "Profilaxis", PREVENTIVO, "80.00", BOCA, 45),
            new ProcedimientoDemoASEJ("DEST", "Destartraje", PERIODONCIA, "120.00", BOCA, 60),
            new ProcedimientoDemoASEJ("FLUOR", "Fluorización", PREVENTIVO, "60.00", BOCA, 20),
            new ProcedimientoDemoASEJ("SELL", "Sellante", PREVENTIVO, "50.00", FormaCobroASEJ.PIEZA, 20),
            new ProcedimientoDemoASEJ("RES-S", "Resina simple (1 superficie)", OPERATORIA, "120.00", FormaCobroASEJ.PIEZA, 30),
            new ProcedimientoDemoASEJ("RES-C", "Resina compuesta (2 o más superficies)", OPERATORIA, "180.00", FormaCobroASEJ.PIEZA, 45),
            new ProcedimientoDemoASEJ("IONO-T", "Ionómero temporal", OPERATORIA, "70.00", FormaCobroASEJ.PIEZA, 20),
            new ProcedimientoDemoASEJ("PULPO", "Pulpotomía", ENDODONCIA, "150.00", FormaCobroASEJ.PIEZA, 45),
            new ProcedimientoDemoASEJ("ENDO-U", "Endodoncia unirradicular", ENDODONCIA, "350.00", FormaCobroASEJ.PIEZA, 60),
            new ProcedimientoDemoASEJ("ENDO-B", "Endodoncia birradicular", ENDODONCIA, "450.00", FormaCobroASEJ.PIEZA, 90),
            new ProcedimientoDemoASEJ("ENDO-M", "Endodoncia multirradicular", ENDODONCIA, "600.00", FormaCobroASEJ.PIEZA, 120),
            new ProcedimientoDemoASEJ("EXO", "Exodoncia simple", CIRUGIA, "100.00", FormaCobroASEJ.PIEZA, 30),
            new ProcedimientoDemoASEJ("EXO-RR", "Remoción de remanente radicular", CIRUGIA, "120.00", FormaCobroASEJ.PIEZA, 45),
            new ProcedimientoDemoASEJ("CORO-T", "Corona temporal", PROTESIS, "150.00", FormaCobroASEJ.PIEZA, 45),
            new ProcedimientoDemoASEJ("CORO-MC", "Corona metal-cerámica", PROTESIS, "900.00", FormaCobroASEJ.PIEZA, 60),
            new ProcedimientoDemoASEJ("PF-PZ", "Prótesis fija por pieza", PROTESIS, "800.00", FormaCobroASEJ.PIEZA, 60),
            new ProcedimientoDemoASEJ("IMPL", "Implante", IMPLANTOLOGIA, "3500.00", FormaCobroASEJ.PIEZA, 90),
            new ProcedimientoDemoASEJ("FERU", "Férula", PROTESIS, "400.00", ARCADA, 30));

    static final List<ReglaDemoASEJ> REGLASASEJ = List.of(
            new ReglaDemoASEJ(null, null, null, 0, 5, "CONS", 1, 1),
            new ReglaDemoASEJ(null, null, null, 0, 5, "PROF", 4, 2),
            new ReglaDemoASEJ("REMANENTE_RADICULAR", null, null, 0, 5, "EXO-RR", 1, 10),
            new ReglaDemoASEJ("FRACTURA", null, null, 0, 5, "RES-C", 1, 20),
            new ReglaDemoASEJ("CARIES", null, null, 1, 1, "RES-S", 2, 30),
            new ReglaDemoASEJ("CARIES", null, null, 2, 5, "RES-C", 2, 31),
            new ReglaDemoASEJ("RESTAURACION_TEMPORAL", null, null, 1, 1, "RES-S", 2, 32),
            new ReglaDemoASEJ("RESTAURACION_TEMPORAL", null, null, 2, 5, "RES-C", 2, 33),
            new ReglaDemoASEJ("RESTAURACION_DEFINITIVA", EstadoHallazgoASEJ.MALO, null, 1, 1, "RES-S", 2, 34),
            new ReglaDemoASEJ("RESTAURACION_DEFINITIVA", EstadoHallazgoASEJ.MALO, null, 2, 5, "RES-C", 2, 35),
            new ReglaDemoASEJ("TRATAMIENTO_CONDUCTOS", EstadoHallazgoASEJ.MALO, TipoRadicularASEJ.UNIRRADICULAR, 0, 5, "ENDO-U", 1, 40),
            new ReglaDemoASEJ("TRATAMIENTO_CONDUCTOS", EstadoHallazgoASEJ.MALO, TipoRadicularASEJ.BIRRADICULAR, 0, 5, "ENDO-B", 1, 41),
            new ReglaDemoASEJ("TRATAMIENTO_CONDUCTOS", EstadoHallazgoASEJ.MALO, TipoRadicularASEJ.MULTIRRADICULAR, 0, 5, "ENDO-M", 1, 42),
            new ReglaDemoASEJ("CORONA_TEMPORAL", null, null, 0, 5, "CORO-MC", 3, 50),
            new ReglaDemoASEJ("CORONA", EstadoHallazgoASEJ.MALO, null, 0, 5, "CORO-MC", 3, 51),
            new ReglaDemoASEJ("MOVILIDAD", null, null, 0, 5, "DEST", 2, 60));

    private final HallazgoTipoRepositoryASEJ hallazgoTipoRepositoryASEJ;
    private final ProcedimientoRepositoryASEJ procedimientoRepositoryASEJ;
    private final ReglaSugerenciaRepositoryASEJ reglaRepositoryASEJ;

    @Override
    @Transactional
    public void run(ApplicationArguments argsASEJ) {
        boolean primeraVezASEJ = hallazgoTipoRepositoryASEJ.count() == 0;
        int tiposASEJ = sembrarTiposASEJ();
        int procedimientosASEJ = sembrarProcedimientosASEJ();
        int reglasASEJ = primeraVezASEJ ? sembrarReglasASEJ() : 0;
        if (tiposASEJ + procedimientosASEJ + reglasASEJ > 0) {
            logASEJ.info("Catálogos iniciales: {} tipo(s) de hallazgo, {} procedimiento(s), {} regla(s)", tiposASEJ, procedimientosASEJ, reglasASEJ);
        }
    }

    private int sembrarTiposASEJ() {
        int insertadosASEJ = 0;
        for (int iASEJ = 0; iASEJ < TIPOSASEJ.size(); iASEJ++) {
            TipoDemoASEJ dASEJ = TIPOSASEJ.get(iASEJ);
            if (hallazgoTipoRepositoryASEJ.findByCodigoASEJIgnoreCase(dASEJ.codigoASEJ()).isPresent()) {
                continue;
            }
            HallazgoTipoASEJ tASEJ = new HallazgoTipoASEJ();
            tASEJ.setCodigoASEJ(dASEJ.codigoASEJ());
            tASEJ.setNombreASEJ(dASEJ.nombreASEJ());
            tASEJ.setSiglaASEJ(dASEJ.siglaASEJ());
            tASEJ.setSiglasOpcionesASEJ(dASEJ.siglasOpcionesASEJ());
            tASEJ.setRepresentacionASEJ(dASEJ.representacionASEJ());
            tASEJ.setSimboloASEJ(dASEJ.simboloASEJ());
            tASEJ.setReglaColorASEJ(dASEJ.colorASEJ());
            tASEJ.setGrupoExclusionASEJ(dASEJ.capaASEJ());
            tASEJ.setOcultaSuperficiesASEJ(dASEJ.ocultaASEJ());
            tASEJ.setConteoCpodASEJ(dASEJ.cpodASEJ());
            tASEJ.setConteoCeodASEJ(dASEJ.ceodASEJ());
            tASEJ.setActivoASEJ(true);
            tASEJ.setOrdenASEJ((iASEJ + 1) * 10);
            hallazgoTipoRepositoryASEJ.save(tASEJ);
            insertadosASEJ++;
        }
        return insertadosASEJ;
    }

    private int sembrarProcedimientosASEJ() {
        int insertadosASEJ = 0;
        for (ProcedimientoDemoASEJ dASEJ : PROCEDIMIENTOSASEJ) {
            if (procedimientoRepositoryASEJ.findByCodigoASEJIgnoreCase(dASEJ.codigoASEJ()).isPresent()) {
                continue;
            }
            ProcedimientoASEJ pASEJ = new ProcedimientoASEJ();
            pASEJ.setCodigoASEJ(dASEJ.codigoASEJ());
            pASEJ.setNombreASEJ(dASEJ.nombreASEJ());
            pASEJ.setCategoriaASEJ(dASEJ.categoriaASEJ());
            pASEJ.setPrecioASEJ(new BigDecimal(dASEJ.precioASEJ()));
            pASEJ.setFormaCobroASEJ(dASEJ.formaASEJ());
            pASEJ.setDuracionMinASEJ(dASEJ.duracionASEJ());
            pASEJ.setActivoASEJ(true);
            procedimientoRepositoryASEJ.save(pASEJ);
            insertadosASEJ++;
        }
        return insertadosASEJ;
    }

    private int sembrarReglasASEJ() {
        for (ReglaDemoASEJ dASEJ : REGLASASEJ) {
            ReglaSugerenciaASEJ rASEJ = new ReglaSugerenciaASEJ();
            rASEJ.setHallazgoTipoASEJ(dASEJ.hallazgoASEJ() == null ? null
                    : hallazgoTipoRepositoryASEJ.findByCodigoASEJIgnoreCase(dASEJ.hallazgoASEJ()).orElseThrow());
            rASEJ.setEstadoASEJ(dASEJ.estadoASEJ());
            rASEJ.setTipoRadicularASEJ(dASEJ.radicularASEJ());
            rASEJ.setMinSuperficiesASEJ(dASEJ.minASEJ());
            rASEJ.setMaxSuperficiesASEJ(dASEJ.maxASEJ());
            rASEJ.setProcedimientoASEJ(procedimientoRepositoryASEJ.findByCodigoASEJIgnoreCase(dASEJ.procedimientoASEJ()).orElseThrow());
            rASEJ.setFaseASEJ(dASEJ.faseASEJ());
            rASEJ.setPrioridadASEJ(dASEJ.prioridadASEJ());
            rASEJ.setActivoASEJ(true);
            reglaRepositoryASEJ.save(rASEJ);
        }
        return REGLASASEJ.size();
    }
}
