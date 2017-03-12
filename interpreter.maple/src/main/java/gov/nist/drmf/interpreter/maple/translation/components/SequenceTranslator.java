package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.application.Maple;
import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.Keys;
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
                throw new TranslationException(
                        Keys.KEY_MAPLE,
                        Keys.KEY_LATEX,
                        "Cannot parse expression sequences here!" );
            default:
                LOG.debug( "Unknown object reached. " + root );
                failures.addFailure("Unknown Sequence.", this.getClass(), expression.toString());
                return false;
        }
    }

    private void parseProd( List list ) throws TranslationException, MapleException{
        //parseSequence(list, true, MULTIPLY, true);
        Algebraic factor;
        List flist;
        int start_index = 2;
        int length = list.length();
        boolean negative = false;
        MapleInternal maple_internal;
        TranslatedList inner_trans = new TranslatedList();

        // check starting sign
        // since we reorder internal maple structure, the constant +/-1 is
        // always leading a sequence
        flist = (List)list.select( start_index );
        // check if first is pos oder neg integer
        maple_internal = getAbstractInternal(flist.select(1).toString());
        switch ( maple_internal ){
            case intneg:
                negative = true;
            case intpos:
                Numeric num = (Numeric)flist.select(2);
                int n = num.intValue();
                if ( n == 1 ){
                    start_index++;
                } else negative = false;
                LOG.debug( root + ": Found +/- 1 multiplication and skip it. Sign-Negative: " + negative );
        }

        for ( int i = start_index; i <= length; i++ ){
            factor = list.select(i);
            if ( !(factor instanceof List) )
                throw new TranslationException(
                        Keys.KEY_MAPLE, Keys.KEY_LATEX,
                        "Expected inner list in product but get: " + factor );
            flist = (List)factor;
            maple_internal = getAbstractInternal( flist.select(1).toString() );

            boolean embrace =
                    maple_internal.equals( MapleInternal.sum ) ||
                            maple_internal.equals( MapleInternal.complex );

            TranslatedList tmp = translateGeneralExpression( flist );
            if ( embrace ) tmp.embrace();
            LOG.debug( "Factor-"+(i-start_index)+": " + tmp );
            inner_trans.addTranslatedExpression( tmp );

            if ( i < length )
                inner_trans.addTranslatedExpression( MULTIPLY );
        }

        if ( negative ) inner_trans.setSign( MapleConstants.NEGATIVE );
        translatedList.addTranslatedExpression( inner_trans );
        LOG.debug("Prod Changed List: " + translatedList);
    }

    private void parseSum( List list ) throws MapleException{
        Algebraic summand;
        int length = list.length();

        for ( int i = 2; i <= length; i++ ){
            summand = list.select(i);
            translatedList.addTranslatedExpression(translateGeneralExpression( summand ));
            if ( i < length )
                translatedList.addTranslatedExpression( ADD );
            LOG.debug("Sum Changed List: " + translatedList);
        }
    }
}
