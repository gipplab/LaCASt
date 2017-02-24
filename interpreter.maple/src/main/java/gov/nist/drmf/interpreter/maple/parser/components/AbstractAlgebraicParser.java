package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.common.grammar.IParser;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public abstract class AbstractAlgebraicParser<T extends Algebraic> implements IParser<T> {

    private static final String SYNTAX_REGEX = "_Inert_([A-Z]+)";

    public static final Pattern PATTERN = Pattern.compile( SYNTAX_REGEX );

    protected String translatedExpression = "";

    protected String internalErrorLog = "";

    /**
     *
     * @param element
     * @return
     */
    protected String parseGeneralExpression( Algebraic element )  {
        // First, test if the element is a list.
        if ( element instanceof List ){
            List list = (List)element;
            try{
                String root = list.select(1).toString();
                ListParser lParser = new ListParser( root, list.length() );
                if ( !lParser.parse(list) ) {
                    this.internalErrorLog += "ListParser crashes: " + lParser.internalErrorLog;
                    return null;
                }
                else {
                    return lParser.translatedExpression;
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
        return translatedExpression;
    }

    public String getInternalErrorLog(){
        return internalErrorLog;
    }

    @Override
    public abstract boolean parse( T element );

    public String extractSyntax( String inert ){
        return null;
    }
}