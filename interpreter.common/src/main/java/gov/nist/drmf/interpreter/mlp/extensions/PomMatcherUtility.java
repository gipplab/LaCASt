package gov.nist.drmf.interpreter.mlp.extensions;

import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public final class PomMatcherUtility {

    private PomMatcherUtility() {}

    public static String fillPatterns(String expression, Map<String, String> replacements) {
        for ( Map.Entry<String, String> e : replacements.entrySet() ) {
            expression = expression.replace( e.getKey(), e.getValue() );
        }
        return expression;
    }
}
