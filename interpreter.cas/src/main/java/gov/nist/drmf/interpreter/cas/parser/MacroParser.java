package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import mlp.PomTaggedExpression;

/**
 * @author Andre Greiner-Petter
 */
public class MacroParser extends AbstractParser {

    private int
            numOfParams,
            numOfAts,
            numOfVars;

    private String def_dlmf, def_maple;

    private String translation_pattern;

    public MacroParser(
            int numOfParams,
            int numOfAts,
            int numOfVars ){
        this.numOfParams    = numOfParams;
        this.numOfAts       = numOfAts;
        this.numOfVars      = numOfVars;
    }

    @Override
    public boolean parse(PomTaggedExpression expression) {
        return false;
    }
}
