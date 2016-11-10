package gov.nist.drmf.interpreter.core.grammar;

import gov.nist.drmf.interpreter.common.letters.AbstractGreekLetterInterpreter;
import gov.nist.drmf.interpreter.common.letters.GreekLetters;

/**
 * Created by Andre Greiner-Petter on 10.11.2016.
 */
public class MathematicaGreekLetterInterpreter extends AbstractGreekLetterInterpreter {
    @Override
    public String convertToCAS(String latex) {
        return GreekLetters.getLetterFromLaTeX(latex).mathematica;
    }

    @Override
    public String convertFromCAS(String cas) {
        return GreekLetters.getLetterFromMathematica(cas).latex;
    }
}
