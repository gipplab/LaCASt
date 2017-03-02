package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.MString;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class FunctionAndVariableTranslator extends AbstractAlgebraicTranslator<List> {
    private MapleInternal internal;

    public FunctionAndVariableTranslator(MapleInternal internal){
        this.internal = internal;
    }

    @Override
    public boolean translate(List list) {
        switch ( internal ){
            case string:
            case name:
                return parseString( list );
            case ass_name:
                String name = null;
                try {
                    name = ((MString)list.select(2)).stringValue();
                } catch ( Exception e ){}
                internalErrorLog +=
                        "Assigned names are only allowed in functions! " +
                                "To find this here, means you previously defined the " +
                                "name: " + name + ". Which is not allowed!";
                return false;
            case function:
                return false;
            case power:
                return parsePower( list );
            default:
                internalErrorLog += "Wrong Parser for given element: " + internal;
                return false;
        }
    }

    private boolean parseString( List list ){
        try {
            Algebraic a = list.select(2);
            if ( !(a instanceof MString) )
                throw new MapleException("Expected a MString object but get: " + a);
            MString ms = (MString)a;
            String str = ms.stringValue();

            TranslatedExpression t;

            // a possible greek letter
            GreekLetters greek = MapleInterface.getGreekTranslator();
            Constants constants = MapleInterface.getConstantsTranslator();

            // TODO additional information here!
            String constant = constants.translate( str );
            if ( constant != null )
                t = new TranslatedExpression(constant);
            else {
                String greekResult = greek.translate( str );
                if ( greekResult != null )
                    t = new TranslatedExpression(greekResult);
                else t = new TranslatedExpression(str);
            }

            translatedList.addTranslatedExpression( t );
            return true;
        } catch ( MapleException e ){
            internalErrorLog += "Cannot translate string. " + e.getMessage();
            return false;
        }
    }

    private boolean parsePower( List list ){
        try {
            List base = (List)list.select(2);
            List exponent = (List)list.select(3);

            TranslatedList trans_base = parseGeneralExpression( base );
            if ( trans_base.getLength() > 1 )
                trans_base.embrace();

            TranslatedList trans_exponent = parseGeneralExpression( exponent );
            trans_exponent.embrace( Brackets.left_braces );

            translatedList.addTranslatedExpression( trans_base );
            translatedList.addTranslatedExpression( GlobalConstants.CARET_CHAR );
            translatedList.addTranslatedExpression( trans_exponent );
            return true;
        } catch ( MapleException e ){
            internalErrorLog += "Cannot translate power. " + e.getMessage();
            return false;
        }
    }

}
