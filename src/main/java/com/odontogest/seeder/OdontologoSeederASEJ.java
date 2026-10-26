package com.odontogest.seeder;

import com.odontogest.modelo.HorarioAtencionASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.persistencia.repositorio.HorarioAtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontologoRepositoryASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OdontologoSeederASEJ {

    private final OdontologoRepositoryASEJ odontologoRepositoryASEJ;
    private final HorarioAtencionRepositoryASEJ horarioRepositoryASEJ;

    public List<OdontologoASEJ> sembrarASEJ() {
        List<OdontologoASEJ> creadosASEJ = new ArrayList<>();
        creadosASEJ.add(crearASEJ("Carlos Alberto", "Quispe Mamani", "90101", null, "Odontología general", "#2563EB"));
        creadosASEJ.add(crearASEJ("María Elena", "Condori Huanca", "90102", "R-0901", "Endodoncia", "#16A34A"));
        creadosASEJ.add(crearASEJ("Jorge Luis", "Ticona Flores", "90103", "R-0902", "Ortodoncia", "#DB2777"));
        return creadosASEJ;
    }

    private OdontologoASEJ crearASEJ(String nombresASEJ, String apellidosASEJ, String copASEJ, String rneASEJ, String especialidadASEJ, String colorASEJ) {
        OdontologoASEJ oASEJ = new OdontologoASEJ();
        oASEJ.setNombresASEJ(nombresASEJ);
        oASEJ.setApellidosASEJ(apellidosASEJ);
        oASEJ.setCopASEJ(copASEJ);
        oASEJ.setRneASEJ(rneASEJ);
        oASEJ.setEspecialidadASEJ(especialidadASEJ);
        oASEJ.setColorAgendaASEJ(colorASEJ);
        oASEJ.setActivoASEJ(true);
        OdontologoASEJ guardadoASEJ = odontologoRepositoryASEJ.save(oASEJ);

        for (DayOfWeek diaASEJ : DayOfWeek.values()) {
            if (diaASEJ == DayOfWeek.SUNDAY) {
                continue;
            }
            bloqueASEJ(guardadoASEJ, diaASEJ, 9, 13);
            if (diaASEJ != DayOfWeek.SATURDAY) {
                bloqueASEJ(guardadoASEJ, diaASEJ, 15, 19);
            }
        }
        return guardadoASEJ;
    }

    private void bloqueASEJ(OdontologoASEJ oASEJ, DayOfWeek diaASEJ, int desdeASEJ, int hastaASEJ) {
        HorarioAtencionASEJ hASEJ = new HorarioAtencionASEJ();
        hASEJ.setOdontologoASEJ(oASEJ);
        hASEJ.setDiaSemanaASEJ(diaASEJ);
        hASEJ.setHoraInicioASEJ(LocalTime.of(desdeASEJ, 0));
        hASEJ.setHoraFinASEJ(LocalTime.of(hastaASEJ, 0));
        horarioRepositoryASEJ.save(hASEJ);
    }
}
