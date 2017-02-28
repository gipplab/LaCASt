package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.MString;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class NumericalParser extends AbstractAlgebraicParser<List> {

    private MapleInternal root;
    private int length;

    public NumericalParser( MapleInternal internal, int length )
            throws IllegalArgumentException {
        if ( length < 2 || length > 3 )
            throw new IllegalArgumentException(
                    "Numerical objects are only 2 or 3 elements long. This has "
                            + length);

        this.root = internal;
        this.length = length;
    }

    @Override
    public boolean parse( List list ) {
        switch (root) {
            case intpos: return parsePosInt(list);
            case intneg: return parseNegInt(list);
            case complex: return parseComplexNumber(list);
            case floating: return parseFloatingNumber(list);
            case rational: return parseRationalNumber(list);
            default:
                internalErrorLog += "Not a numerical element! " + root;
                return false;
        }
    }

    private boolean parsePosInt( List list ){
        String s = parseInt( list );
        if ( s == null ) return false;
        translatedList.addTranslatedExpression(s);
        return true;
    }

    private boolean parseNegInt( List list ){
        String s = parseInt( list );
        if ( s == null ) return false;
        translatedList.addTranslatedExpression(MINUS_SIGN + s);
        return true;
    }

    private String parseInt( List list ){
        try {
            Numeric n = (Numeric)list.select(2);
            return Integer.toString( n.intValue() );
        } catch( MapleException me ){
            internalErrorLog += "Could not parse positive integer! " + me.getMessage();
            return null;
        }
    }

    private boolean parseRationalNumber( List list ){
        try {
            List numerator = (List)list.select(2);
            List denominator = (List)list.select(3);

            String num = parseInt( numerator );
            String denom = parseInt( denominator );

            BasicFunctionsTranslator funcTrans = MapleInterface.getBasicFunctionsTranslator();
            String[] args = new String[]{num, denom};
            String pattern = funcTrans.translate( args, Keys.MLP_KEY_FRACTION );

            TranslatedExpression t;
            MapleInternal num_internal = getAbstractInternal(numerator.select(1).toString());
            if ( num_internal.equals( MapleInternal.intneg ) ){
                t = new TranslatedExpression( pattern, NEGATIVE );
            } else if ( num_internal.equals( MapleInternal.intpos ) ){
                t = new TranslatedExpression( pattern, POSITIVE );
            } else throw new MapleException("Illegal argument in rational object. " + numerator);

            translatedList.addTranslatedExpression( t );
            return true;
        } catch ( MapleException e ){
            internalErrorLog += "Cannot parse rational numbers! " + e.getMessage();
            return false;
        }
    }

    /**
     * A floating number has 2 arguments for sure.
     * First argument is always an integer: INTPOS or INTNEG.
     * The second argument could be an integer or a name for UNDEFINED or INFINITY.
     * @param list
     * @return
     */
    private boolean parseFloatingNumber( List list ){
        try{
            Algebraic first = list.select(2);
            if ( !(first instanceof Numeric) ){
                internalErrorLog +=
                        "A floating number is expected to be VERBATIM with openmaple.Numeric object " +
                                "in the second argument but got: " + first;
                return false;
            }
            Numeric n = (Numeric)first;
            Double d = n.doubleValue();
            TranslatedExpression t;
            if ( d.equals( Double.POSITIVE_INFINITY ) )
                t = new TranslatedExpression( INFINITY, POSITIVE );
            else if ( d.equals( Double.NEGATIVE_INFINITY ) )
                t = new TranslatedExpression( INFINITY, NEGATIVE );
            else t = new TranslatedExpression( d.toString() );
            translatedList.addTranslatedExpression( t );
            return true;
        } catch ( MapleException me ){
            internalErrorLog += "Cannot calculate given double value.";
            return false;
        }
    }

    /**
     * A complex number has length of 2 or 3.
     * In case of 2, the second argument is the complex part
     * In case of 3, the second argument is the real part and the third is the complex part
     *
     * In both cases, the 2nd and 3rd argument can be:
     *  INTPOS, INTNEG, RATIONAL, FLOAT
     * Or NAME or PROD.
     * In the case of NAME only +infinity is allowed -> [COMPLEX, [NAME, "inifinity"]...]
     * In the case of PROD only -infinity is allowed -> [COMPLEX, [PROD, [NAME, "infinity",...], [INTNEG,1]]]
     *
     * @param list
     * @return
     */
    private boolean parseComplexNumber( List list ){
        try {
            TranslatedExpression first = parseComplexElement( (List)list.select(2) );
            translatedList.addTranslatedExpression(first);

            if ( list.length() == 3 ){
                TranslatedExpression second = parseComplexElement( (List)list.select(3) );
                translatedList.addTranslatedExpression(PLUS_SIGN);
                translatedList.addTranslatedExpression(second);
            }

            Constants constants = MapleInterface.getConstantsTranslator();
            String i_unit = constants.translate(MapleConstants.I_UNIT);

            TranslatedExpression imaginary = new TranslatedExpression( MULTIPLY+i_unit , POSITIVE);
            translatedList.addTranslatedExpression( imaginary );
            return true;
        } catch ( MapleException | IllegalArgumentException me ){
            internalErrorLog += "Cannot parse complex number! " + me.getMessage();
            return false;
        }
    }

    private TranslatedExpression parseComplexElement( List list )
            throws IllegalArgumentException, MapleException{
        MapleInternal in = getAbstractInternal(list.select(1).toString());
        String name;
        switch ( in ){
            case name:
                name = list.select(2).toString();
                if ( !name.matches( INFINITY ) )
                    throw new IllegalArgumentException("Complex _Inert_NAME is not infinity! " + name);
                return new TranslatedExpression(INFINITY, POSITIVE);
            case prod:
                List l1 = (List)list.select(2);
                List l2 = (List)list.select(3);
                Algebraic a = l1.select(2);
                if ( !(a instanceof MString) )
                    throw new IllegalArgumentException("Illegal argument for complex numbers. " + l1 );
                MString mString = (MString)a;
                if ( !mString.stringValue().matches( INFINITY ) )
                    throw new IllegalArgumentException("Not allowed structure for -infinity. " + l1 );
                MapleInternal i = getAbstractInternal( l2.select(1).toString() );
                if ( !i.equals(MapleInternal.intneg) )
                    throw new IllegalArgumentException("Not allowed structure for -infinity. " + l2 );
                return new TranslatedExpression(INFINITY, NEGATIVE);
            case intpos:
            case intneg:
            case floating:
            case rational:
                NumericalParser np = new NumericalParser( in, 2 );
                if (!np.parse(list)) throw new MapleException("Cannot parse rational number.");
                return np.translatedList.merge();
            default: throw new IllegalArgumentException("Illegal argument in complex!");
        }
    }
}
