package com.odontogest.dominio.comun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumeroALetrasTestASEJ {

    @Test
    void ejemploDelPlanASEJ() {
        assertThat(NumeroALetrasASEJ.solesASEJ(12050)).isEqualTo("CIENTO VEINTE CON 50/100 SOLES");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "0; CERO",
            "1; UNO",
            "15; QUINCE",
            "16; DIECISÉIS",
            "21; VEINTIUNO",
            "22; VEINTIDÓS",
            "30; TREINTA",
            "31; TREINTA Y UNO",
            "99; NOVENTA Y NUEVE",
            "100; CIEN",
            "101; CIENTO UNO",
            "115; CIENTO QUINCE",
            "500; QUINIENTOS",
            "777; SETECIENTOS SETENTA Y SIETE",
            "999; NOVECIENTOS NOVENTA Y NUEVE",
            "1000; MIL",
            "1001; MIL UNO",
            "2000; DOS MIL",
            "21000; VEINTIÚN MIL",
            "31000; TREINTA Y UN MIL",
            "100000; CIEN MIL",
            "101000; CIENTO UN MIL",
            "999999; NOVECIENTOS NOVENTA Y NUEVE MIL NOVECIENTOS NOVENTA Y NUEVE",
            "1000000; UN MILLÓN",
            "1000001; UN MILLÓN UNO",
            "2500000; DOS MILLONES QUINIENTOS MIL",
            "21000000; VEINTIÚN MILLONES",
            "1000000000; MIL MILLONES",
            "1001000000; MIL UN MILLONES",
            "999999999999; NOVECIENTOS NOVENTA Y NUEVE MIL NOVECIENTOS NOVENTA Y NUEVE MILLONES NOVECIENTOS NOVENTA Y NUEVE MIL NOVECIENTOS NOVENTA Y NUEVE"
    })
    void numerosEnLetrasASEJ(long nASEJ, String esperadoASEJ) {
        assertThat(NumeroALetrasASEJ.numeroASEJ(nASEJ)).isEqualTo(esperadoASEJ);
    }

    @Test
    void centimosConDosDigitosASEJ() {
        assertThat(NumeroALetrasASEJ.solesASEJ(5)).isEqualTo("CERO CON 05/100 SOLES");
        assertThat(NumeroALetrasASEJ.solesASEJ(100)).isEqualTo("UNO CON 00/100 SOLES");
        assertThat(NumeroALetrasASEJ.solesASEJ(100000)).isEqualTo("MIL CON 00/100 SOLES");
        assertThat(NumeroALetrasASEJ.solesASEJ(2_100_099)).isEqualTo("VEINTIÚN MIL CON 99/100 SOLES");
    }

    @Test
    void fueraDeRangoASEJ() {
        assertThatThrownBy(() -> NumeroALetrasASEJ.solesASEJ(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NumeroALetrasASEJ.numeroASEJ(NumeroALetrasASEJ.MAXIMOASEJ + 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
