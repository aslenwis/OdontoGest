package com.odontogest.integracion.correo;

public record ParametrosSmtpASEJ(String hostASEJ, int puertoASEJ, String usuarioASEJ, String passwordASEJ, boolean starttlsASEJ, String remitenteASEJ,
                             String nombreRemitenteASEJ) {

    public boolean configuradoASEJ() {
        return hostASEJ != null && !hostASEJ.isBlank() && puertoASEJ > 0
                && ((remitenteASEJ != null && !remitenteASEJ.isBlank()) || (usuarioASEJ != null && !usuarioASEJ.isBlank()));
    }

    public String desdeASEJ() {
        return remitenteASEJ != null && !remitenteASEJ.isBlank() ? remitenteASEJ : usuarioASEJ;
    }
}
