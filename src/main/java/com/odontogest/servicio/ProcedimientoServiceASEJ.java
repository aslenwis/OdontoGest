package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.ProcedimientoASEJ;
import com.odontogest.persistencia.repositorio.ProcedimientoRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ProcedimientoFormASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcedimientoServiceASEJ {

    private static final Pattern CODIGOASEJ = Pattern.compile("[A-Z0-9][A-Z0-9-]{1,11}");
    private static final BigDecimal PRECIO_MAXIMOASEJ = new BigDecimal("999999.99");

    private final ProcedimientoRepositoryASEJ procedimientoRepositoryASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<ProcedimientoDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        return procedimientoRepositoryASEJ.findAllByOrderByCategoriaASEJAscCodigoASEJAsc().stream()
                .filter(pASEJ -> incluirInactivosASEJ || pASEJ.isActivoASEJ())
                .map(ProcedimientoServiceASEJ::dtoASEJ)
                .toList();
    }

    @Transactional
    public long guardarASEJ(ProcedimientoFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CATALOGOS_EDITAR);
        String codigoASEJ = ValidadorASEJ.limpioASEJ(formASEJ.codigoASEJ()) == null ? null : formASEJ.codigoASEJ().trim().toUpperCase(Locale.ROOT);
        String nombreASEJ = ValidadorASEJ.limpioASEJ(formASEJ.nombreASEJ());
        BigDecimal precioASEJ = formASEJ.precioASEJ();
        new ValidadorASEJ()
                .requeridoASEJ(codigoASEJ, "el código")
                .siASEJ(codigoASEJ != null && !CODIGOASEJ.matcher(codigoASEJ).matches(),
                        "El código admite de 2 a 12 letras, dígitos o guiones (p. ej. RES-S).")
                .requeridoASEJ(nombreASEJ, "el nombre")
                .maximoASEJ(nombreASEJ, 120, "nombre")
                .requeridoASEJ(formASEJ.categoriaASEJ(), "la categoría")
                .requeridoASEJ(formASEJ.formaCobroASEJ(), "la forma de cobro")
                .requeridoASEJ(precioASEJ, "el precio")
                .siASEJ(precioASEJ != null && (precioASEJ.signum() < 0 || precioASEJ.compareTo(PRECIO_MAXIMOASEJ) > 0),
                        "El precio debe estar entre 0 y 999 999.99.")
                .siASEJ(precioASEJ != null && precioASEJ.stripTrailingZeros().scale() > 2, "El precio admite como máximo 2 decimales.")
                .siASEJ(formASEJ.duracionMinASEJ() < 5 || formASEJ.duracionMinASEJ() > 480, "La duración debe estar entre 5 y 480 minutos.")
                .lanzarSiHayErroresASEJ();

        procedimientoRepositoryASEJ.findByCodigoASEJIgnoreCase(codigoASEJ)
                .filter(pASEJ -> !pASEJ.getIdASEJ().equals(formASEJ.idASEJ()))
                .ifPresent(pASEJ -> {
                    throw new NegocioExceptionASEJ("Ya existe un procedimiento con el código " + codigoASEJ + ".");
                });

        ProcedimientoASEJ pASEJ = formASEJ.idASEJ() == null ? new ProcedimientoASEJ() : procedimientoRepositoryASEJ.findById(formASEJ.idASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El procedimiento ya no existe en la base de datos."));
        pASEJ.setCodigoASEJ(codigoASEJ);
        pASEJ.setNombreASEJ(nombreASEJ);
        pASEJ.setCategoriaASEJ(formASEJ.categoriaASEJ());
        pASEJ.setPrecioASEJ(precioASEJ);
        pASEJ.setFormaCobroASEJ(formASEJ.formaCobroASEJ());
        pASEJ.setDuracionMinASEJ(formASEJ.duracionMinASEJ());
        pASEJ.setActivoASEJ(formASEJ.activoASEJ());
        return procedimientoRepositoryASEJ.save(pASEJ).getIdASEJ();
    }

    static ProcedimientoDtoASEJ dtoASEJ(ProcedimientoASEJ pASEJ) {
        return new ProcedimientoDtoASEJ(pASEJ.getIdASEJ(), pASEJ.getCodigoASEJ(), pASEJ.getNombreASEJ(), pASEJ.getCategoriaASEJ(), pASEJ.getPrecioASEJ(),
                pASEJ.getFormaCobroASEJ(), pASEJ.getDuracionMinASEJ(), pASEJ.isActivoASEJ());
    }
}
