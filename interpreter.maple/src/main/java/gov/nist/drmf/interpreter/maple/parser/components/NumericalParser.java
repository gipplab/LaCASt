package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import java.util.regex.Matcher;

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
            case rational:
                internalErrorLog += "Not yet implemented.";
                return false;
                /*
                List numerator = (List)list.select(2);
                List denominator = (List)list.select(3);
                Matcher m = MAPLE_INTERNAL_PATTERN.matcher( numerator.select(1).toString() );
                m.matches();
                MapleInternal mi = MapleInternal.getInternal(m.group(1));

                if ( mi.equals( MapleInternal.intneg ) ){
                    translatedExpression += "-";
                }
                translatedExpression +=
                        "\\frac{" +
                                numerator.select(2).toString()
                                + "}{" +
                                denominator.select(2).toString()
                                + "}";
                return true;
                */
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
            Algebraic a = list.select(2);
            return a.toString();
        } catch( MapleException me ){
            internalErrorLog += "Could not parse positive integer!";
            return null;
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
            List first_arg = (List)list.select(2);

            // TODO probably we should change our list into Inert-Form and call FromInert(...)

        } catch ( MapleException me ){
            internalErrorLog += "Cannot calculate given double value.";
            return false;
        }

        internalErrorLog += "Not yet implemented";
        return false;

        /*
        String num_s = parseGeneralExpression(list.select(2));
        String exponent_s = parseGeneralExpression(list.select(3));
        try {
            Algebraic tmp =
                    MapleInterface.evaluateExpression(
                            "evalf("+num_s + "*10^(" + exponent_s + "));"
                    );
            Double d = Double.parseDouble(tmp.toString());
            translatedExpression += d;
            return true;
        } catch ( MapleException me ){
            internalErrorLog += "Cannot calculate given double value.";
            return false;
        }
        */
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

            TranslatedExpression imaginary = new TranslatedExpression("*\\iunit", POSITIVE);
            translatedList.addTranslatedExpression( imaginary );
            return true;
        } catch ( MapleException | IllegalArgumentException me ){
            internalErrorLog += "Cannot parse complex number!";
            return false;
        }
    }

    private TranslatedExpression parseComplexElement( List list )
            throws IllegalArgumentException, MapleException{
        MapleInternal in = getAbstractInternal(list.select(0).toString());
        String name;
        switch ( in ){
            case name:
                name = list.select(2).toString();
                if ( !name.matches( INFINITY ) )
                    throw new IllegalArgumentException("Complex _Inert_NAME is not infinity! " + name);
                return new TranslatedExpression("\\infty", POSITIVE);
            case prod:
                List l1 = (List)list.select(2);
                List l2 = (List)list.select(3);
                if ( !l1.select(2).toString().matches( INFINITY ) )
                    throw new IllegalArgumentException("Not allowed structure for -infinity. " + l1 );
                MapleInternal i = getAbstractInternal( l2.select(2).toString() );
                if ( !i.equals(MapleInternal.intneg) )
                    throw new IllegalArgumentException("Not allowed structure for -infinity. " + l2);
                return new TranslatedExpression("\\infty", NEGATIVE);
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
