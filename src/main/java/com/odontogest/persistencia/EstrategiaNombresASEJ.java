package com.odontogest.persistencia;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class EstrategiaNombresASEJ extends CamelCaseToUnderscoresNamingStrategy {

    private static final String SUFIJOASEJ = "ASEJ";

    @Override
    public Identifier toPhysicalTableName(Identifier nombreASEJ, JdbcEnvironment entornoASEJ) {
        return super.toPhysicalTableName(sinSufijoASEJ(nombreASEJ), entornoASEJ);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier nombreASEJ, JdbcEnvironment entornoASEJ) {
        return super.toPhysicalColumnName(sinSufijoASEJ(nombreASEJ), entornoASEJ);
    }

    private static Identifier sinSufijoASEJ(Identifier nombreASEJ) {
        if (nombreASEJ == null) {
            return null;
        }
        String textoASEJ = nombreASEJ.getText();
        if (!textoASEJ.endsWith(SUFIJOASEJ)) {
            return nombreASEJ;
        }
        return Identifier.toIdentifier(textoASEJ.substring(0, textoASEJ.length() - SUFIJOASEJ.length()),
                nombreASEJ.isQuoted());
    }
}
