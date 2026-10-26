package com.odontogest.seeder;

import com.odontogest.config.RelojAplicacionASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.servicio.ConfiguracionServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(3)
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeederASEJ implements ApplicationRunner {

    public static final String CLAVE_ULTIMO_HC_DEMOASEJ = PacienteServiceASEJ.CLAVE_ULTIMO_HC_DEMOASEJ;

    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final UsuarioSeederASEJ usuarioSeederASEJ;
    private final OdontologoSeederASEJ odontologoSeederASEJ;
    private final ClinicaDemoSeederASEJ clinicaDemoSeederASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final RelojAplicacionASEJ relojASEJ;
    private final PlatformTransactionManager transaccionesASEJ;

    @Value("${app.seeder.datos-demo:true}")
    private boolean datosDemoASEJ;
    @Value("${app.seeder.semilla:2026}")
    private long semillaASEJ;

    @Override
    public void run(ApplicationArguments argsASEJ) {
        if (usuarioRepositoryASEJ.count() > 0) {
            return;
        }
        LocalDateTime ahoraASEJ = LocalDateTime.now(relojASEJ);
        logASEJ.info("Base de datos vacía: cargando datos de demostración");
        relojASEJ.fijarASEJ(ahoraASEJ.minusDays(ClinicaDemoSeederASEJ.DIAS_HISTORIAASEJ + 1L).withHour(8).withMinute(0));
        List<OdontologoASEJ> odontologosASEJ;
        try {
            odontologosASEJ = new TransactionTemplate(transaccionesASEJ).execute(sASEJ -> {
                List<OdontologoASEJ> creadosASEJ = odontologoSeederASEJ.sembrarASEJ();
                usuarioSeederASEJ.sembrarASEJ(creadosASEJ.get(0));
                return creadosASEJ;
            });
        } finally {
            relojASEJ.liberarASEJ();
        }
        if (!datosDemoASEJ || odontologosASEJ == null) {
            return;
        }
        long inicioASEJ = System.currentTimeMillis();
        try {
            ClinicaDemoSeederASEJ.ResumenASEJ rASEJ = clinicaDemoSeederASEJ.sembrarASEJ(odontologosASEJ, semillaASEJ, ahoraASEJ);
            configuracionServiceASEJ.guardarSistemaASEJ(CLAVE_ULTIMO_HC_DEMOASEJ, rASEJ.ultimaHistoriaASEJ());
            logASEJ.info("Datos demo en {} s: {} pacientes, {} primeras consultas, {} evoluciones, {} pagos, {} citas en ±30 días"
                            + " ({} pasos omitidos)", (System.currentTimeMillis() - inicioASEJ) / 1000, rASEJ.pacientesASEJ(),
                    rASEJ.primerasConsultasASEJ(), rASEJ.evolucionesASEJ(), rASEJ.pagosASEJ(), rASEJ.citasVentanaASEJ(), rASEJ.omitidosASEJ());
        } catch (RuntimeException eASEJ) {
            logASEJ.error("No se pudieron cargar todos los datos de demostración", eASEJ);
        }
    }
}
