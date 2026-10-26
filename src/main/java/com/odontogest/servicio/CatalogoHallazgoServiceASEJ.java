package com.odontogest.servicio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.TipoHallazgoDefASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.HallazgoTipoASEJ;
import com.odontogest.modelo.ProcedimientoASEJ;
import com.odontogest.modelo.ReglaSugerenciaASEJ;
import com.odontogest.persistencia.repositorio.HallazgoTipoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ProcedimientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ReglaSugerenciaRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoFormASEJ;
import com.odontogest.servicio.dto.ReglaSugerenciaDtoASEJ;
import com.odontogest.servicio.dto.ReglaSugerenciaFormASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoHallazgoServiceASEJ {

    private static final Pattern CODIGOASEJ = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");
    private static final Pattern SIGLAASEJ = Pattern.compile("[A-Z0-9]{1,4}");
    private static final Type LISTAASEJ = new TypeToken<List<String>>() { }.getType();
    private static final Gson GSONASEJ = new Gson();

    private final HallazgoTipoRepositoryASEJ hallazgoTipoRepositoryASEJ;
    private final ReglaSugerenciaRepositoryASEJ reglaRepositoryASEJ;
    private final ProcedimientoRepositoryASEJ procedimientoRepositoryASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<HallazgoTipoDtoASEJ> listarTiposASEJ(boolean incluirInactivosASEJ) {
        return hallazgoTipoRepositoryASEJ.findAllByOrderByOrdenASEJAscNombreASEJAsc().stream()
                .filter(tASEJ -> incluirInactivosASEJ || tASEJ.isActivoASEJ())
                .map(this::dtoASEJ)
                .toList();
    }

    public List<TipoHallazgoDefASEJ> definicionesASEJ() {
        return hallazgoTipoRepositoryASEJ.findAllByOrderByOrdenASEJAscNombreASEJAsc().stream()
                .map(CatalogoHallazgoServiceASEJ::definicionASEJ)
                .toList();
    }

    public List<String> gruposExclusionASEJ() {
        return hallazgoTipoRepositoryASEJ.gruposExclusionASEJ();
    }

    @Transactional
    public long guardarTipoASEJ(HallazgoTipoFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CATALOGOS_EDITAR);
        String codigoASEJ = mayusculasASEJ(formASEJ.codigoASEJ());
        String grupoASEJ = mayusculasASEJ(formASEJ.grupoExclusionASEJ());
        String siglaASEJ = mayusculasASEJ(formASEJ.siglaASEJ());
        List<String> opcionesASEJ = formASEJ.siglasOpcionesASEJ().stream()
                .map(CatalogoHallazgoServiceASEJ::mayusculasASEJ).filter(Objects::nonNull).distinct().toList();

        ValidadorASEJ vASEJ = new ValidadorASEJ()
                .requeridoASEJ(codigoASEJ, "el código")
                .siASEJ(codigoASEJ != null && !CODIGOASEJ.matcher(codigoASEJ).matches(),
                        "El código admite mayúsculas, dígitos y guion bajo (p. ej. PIEZA_AUSENTE).")
                .requeridoASEJ(ValidadorASEJ.limpioASEJ(formASEJ.nombreASEJ()), "el nombre")
                .requeridoASEJ(formASEJ.representacionASEJ(), "la representación")
                .requeridoASEJ(formASEJ.simboloASEJ(), "el símbolo")
                .requeridoASEJ(formASEJ.reglaColorASEJ(), "la regla de color")
                .requeridoASEJ(grupoASEJ, "la capa de exclusión")
                .siASEJ(grupoASEJ != null && !CODIGOASEJ.matcher(grupoASEJ).matches(), "La capa de exclusión admite mayúsculas, dígitos y guion bajo.")
                .siASEJ(siglaASEJ != null && !SIGLAASEJ.matcher(siglaASEJ).matches(), "La sigla admite de 1 a 4 letras o dígitos.")
                .siASEJ(opcionesASEJ.stream().anyMatch(oASEJ -> !SIGLAASEJ.matcher(oASEJ).matches()), "Cada opción de sigla admite de 1 a 4 letras o dígitos.")
                .siASEJ(siglaASEJ != null && !opcionesASEJ.isEmpty(), "Use una sigla fija o una lista de opciones, no ambas.")
                .siASEJ(formASEJ.conteoCpodASEJ() == ConteoIndiceASEJ.E, "El CPO-D no tiene componente E (use P).")
                .siASEJ(formASEJ.conteoCeodASEJ() == ConteoIndiceASEJ.P, "El ceo-d no tiene componente P (use E).");
        vASEJ.lanzarSiHayErroresASEJ();

        hallazgoTipoRepositoryASEJ.findByCodigoASEJIgnoreCase(codigoASEJ)
                .filter(tASEJ -> !tASEJ.getIdASEJ().equals(formASEJ.idASEJ()))
                .ifPresent(tASEJ -> {
                    throw new NegocioExceptionASEJ("Ya existe un tipo de hallazgo con el código " + codigoASEJ + ".");
                });

        HallazgoTipoASEJ tASEJ = formASEJ.idASEJ() == null ? new HallazgoTipoASEJ() : buscarTipoASEJ(formASEJ.idASEJ());
        if (tASEJ.getIdASEJ() != null && hallazgoTipoRepositoryASEJ.contarHallazgosASEJ(tASEJ.getIdASEJ()) > 0) {
            boolean cambiaEstructuraASEJ = !tASEJ.getCodigoASEJ().equals(codigoASEJ)
                    || tASEJ.getRepresentacionASEJ() != formASEJ.representacionASEJ()
                    || !tASEJ.getGrupoExclusionASEJ().equals(grupoASEJ)
                    || tASEJ.getReglaColorASEJ() != formASEJ.reglaColorASEJ()
                    || tASEJ.isOcultaSuperficiesASEJ() != formASEJ.ocultaSuperficiesASEJ()
                    || tASEJ.getConteoCpodASEJ() != conteoASEJ(formASEJ.conteoCpodASEJ())
                    || tASEJ.getConteoCeodASEJ() != conteoASEJ(formASEJ.conteoCeodASEJ());
            if (cambiaEstructuraASEJ) {
                throw new NegocioExceptionASEJ("El tipo " + tASEJ.getCodigoASEJ() + " ya tiene hallazgos registrados: solo se puede "
                        + "cambiar el nombre, la sigla, las opciones de sigla, el símbolo, el orden y si está activo.");
            }
        }
        tASEJ.setCodigoASEJ(codigoASEJ);
        tASEJ.setNombreASEJ(ValidadorASEJ.limpioASEJ(formASEJ.nombreASEJ()));
        tASEJ.setSiglaASEJ(siglaASEJ);
        tASEJ.setSiglasOpcionesASEJ(opcionesASEJ.isEmpty() ? null : GSONASEJ.toJson(opcionesASEJ));
        tASEJ.setRepresentacionASEJ(formASEJ.representacionASEJ());
        tASEJ.setSimboloASEJ(formASEJ.simboloASEJ());
        tASEJ.setReglaColorASEJ(formASEJ.reglaColorASEJ());
        tASEJ.setGrupoExclusionASEJ(grupoASEJ);
        tASEJ.setOcultaSuperficiesASEJ(formASEJ.ocultaSuperficiesASEJ());
        tASEJ.setConteoCpodASEJ(conteoASEJ(formASEJ.conteoCpodASEJ()));
        tASEJ.setConteoCeodASEJ(conteoASEJ(formASEJ.conteoCeodASEJ()));
        tASEJ.setActivoASEJ(formASEJ.activoASEJ());
        tASEJ.setOrdenASEJ(formASEJ.ordenASEJ());
        return hallazgoTipoRepositoryASEJ.save(tASEJ).getIdASEJ();
    }

    public List<ReglaSugerenciaDtoASEJ> listarReglasASEJ() {
        return reglaRepositoryASEJ.listarConRelacionesASEJ().stream().map(CatalogoHallazgoServiceASEJ::dtoASEJ).toList();
    }

    @Transactional
    public long guardarReglaASEJ(ReglaSugerenciaFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CATALOGOS_EDITAR);
        HallazgoTipoASEJ tipoASEJ = formASEJ.hallazgoTipoIdASEJ() == null ? null : buscarTipoASEJ(formASEJ.hallazgoTipoIdASEJ());
        new ValidadorASEJ()
                .requeridoASEJ(formASEJ.procedimientoIdASEJ(), "el procedimiento")
                .siASEJ(formASEJ.minSuperficiesASEJ() < 0 || formASEJ.maxSuperficiesASEJ() > 5 || formASEJ.minSuperficiesASEJ() > formASEJ.maxSuperficiesASEJ(),
                        "El rango de superficies debe estar entre 0 y 5 (mínimo ≤ máximo).")
                .siASEJ(formASEJ.faseASEJ() < 1 || formASEJ.faseASEJ() > 4, "La fase debe estar entre 1 y 4.")
                .siASEJ(formASEJ.prioridadASEJ() < 0, "La prioridad no puede ser negativa.")
                .siASEJ(tipoASEJ == null && (formASEJ.estadoASEJ() != null || formASEJ.tipoRadicularASEJ() != null),
                        "Una regla general (sin hallazgo) no puede filtrar por estado ni por tipo radicular.")
                .siASEJ(tipoASEJ != null && formASEJ.estadoASEJ() != null && tipoASEJ.getReglaColorASEJ() != ReglaColorASEJ.POR_ESTADO,
                        "El hallazgo " + (tipoASEJ == null ? "" : tipoASEJ.getNombreASEJ()) + " no usa estado bueno/malo.")
                .lanzarSiHayErroresASEJ();
        ProcedimientoASEJ procedimientoASEJ = procedimientoRepositoryASEJ.findById(formASEJ.procedimientoIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El procedimiento ya no existe en la base de datos."));

        ReglaSugerenciaASEJ rASEJ = formASEJ.idASEJ() == null ? new ReglaSugerenciaASEJ() : reglaRepositoryASEJ.findById(formASEJ.idASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("La regla ya no existe en la base de datos."));
        rASEJ.setHallazgoTipoASEJ(tipoASEJ);
        rASEJ.setEstadoASEJ(formASEJ.estadoASEJ());
        rASEJ.setTipoRadicularASEJ(formASEJ.tipoRadicularASEJ());
        rASEJ.setMinSuperficiesASEJ(formASEJ.minSuperficiesASEJ());
        rASEJ.setMaxSuperficiesASEJ(formASEJ.maxSuperficiesASEJ());
        rASEJ.setProcedimientoASEJ(procedimientoASEJ);
        rASEJ.setFaseASEJ(formASEJ.faseASEJ());
        rASEJ.setPrioridadASEJ(formASEJ.prioridadASEJ());
        rASEJ.setActivoASEJ(formASEJ.activoASEJ());
        return reglaRepositoryASEJ.save(rASEJ).getIdASEJ();
    }

    @Transactional
    public void eliminarReglaASEJ(long idASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CATALOGOS_EDITAR);
        if (!reglaRepositoryASEJ.existsById(idASEJ)) {
            throw new NegocioExceptionASEJ("La regla ya no existe en la base de datos.");
        }
        reglaRepositoryASEJ.deleteById(idASEJ);
    }

    private HallazgoTipoASEJ buscarTipoASEJ(long idASEJ) {
        return hallazgoTipoRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El tipo de hallazgo ya no existe en la base de datos."));
    }

    private static ConteoIndiceASEJ conteoASEJ(ConteoIndiceASEJ cASEJ) {
        return cASEJ == null ? ConteoIndiceASEJ.NINGUNO : cASEJ;
    }

    private static String mayusculasASEJ(String valorASEJ) {
        String limpioASEJ = ValidadorASEJ.limpioASEJ(valorASEJ);
        return limpioASEJ == null ? null : limpioASEJ.toUpperCase(Locale.ROOT);
    }

    static List<String> opcionesASEJ(String jsonASEJ) {
        if (jsonASEJ == null || jsonASEJ.isBlank()) {
            return List.of();
        }
        List<String> listaASEJ = GSONASEJ.fromJson(jsonASEJ, LISTAASEJ);
        return listaASEJ == null ? List.of() : List.copyOf(listaASEJ);
    }

    private HallazgoTipoDtoASEJ dtoASEJ(HallazgoTipoASEJ tASEJ) {
        return new HallazgoTipoDtoASEJ(tASEJ.getIdASEJ(), tASEJ.getCodigoASEJ(), tASEJ.getNombreASEJ(), tASEJ.getSiglaASEJ(), opcionesASEJ(tASEJ.getSiglasOpcionesASEJ()),
                tASEJ.getRepresentacionASEJ(), tASEJ.getSimboloASEJ(), tASEJ.getReglaColorASEJ(), tASEJ.getGrupoExclusionASEJ(), tASEJ.isOcultaSuperficiesASEJ(),
                tASEJ.getConteoCpodASEJ(), tASEJ.getConteoCeodASEJ(), tASEJ.isActivoASEJ(), tASEJ.getOrdenASEJ(),
                hallazgoTipoRepositoryASEJ.contarHallazgosASEJ(tASEJ.getIdASEJ()) > 0);
    }

    static TipoHallazgoDefASEJ definicionASEJ(HallazgoTipoASEJ tASEJ) {
        return new TipoHallazgoDefASEJ(tASEJ.getIdASEJ(), tASEJ.getCodigoASEJ(), tASEJ.getNombreASEJ(), tASEJ.getSiglaASEJ(), opcionesASEJ(tASEJ.getSiglasOpcionesASEJ()),
                tASEJ.getRepresentacionASEJ(), tASEJ.getSimboloASEJ(), tASEJ.getReglaColorASEJ(), tASEJ.getGrupoExclusionASEJ(), tASEJ.isOcultaSuperficiesASEJ(),
                tASEJ.getConteoCpodASEJ(), tASEJ.getConteoCeodASEJ());
    }

    private static ReglaSugerenciaDtoASEJ dtoASEJ(ReglaSugerenciaASEJ rASEJ) {
        HallazgoTipoASEJ tASEJ = rASEJ.getHallazgoTipoASEJ();
        return new ReglaSugerenciaDtoASEJ(rASEJ.getIdASEJ(), tASEJ == null ? null : tASEJ.getIdASEJ(), tASEJ == null ? "(general)" : tASEJ.getNombreASEJ(),
                rASEJ.getEstadoASEJ(), rASEJ.getTipoRadicularASEJ(), rASEJ.getMinSuperficiesASEJ(), rASEJ.getMaxSuperficiesASEJ(),
                rASEJ.getProcedimientoASEJ().getIdASEJ(), rASEJ.getProcedimientoASEJ().getCodigoASEJ() + " · " + rASEJ.getProcedimientoASEJ().getNombreASEJ(),
                rASEJ.getFaseASEJ(), rASEJ.getPrioridadASEJ(), rASEJ.isActivoASEJ());
    }
}
