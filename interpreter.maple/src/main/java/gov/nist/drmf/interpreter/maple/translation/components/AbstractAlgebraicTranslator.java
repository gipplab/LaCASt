package gov.nist.drmf.interpreter.maple.translation.components;

import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.config.TranslationProcessConfig;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.interfaces.IBackwardTranslator;
import gov.nist.drmf.interpreter.common.exceptions.MapleTranslationException;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.grammar.TranslationFailures;
import gov.nist.drmf.interpreter.maple.wrapper.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;
import gov.nist.drmf.interpreter.maple.wrapper.MapleList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.MAPLE_INTERNAL_PATTERN;

/**
 *
 * Created by AndreG-P on 21.02.2017.
 */
public abstract class AbstractAlgebraicTranslator<T extends Algebraic>
        implements IBackwardTranslator<T, Boolean> {
    protected static String MULTIPLY, ADD, INFINITY;

    static final Logger LOG = LogManager.getLogger( AbstractAlgebraicTranslator.class );

    protected TranslatedList translatedList = new TranslatedList();

    static TranslationFailures failures = new TranslationFailures();

    static InformationLogger infos = new InformationLogger();

    private TranslationProcessConfig config;

    public void setConfig(TranslationProcessConfig config) {
        this.config = config;
    }

    @Override
    public TranslationProcessConfig getConfig() {
        return config;
    }

    /**
     *
     * @param element
     * @return
     */
    protected TranslatedList translateGeneralExpression( Algebraic element )
            throws TranslationException, MapleException {
        if ( !(MapleList.isInstance(element)) ){
            LOG.fatal(
                    "The general translator assumes an algebraic object " +
                    "in a Maple inert-form in a List structure but get: " +
                    element
            );
            return null;
        }

        MapleList list = MapleList.cast(element);
        String root = list.select(1).toString();
        MapleInternal in = getAbstractInternal(root);
        LOG.trace( "Translate general expression: " + list );
        ListTranslator lParser = new ListTranslator( in, list.length() );
        lParser.translate( list );
        return lParser.translatedList;
    }

    @Override
    public String getTranslatedExpression() {
        return translatedList.getAccurateString();
    }

    public TranslationFailures getFailures(){
        return failures;
    }

    public InformationLogger getInfos(){
        return infos;
    }

    @Override
    public abstract Boolean translate( T element ) throws TranslationException;

    MapleInternal getAbstractInternal(String root ) throws IllegalArgumentException{
        Matcher match = MAPLE_INTERNAL_PATTERN.matcher(root);
        if ( !match.matches() )
            throw new IllegalArgumentException("Unknown name of maple object: " + root);

        MapleInternal in = MapleInternal.getInternal( match.group(1) );
        if ( root == null )
            throw new IllegalArgumentException("Not supported maple object: " + root);

        return in;
    }

    String[] translateExpressionSequence( MapleList exp_seq )
            throws MapleTranslationException, MapleException {
        String[] translations = new String[exp_seq.length()-1];
        for ( int i = 2; i-2 < translations.length; i++ ){
            TranslatedList tl = translateGeneralExpression( exp_seq.select(i) );
            translations[i-2] = tl.getAccurateString();
        }
        return translations;
    }

    public static TranslationException createException(String message) {
        return createException(message, TranslationExceptionReason.MAPLE_TRANSLATION_ERROR);
    }

    public static TranslationException createException(String message, Throwable throwable) {
        return createException(message, TranslationExceptionReason.MAPLE_TRANSLATION_ERROR, throwable);
    }

    public static TranslationException createException(String message, TranslationExceptionReason reason) {
        return new TranslationException(
                Keys.KEY_MAPLE,
                Keys.KEY_LATEX,
                message,
                reason
        );
    }

    public static TranslationException createException(
            String message,
            TranslationExceptionReason reason,
            Throwable throwable
            ) {
        return new TranslationException(
                Keys.KEY_MAPLE,
                Keys.KEY_LATEX,
                message,
                reason,
                throwable
        );
    }
}
