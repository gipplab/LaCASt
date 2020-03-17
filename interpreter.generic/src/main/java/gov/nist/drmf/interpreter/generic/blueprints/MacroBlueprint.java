package gov.nist.drmf.interpreter.generic.blueprints;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

/**
 * TODO currently just a dummy
 * @author Andre Greiner-Petter
 */
public class MacroBlueprint {

    private final PomParser parser;

    private final String blueprintString;

    public MacroBlueprint(String blueprintString) throws ParseException {
        this.blueprintString = blueprintString;
        parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA.toString());
        PomTaggedExpression pte = parser.parse(blueprintString);
    }

    /**
     * Matches the given expression against this blueprint object. If the expression
     * matches the blueprint it returns true. Otherwise false.
     * @param expression
     * @return
     */
    public boolean match(String expression) {
        return false;
    }
}
