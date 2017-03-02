package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class SequenceTranslator extends ListTranslator {
    SequenceTranslator(MapleInternal internal, int length ){
        super( internal, length );
    }

    @Override
    public boolean translate( List expression ) throws Exception {
        switch ( root ){
            case sum:
                parseSum( expression );
                return true;
            case prod:
                parseProd( expression );
                return true;
            case exp:
                parseExpSeq( expression );
                return true;
            default:
                failures.addFailure("Unknown Sequence.", this.getClass(), expression.toString());
                return false;
        }
    }

    private void parseProd( List list ) throws Exception{
        parseSequence(list, true, MULTIPLY);
    }

    private void parseSum( List list ) throws Exception{
        parseSequence(list, false, ADD);
    }

    private void parseExpSeq( List list ) throws Exception{
        parseSequence(list, false, ", ");
    }

    private void parseSequence( List list, boolean embraceCheck, String symbol ) throws Exception {
        Algebraic summand;
        TranslatedList inner_list;
        int length = list.length();
        for ( int i = 2; i < length; i++ ){
            summand = list.select(i);
            inner_list = translateGeneralExpression( summand );

            // check if this was a sum!
            if ( embraceCheck && checkForEmbrace( summand ) )
                inner_list.embrace();

            translatedList.addTranslatedExpression( inner_list );
            translatedList.addTranslatedExpression( symbol );
        }
        summand = list.select(length);
        inner_list = translateGeneralExpression( summand );
        if ( embraceCheck && checkForEmbrace( summand ) )
            inner_list.embrace();

        translatedList.addTranslatedExpression( inner_list );
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
