package com.odontogest.servicio;

import com.odontogest.dominio.comun.ValidacionesASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.HorarioAtencionASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.persistencia.repositorio.HorarioAtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontologoRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.HorarioDtoASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.OdontologoFormASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OdontologoServiceASEJ {

    private static final Pattern COPASEJ = Pattern.compile("\\d{1,6}");
    private static final Pattern COLORASEJ = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Locale ESASEJ = Locale.forLanguageTag("es-PE");

    private final OdontologoRepositoryASEJ odontologoRepositoryASEJ;
    private final HorarioAtencionRepositoryASEJ horarioRepositoryASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<OdontologoDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        List<OdontologoASEJ> listaASEJ = incluirInactivosASEJ
                ? odontologoRepositoryASEJ.findAllByOrderByApellidosASEJAscNombresASEJAsc()
                : odontologoRepositoryASEJ.findByActivoASEJTrueOrderByApellidosASEJAscNombresASEJAsc();
        return listaASEJ.stream().map(this::dtoASEJ).toList();
    }

    public OdontologoDtoASEJ obtenerASEJ(long idASEJ) {
        return dtoASEJ(buscarASEJ(idASEJ));
    }

    @Transactional
    public long guardarASEJ(OdontologoFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CATALOGOS_EDITAR);
        String nombresASEJ = ValidadorASEJ.limpioASEJ(formASEJ.nombresASEJ());
        String apellidosASEJ = ValidadorASEJ.limpioASEJ(formASEJ.apellidosASEJ());
        String copASEJ = ValidadorASEJ.limpioASEJ(formASEJ.copASEJ());
        String emailASEJ = ValidadorASEJ.limpioASEJ(formASEJ.emailASEJ());
        String telefonoASEJ = ValidadorASEJ.limpioASEJ(formASEJ.telefonoASEJ());
        String colorASEJ = ValidadorASEJ.limpioASEJ(formASEJ.colorAgendaASEJ()) == null ? "#2563EB" : formASEJ.colorAgendaASEJ().trim().toUpperCase(Locale.ROOT);
        String especialidadASEJ = ValidadorASEJ.limpioASEJ(formASEJ.especialidadASEJ()) == null ? "Odontología general" : ValidadorASEJ.limpioASEJ(formASEJ.especialidadASEJ());

        ValidadorASEJ vASEJ = new ValidadorASEJ()
                .requeridoASEJ(nombresASEJ, "los nombres")
                .requeridoASEJ(apellidosASEJ, "los apellidos")
                .requeridoASEJ(copASEJ, "el N° de colegiatura (COP)")
                .siASEJ(copASEJ != null && !COPASEJ.matcher(copASEJ).matches(), "El COP debe tener solo dígitos (hasta 6).")
                .siASEJ(!COLORASEJ.matcher(colorASEJ).matches(), "El color de agenda debe tener el formato #RRGGBB.")
                .siASEJ(emailASEJ != null && !ValidacionesASEJ.esEmailASEJ(emailASEJ), "El correo electrónico no es válido.")
                .siASEJ(telefonoASEJ != null && !ValidacionesASEJ.esTelefonoASEJ(telefonoASEJ), "El teléfono no es válido.");
        validarHorariosASEJ(formASEJ.horariosASEJ(), vASEJ);
        vASEJ.lanzarSiHayErroresASEJ();

        odontologoRepositoryASEJ.findByCopASEJIgnoreCase(copASEJ)
                .filter(oASEJ -> !oASEJ.getIdASEJ().equals(formASEJ.idASEJ()))
                .ifPresent(oASEJ -> {
                    throw new NegocioExceptionASEJ("El COP " + copASEJ + " ya está registrado para " + oASEJ.getNombreCompletoASEJ() + ".");
                });

        OdontologoASEJ oASEJ = formASEJ.idASEJ() == null ? new OdontologoASEJ() : buscarASEJ(formASEJ.idASEJ());
        oASEJ.setNombresASEJ(nombresASEJ);
        oASEJ.setApellidosASEJ(apellidosASEJ);
        oASEJ.setCopASEJ(copASEJ);
        oASEJ.setRneASEJ(ValidadorASEJ.limpioASEJ(formASEJ.rneASEJ()));
        oASEJ.setEspecialidadASEJ(especialidadASEJ);
        oASEJ.setTelefonoASEJ(telefonoASEJ);
        oASEJ.setEmailASEJ(emailASEJ == null ? null : emailASEJ.toLowerCase(Locale.ROOT));
        oASEJ.setColorAgendaASEJ(colorASEJ);
        oASEJ.setActivoASEJ(formASEJ.activoASEJ());
        OdontologoASEJ guardadoASEJ = odontologoRepositoryASEJ.save(oASEJ);

        horarioRepositoryASEJ.eliminarPorOdontologoASEJ(guardadoASEJ.getIdASEJ());
        for (HorarioDtoASEJ hASEJ : formASEJ.horariosASEJ()) {
            HorarioAtencionASEJ bloqueASEJ = new HorarioAtencionASEJ();
            bloqueASEJ.setOdontologoASEJ(guardadoASEJ);
            bloqueASEJ.setDiaSemanaASEJ(hASEJ.diaASEJ());
            bloqueASEJ.setHoraInicioASEJ(hASEJ.inicioASEJ());
            bloqueASEJ.setHoraFinASEJ(hASEJ.finASEJ());
            horarioRepositoryASEJ.save(bloqueASEJ);
        }
        return guardadoASEJ.getIdASEJ();
    }

    private static void validarHorariosASEJ(List<HorarioDtoASEJ> horariosASEJ, ValidadorASEJ vASEJ) {
        for (HorarioDtoASEJ hASEJ : horariosASEJ) {
            if (hASEJ.diaASEJ() == null || hASEJ.inicioASEJ() == null || hASEJ.finASEJ() == null) {
                vASEJ.siASEJ(true, "Complete el día y las horas de todos los bloques del horario.");
                return;
            }
            vASEJ.siASEJ(!hASEJ.finASEJ().isAfter(hASEJ.inicioASEJ()), "En el horario, la hora de fin debe ser posterior a la de inicio ("
                    + hASEJ.diaASEJ().getDisplayName(TextStyle.FULL, ESASEJ) + ").");
        }
        List<HorarioDtoASEJ> ordenadosASEJ = new ArrayList<>(horariosASEJ);
        ordenadosASEJ.sort(Comparator.comparing(HorarioDtoASEJ::diaASEJ).thenComparing(HorarioDtoASEJ::inicioASEJ));
        for (int iASEJ = 1; iASEJ < ordenadosASEJ.size(); iASEJ++) {
            HorarioDtoASEJ aASEJ = ordenadosASEJ.get(iASEJ - 1);
            HorarioDtoASEJ bASEJ = ordenadosASEJ.get(iASEJ);
            vASEJ.siASEJ(aASEJ.diaASEJ() == bASEJ.diaASEJ() && bASEJ.inicioASEJ().isBefore(aASEJ.finASEJ()),
                    "Los bloques del " + bASEJ.diaASEJ().getDisplayName(TextStyle.FULL, ESASEJ) + " se superponen.");
        }
    }

    private OdontologoASEJ buscarASEJ(long idASEJ) {
        return odontologoRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El odontólogo ya no existe en la base de datos."));
    }

    private OdontologoDtoASEJ dtoASEJ(OdontologoASEJ oASEJ) {
        List<HorarioDtoASEJ> horariosASEJ = horarioRepositoryASEJ.findByOdontologoASEJIdASEJOrderByDiaSemanaASEJAscHoraInicioASEJAsc(oASEJ.getIdASEJ()).stream()
                .map(hASEJ -> new HorarioDtoASEJ(hASEJ.getDiaSemanaASEJ(), hASEJ.getHoraInicioASEJ(), hASEJ.getHoraFinASEJ()))
                .toList();
        return new OdontologoDtoASEJ(oASEJ.getIdASEJ(), oASEJ.getNombresASEJ(), oASEJ.getApellidosASEJ(), oASEJ.getCopASEJ(), oASEJ.getRneASEJ(),
                oASEJ.getEspecialidadASEJ(), oASEJ.getTelefonoASEJ(), oASEJ.getEmailASEJ(), oASEJ.getColorAgendaASEJ(), oASEJ.isActivoASEJ(), horariosASEJ);
    }
}
