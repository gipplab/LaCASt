package gov.nist.drmf.interpreter.common.latex;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.symbols.GenericTranslationMapper;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Represents relation symbols
 * @author Andre Greiner-Petter
 */
public enum Relations {
    EQUAL("=", "="),
    UNEQUAL("<>", "\\neq"),
    GREATER_THAN(">", ">"),
    GREATER_EQ_THAN(">=", "\\geq"),
    LESS_THAN("<", "<"),
    LESS_EQ_THAN("<=", "\\leq");

    private static final Logger LOG = LogManager.getLogger(Relations.class.getName());

    private final String symbol;
    private final String texSymbol;

    Relations( String symbol, String texSymbol ) {
        this.symbol = symbol;
        this.texSymbol = texSymbol;
    }

    /**
     * The relation symbol commonly used by CAS. This is at least true for the major CAS we currently
     * support, like Maple and Mathematica. In the future it might make more sense to actually translate
     * the tex via {@link gov.nist.drmf.interpreter.common.symbols.SymbolTranslator}.
     * @return the symbol in CAS syntax
     * @deprecated use {@link #getSymbol(String)} instead
     */
    public String getSymbol(){
        return symbol;
    }

    /**
     * Gets the translated version of the relation symbol.
     * If the translator cannot be used, it returns the default symbol (which is valid
     * for Maple and Mathematica at least).
     * @param cas the CAS to translate the symbol to
     * @return the translated symbol
     */
    public String getSymbol(String cas) {
        try {
            GenericTranslationMapper st = SymbolTranslator.getGenericMapper();
            return st.translate(Keys.KEY_LATEX, cas, texSymbol);
        } catch (IOException e) {
            LOG.error("Unable to get symbol translator instance.", e);
            return symbol;
        }
    }

    /**
     * The relation symbol as LaTeX
     * @return latex relation symbol
     */
    public String getTexSymbol() {
        return texSymbol;
    }

    /**
     * Analyzes the given equation and returns the representative relation.
     * @param symbol latex symbol
     * @return the relation or null if the symbol is not a relation
     */
    public static Relations getRelation(String symbol) {
        Relations rel = null;
        if ( symbol.matches("\\s*(?:\\\\leq?|<=)\\s*") ){
            rel = Relations.LESS_EQ_THAN;
        }
        else if ( symbol.matches( "\\s*(?:\\\\geq?|=>)\\s*" ) ){
            rel = Relations.GREATER_EQ_THAN;
        }
        else if ( symbol.matches( "\\s*(?:\\\\neq?|<>)\\s*" ) ){
            rel = Relations.UNEQUAL;
        }
        else if ( symbol.matches("\\s*<\\s*") ){
            rel = Relations.LESS_THAN;
        }
        else if ( symbol.matches( "\\s*>\\s*" ) ){
            rel = Relations.GREATER_THAN;
        }
        else if ( symbol.matches( "\\s*=\\s*" ) ) {
            rel = Relations.EQUAL;
        }
        return rel;
    }
}
