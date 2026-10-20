/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.dominio.comun.Validaciones;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.HorarioAtencion;
import com.odontogest.modelo.Odontologo;
import com.odontogest.persistencia.repositorio.HorarioAtencionRepository;
import com.odontogest.persistencia.repositorio.OdontologoRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.HorarioDto;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.OdontologoForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Odontólogos y sus horarios de atención (RF-CAT-04).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OdontologoService {

    private static final Pattern COP = Pattern.compile("\\d{1,6}");
    private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private final OdontologoRepository odontologoRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final SesionUsuario sesionUsuario;

    public List<OdontologoDto> listar(boolean incluirInactivos) {
        List<Odontologo> lista = incluirInactivos
                ? odontologoRepository.findAllByOrderByApellidosAscNombresAsc()
                : odontologoRepository.findByActivoTrueOrderByApellidosAscNombresAsc();
        return lista.stream().map(this::dto).toList();
    }

    public OdontologoDto obtener(long id) {
        return dto(buscar(id));
    }

    @Transactional
    public long guardar(OdontologoForm form) {
        sesionUsuario.exigir(Permiso.CATALOGOS_EDITAR);
        String nombres = Validador.limpio(form.nombres());
        String apellidos = Validador.limpio(form.apellidos());
        String cop = Validador.limpio(form.cop());
        String email = Validador.limpio(form.email());
        String telefono = Validador.limpio(form.telefono());
        String color = Validador.limpio(form.colorAgenda()) == null ? "#2563EB" : form.colorAgenda().trim().toUpperCase(Locale.ROOT);
        String especialidad = Validador.limpio(form.especialidad()) == null ? "Odontología general" : Validador.limpio(form.especialidad());

        Validador v = new Validador()
                .requerido(nombres, "los nombres")
                .requerido(apellidos, "los apellidos")
                .requerido(cop, "el N° de colegiatura (COP)")
                .si(cop != null && !COP.matcher(cop).matches(), "El COP debe tener solo dígitos (hasta 6).")
                .si(!COLOR.matcher(color).matches(), "El color de agenda debe tener el formato #RRGGBB.")
                .si(email != null && !Validaciones.esEmail(email), "El correo electrónico no es válido.")
                .si(telefono != null && !Validaciones.esTelefono(telefono), "El teléfono no es válido.");
        validarHorarios(form.horarios(), v);
        v.lanzarSiHayErrores();

        odontologoRepository.findByCopIgnoreCase(cop)
                .filter(o -> !o.getId().equals(form.id()))
                .ifPresent(o -> {
                    throw new NegocioException("El COP " + cop + " ya está registrado para " + o.getNombreCompleto() + ".");
                });

        Odontologo o = form.id() == null ? new Odontologo() : buscar(form.id());
        o.setNombres(nombres);
        o.setApellidos(apellidos);
        o.setCop(cop);
        o.setRne(Validador.limpio(form.rne()));
        o.setEspecialidad(especialidad);
        o.setTelefono(telefono);
        o.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        o.setColorAgenda(color);
        o.setActivo(form.activo());
        Odontologo guardado = odontologoRepository.save(o);

        horarioRepository.eliminarPorOdontologo(guardado.getId());
        for (HorarioDto h : form.horarios()) {
            HorarioAtencion bloque = new HorarioAtencion();
            bloque.setOdontologo(guardado);
            bloque.setDiaSemana(h.dia());
            bloque.setHoraInicio(h.inicio());
            bloque.setHoraFin(h.fin());
            horarioRepository.save(bloque);
        }
        return guardado.getId();
    }

    /** Cada bloque con inicio &lt; fin y sin superponerse con otro del mismo día. */
    private static void validarHorarios(List<HorarioDto> horarios, Validador v) {
        for (HorarioDto h : horarios) {
            if (h.dia() == null || h.inicio() == null || h.fin() == null) {
                v.si(true, "Complete el día y las horas de todos los bloques del horario.");
                return;
            }
            v.si(!h.fin().isAfter(h.inicio()), "En el horario, la hora de fin debe ser posterior a la de inicio ("
                    + h.dia().getDisplayName(TextStyle.FULL, ES) + ").");
        }
        List<HorarioDto> ordenados = new ArrayList<>(horarios);
        ordenados.sort(Comparator.comparing(HorarioDto::dia).thenComparing(HorarioDto::inicio));
        for (int i = 1; i < ordenados.size(); i++) {
            HorarioDto a = ordenados.get(i - 1);
            HorarioDto b = ordenados.get(i);
            v.si(a.dia() == b.dia() && b.inicio().isBefore(a.fin()),
                    "Los bloques del " + b.dia().getDisplayName(TextStyle.FULL, ES) + " se superponen.");
        }
    }

    private Odontologo buscar(long id) {
        return odontologoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El odontólogo ya no existe en la base de datos."));
    }

    private OdontologoDto dto(Odontologo o) {
        List<HorarioDto> horarios = horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(o.getId()).stream()
                .map(h -> new HorarioDto(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .toList();
        return new OdontologoDto(o.getId(), o.getNombres(), o.getApellidos(), o.getCop(), o.getRne(),
                o.getEspecialidad(), o.getTelefono(), o.getEmail(), o.getColorAgenda(), o.isActivo(), horarios);
    }
}
