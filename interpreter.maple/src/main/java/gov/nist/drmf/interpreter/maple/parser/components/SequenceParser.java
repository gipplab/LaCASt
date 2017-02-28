package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class SequenceParser extends AbstractAlgebraicParser<List> {

    private int length;
    private MapleInternal internal;

    public SequenceParser( MapleInternal internal, int length ){
        this.length = length;
        this.internal = internal;
    }

    @Override
    public boolean parse( List expression ) {
        switch ( internal ){
            case sum:
                return parseSum( expression );
            case prod:
                return parseProd( expression );
            case exp:
                return parseExpSeq( expression );
            default:
                return false;
        }
    }

    private boolean parseProd( List list ){
        return parseSequence(list, true, " \\cdot ");
    }

    private boolean parseSum( List list ){
        return parseSequence(list, false, " + ");
    }

    private boolean parseExpSeq( List list ){
        return parseSequence(list, false, ", ");
    }

    private boolean parseSequence( List list, boolean embraceCheck, String symbol ){
        try {
            Algebraic summand;
            TranslatedList inner_list;
            int length = list.length();
            for ( int i = 2; i < length; i++ ){
                summand = list.select(i);
                inner_list = parseGeneralExpression( summand );

                // check if this was a sum!
                if ( embraceCheck && checkForEmbrace( summand ) )
                    inner_list.embrace();

                translatedList.addTranslatedExpression( inner_list );
                translatedList.addTranslatedExpression( symbol );
            }
            summand = list.select(length);
            inner_list = parseGeneralExpression( summand );
            if ( embraceCheck && checkForEmbrace( summand ) )
                inner_list.embrace();

            translatedList.addTranslatedExpression( inner_list );
            return true;
        } catch ( MapleException me ){
            internalErrorLog += "Cannot parse sum! " + me.getMessage();
            return false;
        }
    }


    private boolean checkForEmbrace( Algebraic element ){
        try {
            List inner = (List)element;
            MapleInternal internal = getAbstractInternal(inner.select(1).toString());
            return internal.equals( MapleInternal.sum );
        } catch ( Exception e ){
            return false;
        }
    }
}
