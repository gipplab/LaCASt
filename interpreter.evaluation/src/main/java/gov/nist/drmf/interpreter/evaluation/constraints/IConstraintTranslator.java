package gov.nist.drmf.interpreter.evaluation.constraints;

import gov.nist.drmf.interpreter.common.exceptions.TranslationException;

import java.util.Arrays;

/**
 * @author Andre Greiner-Petter
 */
public interface IConstraintTranslator {
    /**
     * @param expression the expression to translate
     * @param label label of the latex expression (can be null if there is none)
     * @return translated expression
     * @throws TranslationException if an error occurred
     */
    String translate( String expression, String label ) throws TranslationException;

    default String[] translateEachConstraint(String[] constraints) {
        return translateEachConstraint(constraints, null);
    }

    default String[] translateEachConstraint(String[] constraints, String label) {
        return Arrays.stream(constraints)
                .filter( c -> !c.matches(".*\\\\[cl]?dots.*") )
                .map( Constraints::stripDollar )
                .map( c -> translate(c, label) )
                // TODO, why did I do that?
//                .map( c -> c.replaceAll("\\*", " ") )
                .map( Constraints::splitMultiAss )
                .toArray(String[]::new);
    }
}
