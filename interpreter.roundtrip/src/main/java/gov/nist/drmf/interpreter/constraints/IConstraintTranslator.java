package gov.nist.drmf.interpreter.constraints;

import gov.nist.drmf.interpreter.common.grammar.ITranslator;

import java.util.Arrays;

/**
 * @author Andre Greiner-Petter
 */
public interface IConstraintTranslator extends ITranslator {
    default String[] translateEachConstraint(String[] constraints) {
        return Arrays.stream(constraints)
                .filter( c -> !c.matches(".*\\\\[cl]?dots.*") )
                .map( Constraints::stripDollar )
                .map( this::translate )
                .map( Constraints::splitMultiAss )
                .toArray(String[]::new);
    }
}
