package gov.nist.drmf.interpreter.evaluation.constraints;

import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.mlp.extensions.MatchablePomTaggedExpression;
import gov.nist.drmf.interpreter.mlp.extensions.MatcherConfig;
import gov.nist.drmf.interpreter.mlp.extensions.PomMatcherBuilder;
import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;

import java.util.LinkedList;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class ConstraintBlueprint {

    private static final SemanticMLPWrapper SMLP = SemanticMLPWrapper.getStandardInstance();

    private static final String KEY_PREFIX = "var";

    private MatchablePomTaggedExpression mpte;

    private MatcherConfig config;

    private String[] values;

    public ConstraintBlueprint(String latex, String... vals) throws ParseException {
        latex = MLPBlueprintTree.preCleaning(latex);

        config = MatcherConfig.getExactMatchConfig();
        config.ignoreBracketLogic(true);
        config.setIllegalCharacterForWildcard("var", "([;]|\\\\[lgn]eq|[<>=]|[-\\d.]+)");
        config.setIllegalCharacterForWildcard("var1", "(\\\\[lgn]eq|[,;<>=]|[-\\d.]+)");
        config.setIllegalCharacterForWildcard("var2", "(\\\\[lgn]eq|[,;<>=]|[-\\d.]+)");

        mpte = PomMatcherBuilder.compile(SMLP, latex, KEY_PREFIX+"\\d*");
        values = vals;
    }

    public boolean match(String expression) throws ParseException {
        expression = MLPBlueprintTree.preCleaning(expression);
        PrintablePomTaggedExpression ppte = SMLP.parse(expression);
        return mpte.matchUnsafe(ppte, config);
    }

    public String[][] getConstraintVariables() {
        String[][] out = new String[2][];
        Map<String, String> groups = mpte.getStringMatches();

        String var = groups.get(KEY_PREFIX);
        if ( var != null ) {
            out[0] = var.split(",");
            out[1] = new String[out[0].length];
            for ( int i = 0; i < out[0].length; i++ ) out[1][i] = values[0];
            return out;
        }

        out[0] = new String[groups.keySet().size()];
        out[1] = values;
        for (int i = 1; i < out[0].length+1; i++) {
            out[0][i-1] = groups.get(KEY_PREFIX+i);
        }

        return out;
    }
}
