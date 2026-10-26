package com.odontogest.dominio.agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BuscadorHuecosASEJ {

    public Optional<IntervaloASEJ> siguienteHuecoASEJ(LocalDateTime desdeASEJ, Duration duracionASEJ, Duration pasoASEJ,
                                              List<BloqueHorarioASEJ> horarioASEJ, List<IntervaloASEJ> ocupadosASEJ, int diasMaximosASEJ) {
        if (duracionASEJ.isZero() || duracionASEJ.isNegative() || pasoASEJ.isZero() || pasoASEJ.isNegative()) {
            throw new IllegalArgumentException("La duración y el paso deben ser positivos");
        }
        LocalDateTime minimoASEJ = redondearArribaASEJ(desdeASEJ, pasoASEJ);
        LocalDate primerDiaASEJ = desdeASEJ.toLocalDate();
        List<BloqueHorarioASEJ> bloquesASEJ = horarioASEJ.stream().sorted(Comparator.comparing(BloqueHorarioASEJ::inicioASEJ)).toList();

        for (int iASEJ = 0; iASEJ <= diasMaximosASEJ; iASEJ++) {
            LocalDate diaASEJ = primerDiaASEJ.plusDays(iASEJ);
            for (BloqueHorarioASEJ bloqueASEJ : bloquesASEJ) {
                if (bloqueASEJ.diaASEJ() != diaASEJ.getDayOfWeek()) {
                    continue;
                }
                IntervaloASEJ bASEJ = bloqueASEJ.enASEJ(diaASEJ);
                LocalDateTime cursorASEJ = maxASEJ(bASEJ.inicioASEJ(), minimoASEJ);
                List<IntervaloASEJ> delBloqueASEJ = ocupadosASEJ.stream()
                        .filter(oASEJ -> oASEJ.solapaASEJ(bASEJ))
                        .sorted(Comparator.comparing(IntervaloASEJ::inicioASEJ))
                        .toList();
                for (IntervaloASEJ oASEJ : delBloqueASEJ) {
                    if (!Duration.between(cursorASEJ, oASEJ.inicioASEJ()).minus(duracionASEJ).isNegative()) {
                        return Optional.of(IntervaloASEJ.deASEJ(cursorASEJ, duracionASEJ));
                    }
                    cursorASEJ = maxASEJ(cursorASEJ, redondearArribaASEJ(oASEJ.finASEJ(), pasoASEJ));
                }
                if (!Duration.between(cursorASEJ, bASEJ.finASEJ()).minus(duracionASEJ).isNegative()) {
                    return Optional.of(IntervaloASEJ.deASEJ(cursorASEJ, duracionASEJ));
                }
            }
        }
        return Optional.empty();
    }

    static LocalDateTime redondearArribaASEJ(LocalDateTime tASEJ, Duration pasoASEJ) {
        LocalDateTime medianocheASEJ = tASEJ.toLocalDate().atStartOfDay();
        long nanosASEJ = Duration.between(medianocheASEJ, tASEJ).toNanos();
        long pasoNanosASEJ = pasoASEJ.toNanos();
        long redondeadoASEJ = ((nanosASEJ + pasoNanosASEJ - 1) / pasoNanosASEJ) * pasoNanosASEJ;
        return medianocheASEJ.plusNanos(redondeadoASEJ);
    }

    private static LocalDateTime maxASEJ(LocalDateTime aASEJ, LocalDateTime bASEJ) {
        return aASEJ.isAfter(bASEJ) ? aASEJ : bASEJ;
    }
}
