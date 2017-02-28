package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.common.grammar.IParser;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.MAPLE_INTERNAL_PATTERN;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public abstract class AbstractAlgebraicParser<T extends Algebraic> implements IParser<T> {
    public static String MULTIPLY, ADD, INFINITY;

    protected TranslatedList translatedList = new TranslatedList();

    protected String internalErrorLog = "";

    /**
     *
     * @param element
     * @return
     */
    protected TranslatedList parseGeneralExpression( Algebraic element )  {
        // First, test if the element is a list.
        if ( element instanceof List ){
            List list = (List)element;
            try{
                String root = list.select(1).toString();
                MapleInternal in = getAbstractInternal(root);
                ListParser lParser = new ListParser( in, list.length() );
                if ( !lParser.parse(list) ) {
                    this.internalErrorLog += "ListParser crashes: " + lParser.internalErrorLog;
                    return null;
                }
                else {
                    return lParser.translatedList;
                }
            } catch ( MapleException | IllegalArgumentException e ){
                this.internalErrorLog += e.getMessage();
                return null;
            }
        }

        // otherwise it must be a usual Algebraic object.
        // TODO hmm, this should not happen.
        System.err.println("Well, the general parser assumes a list in general.");
        return null;
    }

    @Override
    public String getTranslatedExpression() {
        return translatedList.getAccurateString();
    }

    public String getInternalErrorLog(){
        return internalErrorLog;
    }

    @Override
    public abstract boolean parse( T element );

    public MapleInternal getAbstractInternal( String root ) throws IllegalArgumentException{
        Matcher match = MAPLE_INTERNAL_PATTERN.matcher(root);
        if ( !match.matches() )
            throw new IllegalArgumentException("Unknown name of maple object: " + root);

        MapleInternal in = MapleInternal.getInternal( match.group(1) );
        if ( root == null )
            throw new IllegalArgumentException("Not supported maple object: " + root);

        return in;
    }
}
