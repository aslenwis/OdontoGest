/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.TipoHallazgoDef;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.HallazgoTipo;
import com.odontogest.modelo.Procedimiento;
import com.odontogest.modelo.ReglaSugerencia;
import com.odontogest.persistencia.repositorio.HallazgoTipoRepository;
import com.odontogest.persistencia.repositorio.ProcedimientoRepository;
import com.odontogest.persistencia.repositorio.ReglaSugerenciaRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.HallazgoTipoForm;
import com.odontogest.servicio.dto.ReglaSugerenciaDto;
import com.odontogest.servicio.dto.ReglaSugerenciaForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Catálogo de hallazgos del odontograma y reglas de sugerencia de tratamiento (RF-CAT-02, RF-CAT-03).
 * <p>
 * Cuando un tipo ya tiene hallazgos registrados, su estructura (código, representación, capa, regla de
 * color, ocultar superficies y conteo en índices) queda fija: cambiarla alteraría odontogramas cerrados.
 * Siguen editables el nombre, la sigla, las opciones de sigla, el símbolo, el orden y el estado activo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoHallazgoService {

    private static final Pattern CODIGO = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");
    private static final Pattern SIGLA = Pattern.compile("[A-Z0-9]{1,4}");
    private static final Type LISTA = new TypeToken<List<String>>() { }.getType();
    private static final Gson GSON = new Gson();

    private final HallazgoTipoRepository hallazgoTipoRepository;
    private final ReglaSugerenciaRepository reglaRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final SesionUsuario sesionUsuario;

    // ------------------------------------------------------------------ tipos de hallazgo

    public List<HallazgoTipoDto> listarTipos(boolean incluirInactivos) {
        return hallazgoTipoRepository.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(t -> incluirInactivos || t.isActivo())
                .map(this::dto)
                .toList();
    }

    /** Definiciones activas para el dominio (proyección, índices y motor de sugerencias). */
    public List<TipoHallazgoDef> definiciones() {
        return hallazgoTipoRepository.findAllByOrderByOrdenAscNombreAsc().stream()
                .map(CatalogoHallazgoService::definicion)
                .toList();
    }

    /** Capas de exclusión ya usadas, para sugerirlas en el formulario. */
    public List<String> gruposExclusion() {
        return hallazgoTipoRepository.gruposExclusion();
    }

    @Transactional
    public long guardarTipo(HallazgoTipoForm form) {
        sesionUsuario.exigir(Permiso.CATALOGOS_EDITAR);
        String codigo = mayusculas(form.codigo());
        String grupo = mayusculas(form.grupoExclusion());
        String sigla = mayusculas(form.sigla());
        List<String> opciones = form.siglasOpciones().stream()
                .map(CatalogoHallazgoService::mayusculas).filter(Objects::nonNull).distinct().toList();

        Validador v = new Validador()
                .requerido(codigo, "el código")
                .si(codigo != null && !CODIGO.matcher(codigo).matches(),
                        "El código admite mayúsculas, dígitos y guion bajo (p. ej. PIEZA_AUSENTE).")
                .requerido(Validador.limpio(form.nombre()), "el nombre")
                .requerido(form.representacion(), "la representación")
                .requerido(form.simbolo(), "el símbolo")
                .requerido(form.reglaColor(), "la regla de color")
                .requerido(grupo, "la capa de exclusión")
                .si(grupo != null && !CODIGO.matcher(grupo).matches(), "La capa de exclusión admite mayúsculas, dígitos y guion bajo.")
                .si(sigla != null && !SIGLA.matcher(sigla).matches(), "La sigla admite de 1 a 4 letras o dígitos.")
                .si(opciones.stream().anyMatch(o -> !SIGLA.matcher(o).matches()), "Cada opción de sigla admite de 1 a 4 letras o dígitos.")
                .si(sigla != null && !opciones.isEmpty(), "Use una sigla fija o una lista de opciones, no ambas.")
                .si(form.conteoCpod() == ConteoIndice.E, "El CPO-D no tiene componente E (use P).")
                .si(form.conteoCeod() == ConteoIndice.P, "El ceo-d no tiene componente P (use E).");
        v.lanzarSiHayErrores();

        hallazgoTipoRepository.findByCodigoIgnoreCase(codigo)
                .filter(t -> !t.getId().equals(form.id()))
                .ifPresent(t -> {
                    throw new NegocioException("Ya existe un tipo de hallazgo con el código " + codigo + ".");
                });

        HallazgoTipo t = form.id() == null ? new HallazgoTipo() : buscarTipo(form.id());
        if (t.getId() != null && hallazgoTipoRepository.contarHallazgos(t.getId()) > 0) {
            boolean cambiaEstructura = !t.getCodigo().equals(codigo)
                    || t.getRepresentacion() != form.representacion()
                    || !t.getGrupoExclusion().equals(grupo)
                    || t.getReglaColor() != form.reglaColor()
                    || t.isOcultaSuperficies() != form.ocultaSuperficies()
                    || t.getConteoCpod() != conteo(form.conteoCpod())
                    || t.getConteoCeod() != conteo(form.conteoCeod());
            if (cambiaEstructura) {
                throw new NegocioException("El tipo " + t.getCodigo() + " ya tiene hallazgos registrados: solo se puede "
                        + "cambiar el nombre, la sigla, las opciones de sigla, el símbolo, el orden y si está activo.");
            }
        }
        t.setCodigo(codigo);
        t.setNombre(Validador.limpio(form.nombre()));
        t.setSigla(sigla);
        t.setSiglasOpciones(opciones.isEmpty() ? null : GSON.toJson(opciones));
        t.setRepresentacion(form.representacion());
        t.setSimbolo(form.simbolo());
        t.setReglaColor(form.reglaColor());
        t.setGrupoExclusion(grupo);
        t.setOcultaSuperficies(form.ocultaSuperficies());
        t.setConteoCpod(conteo(form.conteoCpod()));
        t.setConteoCeod(conteo(form.conteoCeod()));
        t.setActivo(form.activo());
        t.setOrden(form.orden());
        return hallazgoTipoRepository.save(t).getId();
    }

    // ------------------------------------------------------------------ reglas de sugerencia

    public List<ReglaSugerenciaDto> listarReglas() {
        return reglaRepository.listarConRelaciones().stream().map(CatalogoHallazgoService::dto).toList();
    }

    @Transactional
    public long guardarRegla(ReglaSugerenciaForm form) {
        sesionUsuario.exigir(Permiso.CATALOGOS_EDITAR);
        HallazgoTipo tipo = form.hallazgoTipoId() == null ? null : buscarTipo(form.hallazgoTipoId());
        new Validador()
                .requerido(form.procedimientoId(), "el procedimiento")
                .si(form.minSuperficies() < 0 || form.maxSuperficies() > 5 || form.minSuperficies() > form.maxSuperficies(),
                        "El rango de superficies debe estar entre 0 y 5 (mínimo ≤ máximo).")
                .si(form.fase() < 1 || form.fase() > 4, "La fase debe estar entre 1 y 4.")
                .si(form.prioridad() < 0, "La prioridad no puede ser negativa.")
                .si(tipo == null && (form.estado() != null || form.tipoRadicular() != null),
                        "Una regla general (sin hallazgo) no puede filtrar por estado ni por tipo radicular.")
                .si(tipo != null && form.estado() != null && tipo.getReglaColor() != ReglaColor.POR_ESTADO,
                        "El hallazgo " + (tipo == null ? "" : tipo.getNombre()) + " no usa estado bueno/malo.")
                .lanzarSiHayErrores();
        Procedimiento procedimiento = procedimientoRepository.findById(form.procedimientoId())
                .orElseThrow(() -> new NegocioException("El procedimiento ya no existe en la base de datos."));

        ReglaSugerencia r = form.id() == null ? new ReglaSugerencia() : reglaRepository.findById(form.id())
                .orElseThrow(() -> new NegocioException("La regla ya no existe en la base de datos."));
        r.setHallazgoTipo(tipo);
        r.setEstado(form.estado());
        r.setTipoRadicular(form.tipoRadicular());
        r.setMinSuperficies(form.minSuperficies());
        r.setMaxSuperficies(form.maxSuperficies());
        r.setProcedimiento(procedimiento);
        r.setFase(form.fase());
        r.setPrioridad(form.prioridad());
        r.setActivo(form.activo());
        return reglaRepository.save(r).getId();
    }

    @Transactional
    public void eliminarRegla(long id) {
        sesionUsuario.exigir(Permiso.CATALOGOS_EDITAR);
        if (!reglaRepository.existsById(id)) {
            throw new NegocioException("La regla ya no existe en la base de datos.");
        }
        reglaRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ apoyo

    private HallazgoTipo buscarTipo(long id) {
        return hallazgoTipoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El tipo de hallazgo ya no existe en la base de datos."));
    }

    private static ConteoIndice conteo(ConteoIndice c) {
        return c == null ? ConteoIndice.NINGUNO : c;
    }

    private static String mayusculas(String valor) {
        String limpio = Validador.limpio(valor);
        return limpio == null ? null : limpio.toUpperCase(Locale.ROOT);
    }

    static List<String> opciones(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<String> lista = GSON.fromJson(json, LISTA);
        return lista == null ? List.of() : List.copyOf(lista);
    }

    private HallazgoTipoDto dto(HallazgoTipo t) {
        return new HallazgoTipoDto(t.getId(), t.getCodigo(), t.getNombre(), t.getSigla(), opciones(t.getSiglasOpciones()),
                t.getRepresentacion(), t.getSimbolo(), t.getReglaColor(), t.getGrupoExclusion(), t.isOcultaSuperficies(),
                t.getConteoCpod(), t.getConteoCeod(), t.isActivo(), t.getOrden(),
                hallazgoTipoRepository.contarHallazgos(t.getId()) > 0);
    }

    private static TipoHallazgoDef definicion(HallazgoTipo t) {
        return new TipoHallazgoDef(t.getId(), t.getCodigo(), t.getNombre(), t.getSigla(), opciones(t.getSiglasOpciones()),
                t.getRepresentacion(), t.getSimbolo(), t.getReglaColor(), t.getGrupoExclusion(), t.isOcultaSuperficies(),
                t.getConteoCpod(), t.getConteoCeod());
    }

    private static ReglaSugerenciaDto dto(ReglaSugerencia r) {
        HallazgoTipo t = r.getHallazgoTipo();
        return new ReglaSugerenciaDto(r.getId(), t == null ? null : t.getId(), t == null ? "(general)" : t.getNombre(),
                r.getEstado(), r.getTipoRadicular(), r.getMinSuperficies(), r.getMaxSuperficies(),
                r.getProcedimiento().getId(), r.getProcedimiento().getCodigo() + " · " + r.getProcedimiento().getNombre(),
                r.getFase(), r.getPrioridad(), r.isActivo());
    }
}
