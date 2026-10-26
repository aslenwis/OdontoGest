package com.odontogest.ui.controlador;

import com.odontogest.ui.soporte.RefrescableASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogosControllerASEJ implements RefrescableASEJ {

    private final ProcedimientosTabControllerASEJ procedimientosASEJ;
    private final HallazgosTabControllerASEJ hallazgosASEJ;
    private final ReglasTabControllerASEJ reglasASEJ;
    private final OdontologosTabControllerASEJ odontologosASEJ;

    @Override
    public void refrescarASEJ() {
        procedimientosASEJ.refrescarASEJ();
        hallazgosASEJ.refrescarASEJ();
        reglasASEJ.refrescarASEJ();
        odontologosASEJ.refrescarASEJ();
    }
}
