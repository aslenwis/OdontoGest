package com.odontogest.servicio;

import com.odontogest.dominio.odontograma.CadenaIntegridadASEJ;
import com.odontogest.dominio.odontograma.OdontogramaCanonicoASEJ;
import com.odontogest.dominio.odontograma.ResultadoVerificacionASEJ;
import com.odontogest.dominio.odontograma.SuperficiesASEJ;
import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.HallazgoASEJ;
import com.odontogest.modelo.OdontogramaASEJ;
import com.odontogest.persistencia.repositorio.HallazgoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontogramaRepositoryASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegridadServiceASEJ {

    private final OdontogramaRepositoryASEJ odontogramaRepositoryASEJ;
    private final HallazgoRepositoryASEJ hallazgoRepositoryASEJ;

    public ResultadoVerificacionASEJ verificarASEJ(long pacienteIdASEJ) {
        List<OdontogramaCanonicoASEJ> cadenaASEJ = odontogramaRepositoryASEJ
                .findByPacienteASEJIdASEJAndEstadoASEJOrderByCerradoEnASEJAscIdASEJAsc(pacienteIdASEJ, EstadoOdontogramaRegistroASEJ.CERRADO).stream()
                .map(oASEJ -> canonicoASEJ(oASEJ, hallazgoRepositoryASEJ.listarPorOdontogramaASEJ(oASEJ.getIdASEJ())))
                .toList();
        return CadenaIntegridadASEJ.verificarASEJ(cadenaASEJ);
    }

    public String hashAnteriorASEJ(long pacienteIdASEJ) {
        List<OdontogramaASEJ> cerradosASEJ = odontogramaRepositoryASEJ
                .findByPacienteASEJIdASEJAndEstadoASEJOrderByCerradoEnASEJAscIdASEJAsc(pacienteIdASEJ, EstadoOdontogramaRegistroASEJ.CERRADO);
        return cerradosASEJ.isEmpty() ? CadenaIntegridadASEJ.GENESISASEJ : cerradosASEJ.get(cerradosASEJ.size() - 1).getHashCierreASEJ();
    }

    public String calcularHashASEJ(OdontogramaASEJ oASEJ, String hashAnteriorASEJ) {
        return CadenaIntegridadASEJ.calcularASEJ(hashAnteriorASEJ, canonicoASEJ(oASEJ, hallazgoRepositoryASEJ.listarPorOdontogramaASEJ(oASEJ.getIdASEJ())));
    }

    static OdontogramaCanonicoASEJ canonicoASEJ(OdontogramaASEJ oASEJ, List<HallazgoASEJ> hallazgosASEJ) {
        List<OdontogramaCanonicoASEJ.LineaASEJ> lineasASEJ = hallazgosASEJ.stream()
                .map(hASEJ -> new OdontogramaCanonicoASEJ.LineaASEJ(hASEJ.getIdASEJ(), hASEJ.getHallazgoTipoASEJ().getCodigoASEJ(), hASEJ.getPiezaFdiASEJ(),
                        hASEJ.getPiezaFdiFinASEJ(), SuperficiesASEJ.aMascaraASEJ(hASEJ.getSuperficiesASEJ()),
                        hASEJ.getEstadoASEJ() == null ? null : hASEJ.getEstadoASEJ().name(), hASEJ.getSiglaASEJ(), hASEJ.getRegistradoEnASEJ(),
                        hASEJ.getRegistradoPorASEJ()))
                .toList();
        return new OdontogramaCanonicoASEJ(oASEJ.getIdASEJ(), oASEJ.getPacienteASEJ().getIdASEJ(), oASEJ.getTipoASEJ().name(), oASEJ.getDenticionASEJ().name(),
                oASEJ.getCerradoEnASEJ(), oASEJ.getCerradoPorASEJ() == null ? 0 : oASEJ.getCerradoPorASEJ(), oASEJ.getEspecificacionesASEJ(), lineasASEJ,
                oASEJ.getHashAnteriorASEJ(), oASEJ.getHashCierreASEJ());
    }
}
