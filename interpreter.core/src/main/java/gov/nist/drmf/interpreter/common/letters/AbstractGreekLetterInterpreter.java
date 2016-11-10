package gov.nist.drmf.interpreter.common.letters;

/**
 * This is an abstract class to translate greek letters
 * between LaTeX and a computer algebra system. All greek letters
 * needs to be included in the enum @see gov.nist.drmf.interpreter.common.letters.GreekLetters
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public abstract class AbstractGreekLetterInterpreter {
    /**
     * Converts the given LaTeX greek letter to a CAS
     * @param latex greek letter
     * @return the greek letter in the CAS
     */
    public abstract String convertToCAS( String latex );

    /**
     * Converts the given greek letter in CAS to LaTeX
     * @param cas greek letter
     * @return the greek letter in LaTeX
     */
    public abstract String convertFromCAS( String cas );
}
