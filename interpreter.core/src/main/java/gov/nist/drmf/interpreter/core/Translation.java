package gov.nist.drmf.interpreter.core;

import gov.nist.drmf.interpreter.maple.wrapper.Algebraic;

/**
 * A simple java container to collect the information of a translation process.
 * It contains further information about an translation and the translated
 * expression itself.
 * Additionally it can contains an {@link Algebraic} object of the translation
 * also.
 *
 * Created by AndreG-P on 03.03.2017.
 */
public class Translation {
    /**
     * Translated expression and further information
     */
    private String translatedExpression, additionalInformation;

    /**
     * Additionally an algebraic object of the translated expression
     */
    private Algebraic alg;

    /**
     * Creates an object with translated expression and additional information.
     * @param translatedExpression translated expression
     * @param additionalInformation further information
     */
    Translation( String translatedExpression, String additionalInformation ){
        this.translatedExpression = translatedExpression;
        this.additionalInformation = additionalInformation;
    }

    /**
     * Creates an object with translated expression, further information and
     * an algebraic object of the translated expression
     * @param algebraicTranslatedExpression translated expression as algebraic object
     * @param translatedExpression translated object as string
     * @param additionalInformation further information
     */
    Translation( Algebraic algebraicTranslatedExpression,
                 String translatedExpression,
                 String additionalInformation ){
        this( translatedExpression, additionalInformation );
        this.alg = algebraicTranslatedExpression;
    }

    /**
     * Returns the translated expression
     * @return translated expression
     */
    public String getTranslatedExpression() {
        return translatedExpression;
    }

    /**
     * Returns further information about translation process
     * @return further information
     */
    public String getAdditionalInformation() {
        return additionalInformation;
    }

    /**
     * Returns the algebraic object of the translated expression. Be aware this
     * could be null. This class only contains the Algebraic object if you invoke
     * {@link gov.nist.drmf.interpreter.core.Translator#translateFromLaTeXToMapleAlgebraic(String,String)}
     * (06. March 2017).
     *
     * @return the algebraic object of the translated expression
     */
    public Algebraic getAlgebraicTranslatedExpression() {
        return alg;
    }
}
