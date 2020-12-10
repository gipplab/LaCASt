package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.cas.Constraints;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.IDLMFTranslator;
import gov.nist.drmf.interpreter.common.interfaces.IPackageWrapper;
import gov.nist.drmf.interpreter.common.text.TextUtility;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public interface IConstraintTranslator extends IDLMFTranslator {
    static final Pattern NUM_PATTERN = Pattern.compile("(\\d+)\\s*\\\\[cl]?dots");

    Set<String> getRequiredPackages();

    IPackageWrapper<String, String> getPackageWrapper();

    @Override
    default String translate(String expression) throws TranslationException {
        return translate(expression, null);
    }

    default String[] translateEachConstraint(String[] constraints) {
        return translateEachConstraint(constraints, null);
    }

    default String[] translateEachConstraint(String[] constraints, String label) {
        return Arrays.stream(constraints)
                .map( c -> TextUtility.appendPattern(c, NUM_PATTERN, 1))
                .filter( c -> !c.matches(".*\\\\[cl]?dots.*") )
                .map( Constraints::stripDollar )
                .map( c -> translate(c, label) )
                // TODO, why did I do that?
//                .map( c -> c.replaceAll("\\*", " ") )
                .map( Constraints::splitMultiAss )
                .toArray(String[]::new);
    }
}
