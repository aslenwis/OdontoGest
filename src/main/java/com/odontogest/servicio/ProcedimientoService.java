/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Procedimiento;
import com.odontogest.persistencia.repositorio.ProcedimientoRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ProcedimientoForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tarifario de procedimientos (RF-CAT-01). Los procedimientos no se eliminan (los usan planes y reglas):
 * se desactivan.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcedimientoService {

    private static final Pattern CODIGO = Pattern.compile("[A-Z0-9][A-Z0-9-]{1,11}");
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("999999.99");

    private final ProcedimientoRepository procedimientoRepository;
    private final SesionUsuario sesionUsuario;

    public List<ProcedimientoDto> listar(boolean incluirInactivos) {
        return procedimientoRepository.findAllByOrderByCategoriaAscCodigoAsc().stream()
                .filter(p -> incluirInactivos || p.isActivo())
                .map(ProcedimientoService::dto)
                .toList();
    }

    @Transactional
    public long guardar(ProcedimientoForm form) {
        sesionUsuario.exigir(Permiso.CATALOGOS_EDITAR);
        String codigo = Validador.limpio(form.codigo()) == null ? null : form.codigo().trim().toUpperCase(Locale.ROOT);
        String nombre = Validador.limpio(form.nombre());
        BigDecimal precio = form.precio();
        new Validador()
                .requerido(codigo, "el código")
                .si(codigo != null && !CODIGO.matcher(codigo).matches(),
                        "El código admite de 2 a 12 letras, dígitos o guiones (p. ej. RES-S).")
                .requerido(nombre, "el nombre")
                .maximo(nombre, 120, "nombre")
                .requerido(form.categoria(), "la categoría")
                .requerido(form.formaCobro(), "la forma de cobro")
                .requerido(precio, "el precio")
                .si(precio != null && (precio.signum() < 0 || precio.compareTo(PRECIO_MAXIMO) > 0),
                        "El precio debe estar entre 0 y 999 999.99.")
                .si(precio != null && precio.stripTrailingZeros().scale() > 2, "El precio admite como máximo 2 decimales.")
                .si(form.duracionMin() < 5 || form.duracionMin() > 480, "La duración debe estar entre 5 y 480 minutos.")
                .lanzarSiHayErrores();

        procedimientoRepository.findByCodigoIgnoreCase(codigo)
                .filter(p -> !p.getId().equals(form.id()))
                .ifPresent(p -> {
                    throw new NegocioException("Ya existe un procedimiento con el código " + codigo + ".");
                });

        Procedimiento p = form.id() == null ? new Procedimiento() : procedimientoRepository.findById(form.id())
                .orElseThrow(() -> new NegocioException("El procedimiento ya no existe en la base de datos."));
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setCategoria(form.categoria());
        p.setPrecio(precio);
        p.setFormaCobro(form.formaCobro());
        p.setDuracionMin(form.duracionMin());
        p.setActivo(form.activo());
        return procedimientoRepository.save(p).getId();
    }

    static ProcedimientoDto dto(Procedimiento p) {
        return new ProcedimientoDto(p.getId(), p.getCodigo(), p.getNombre(), p.getCategoria(), p.getPrecio(),
                p.getFormaCobro(), p.getDuracionMin(), p.isActivo());
    }
}
