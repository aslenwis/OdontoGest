package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.ResultadoIndiceASEJ;

public record IndicesVistaASEJ(ResultadoIndiceASEJ cpodASEJ, ResultadoIndiceASEJ ceodASEJ, boolean excluyeTercerasMolaresASEJ) {
}
