package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class SequenceTranslator extends ListTranslator {
    SequenceTranslator( MapleInternal internal, int length ){
        super( internal, length );
    }

    @Override
    public boolean translate( List expression ) throws TranslationException, MapleException {
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
                LOG.debug( "Unknown object reached. " + root );
                failures.addFailure("Unknown Sequence.", this.getClass(), expression.toString());
                return false;
        }
    }

    private void parseProd( List list ) throws MapleException{
        parseSequence(list, true, MULTIPLY, true);
    }

    private void parseSum( List list ) throws MapleException{
        parseSequence(list, false, ADD, false);
    }

    private void parseExpSeq( List list ) throws MapleException{
        parseSequence(list, false, ", ", false);
    }

    private void parseSequence( List list, boolean embraceCheck, String symbol, boolean sign ) throws MapleException {
        Algebraic summand;
        TranslatedList inner_list;
        int length = list.length();

        boolean neg = false;
        boolean embrace = false;
        int starting_index = 2;
        if ( sign ){
            List a = (List)list.select(starting_index);
            MapleInternal in = getAbstractInternal(a.select(1).toString());
            switch ( in ){
                case intneg:
                    neg = true;
                case intpos:
                    Numeric num = (Numeric)a.select(2);
                    int n = num.intValue();
                    if ( n == 1 ){
                        starting_index++;
                    } else neg = false;
                    LOG.debug( root + ": Found +/- 1 multiplication and skip it." );
            }
        }

        for ( int i = starting_index; i <= length; i++ ){
            summand = list.select(i);
            if ( i == starting_index && sign ){
                List l = (List)summand;
                MapleInternal in = getAbstractInternal(l.select(1).toString());
                if ( in.equals( MapleInternal.sum ) )
                    embrace = true;
            }
            inner_list = translateGeneralExpression( summand );

            // check if this was a sum!
            if ( embraceCheck && checkForEmbrace( summand ) )
                inner_list.embrace();

            if ( neg ){
                if ( embrace ) inner_list.setSign( MapleConstants.NEGATIVE );
                else inner_list.setSignWithoutEmbraceCheck( MapleConstants.NEGATIVE );
                neg = false;
            }

            translatedList.addTranslatedExpression( inner_list );
            if ( i < length )
                translatedList.addTranslatedExpression( symbol );
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
