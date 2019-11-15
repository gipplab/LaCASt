package gov.nist.drmf.interpreter.common.exceptions;

import gov.nist.drmf.interpreter.common.constants.Keys;

import static gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason.MAPLE_TRANSLATION_ERROR;

/**
 * Created by AndreG-P on 14.03.2017.
 */
public class MapleTranslationException extends TranslationException
{
    public MapleTranslationException(String message) {
        super(Keys.KEY_MAPLE, Keys.KEY_LATEX, message, MAPLE_TRANSLATION_ERROR);
    }

    public MapleTranslationException(String message, Throwable t){
        super(Keys.KEY_MAPLE, Keys.KEY_LATEX, message, MAPLE_TRANSLATION_ERROR, t);
    }
}
