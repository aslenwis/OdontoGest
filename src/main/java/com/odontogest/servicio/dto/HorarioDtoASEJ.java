package com.odontogest.servicio.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioDtoASEJ(DayOfWeek diaASEJ, LocalTime inicioASEJ, LocalTime finASEJ) {
}
