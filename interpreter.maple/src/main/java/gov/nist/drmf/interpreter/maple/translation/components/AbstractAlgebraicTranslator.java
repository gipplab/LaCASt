package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.grammar.TranslationFailures;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.MAPLE_INTERNAL_PATTERN;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public abstract class AbstractAlgebraicTranslator<T extends Algebraic> implements ITranslator<T> {
    protected static String MULTIPLY, ADD, INFINITY;

    static final Logger LOG = LogManager.getLogger( AbstractAlgebraicTranslator.class );

    protected TranslatedList translatedList = new TranslatedList();

    static TranslationFailures failures = new TranslationFailures();

    static InformationLogger infos = new InformationLogger();

    /**
     *
     * @param element
     * @return
     */
    protected TranslatedList translateGeneralExpression( Algebraic element )
            throws TranslationException, MapleException {
        if ( !(element instanceof List) ){
            LOG.fatal(
                    "The general translator assumes an algebraic object " +
                    "in a Maple inert-form in a List structure but get: " +
                    element
            );
            return null;
        }

        List list = (List)element;
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
    public abstract boolean translate( T element ) throws TranslationException, MapleException;

    MapleInternal getAbstractInternal( String root ) throws IllegalArgumentException{
        Matcher match = MAPLE_INTERNAL_PATTERN.matcher(root);
        if ( !match.matches() )
            throw new IllegalArgumentException("Unknown name of maple object: " + root);

        MapleInternal in = MapleInternal.getInternal( match.group(1) );
        if ( root == null )
            throw new IllegalArgumentException("Not supported maple object: " + root);

        return in;
    }
}
