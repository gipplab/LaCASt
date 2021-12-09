package gov.nist.drmf.interpreter.maple.translation.components;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import gov.nist.drmf.interpreter.maple.wrapper.*;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class NumericalTranslator extends ListTranslator {

    NumericalTranslator( MapleInternal internal, int length )
            throws IllegalArgumentException {
        super( internal, length );
        if ( length < 2 || length > 3 ){
            LOG.error( "Illegal number of arguments in NumericalTranslator. " +
                    "Only 2 or 3 arguments are allowed." );
            throw new IllegalArgumentException(
                    "Numerical objects are only 2 or 3 elements long. This has "
                            + length);
        }
    }

    @Override
    public Boolean translate( MapleList list ) throws TranslationException {
        try {
            return innerTranslate( list );
        } catch (MapleException me) {
            throw createException("Maple error in numerical translator.", me);
        }
    }

    public boolean innerTranslate( MapleList list ) throws MapleException, IllegalArgumentException {
        boolean b;
        switch (root) {
            case intpos:
                translatePosInt(list);
                LOG.trace("Translated positive integer. " + translatedList.getLastExpression());
                return true;
            case intneg:
                translateNegInt(list);
                LOG.trace("Translated negative integer. " + translatedList.getLastExpression());
                return true;
            case complex:
                b = parseComplexNumber(list);
                LOG.trace("Translated complex number. " + translatedList.getLastExpression());
                return b;
            case floating:
                b = parseFloatingNumber(list);
                LOG.trace("Translated floating number. " + translatedList.getLastExpression());
                return b;
            case rational:
                parseRationalNumber(list);
                LOG.trace("Translated rational number. " + translatedList.getLastExpression());
                return true;
            default:
                String message = "Expected an Numeric object but get: " + root;
                LOG.debug( message );
                failures.addFailure( message, this.getClass(), root.toString() );
                return false;
        }
    }

    private void translatePosInt( MapleList list ) throws MapleException {
        String s = translateInt( list );
        translatedList.addTranslatedExpression(s);
    }

    private void translateNegInt( MapleList list ) throws MapleException {
        String s = translateInt( list );
        translatedList.addTranslatedExpression(MINUS_SIGN + s);
    }

    private String translateInt( MapleList list ) throws MapleException {
        try {
            Numeric n = Numeric.cast(list.select(2));
            return Integer.toString( n.intValue() );
        } catch( MapleException me ){
            LOG.fatal( "Cannot parse integer.", me );
            throw me;
        }
    }

    /**
     * Parse a fraction of two numerical expressions.
     * @param list Type: RATIONAL, Struct: [RATIONAL, Numeric, Numeric]
     * @throws MapleException extract elements from list or parse elements
     */
    private void parseRationalNumber( MapleList list ) throws MapleException {
        try {
            // get numerator and denominator
            MapleList numerator = MapleList.cast(list.select(2));
            MapleList denominator = MapleList.cast(list.select(3));

            // translate them into a string representation
            String num = translateInt( numerator );
            String denom = translateInt( denominator );

            // get the pattern for fraction from the function translator
            // and replace the place holders by numerator and denominator.
            MapleTranslator mi = MapleTranslator.getDefaultInstance();
            BasicFunctionsTranslator funcTrans = mi.getBasicFunctionsTranslator();
            String[] args = new String[]{num, denom};
            String pattern = funcTrans.translate( args, Keys.MLP_KEY_FRACTION );

            // put translation into list of translated expressions
            TranslatedExpression t;
            MapleInternal num_internal = getAbstractInternal(numerator.select(1).toString());
            if ( num_internal.equals( MapleInternal.intneg ) ){
                t = new TranslatedExpression( pattern, NEGATIVE );
            } else if ( num_internal.equals( MapleInternal.intpos ) ){
                t = new TranslatedExpression( pattern, POSITIVE );
            } else throw new MapleException("Illegal argument in rational object. " + numerator);
            translatedList.addTranslatedExpression( t );
        } catch ( MapleException e ){
            LOG.fatal("Cannot translate a rational number!", e);
            throw e;
        }
    }

    /**
     * A floating number has 2 arguments for sure.
     * First argument is always an integer: INTPOS or INTNEG.
     * The second argument could be an integer or a name for UNDEFINED or INFINITY.
     * @param list
     * @return
     */
    private boolean parseFloatingNumber( MapleList list ) throws MapleException {
        try{
            Algebraic first = list.select(2);
            if ( !(first instanceof Numeric) ){
                String msg =
                        "A floating number is expected to be VERBATIM with openmaple.Numeric object " +
                                "in the second argument but got: " + first;
                failures.addFailure( msg, this.getClass(), list.toString() );
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
            LOG.error("Cannot calculate given double value.", me);
            throw me;
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
    private boolean parseComplexNumber( MapleList list ) throws MapleException, IllegalArgumentException {
        try {
            TranslatedExpression first = parseComplexElement( MapleList.cast(list.select(2)) );
            if ( first == null ) return false;
            translatedList.addTranslatedExpression(first);

            if ( list.length() == 3 ){
                TranslatedExpression second = parseComplexElement( MapleList.cast(list.select(3)) );
                if ( second == null ) return false;
                translatedList.addTranslatedExpression(PLUS_SIGN);
                translatedList.addTranslatedExpression(second);
            }

            LOG.debug("Complex-Number: " + translatedList);

            MapleTranslator mi = MapleTranslator.getDefaultInstance();
            Constants constants = mi.getConstantsTranslator();
            String i_unit = constants.translate(MapleConstants.I_UNIT);

            TranslatedExpression last = translatedList.removeLastExpression();
            TranslatedExpression imaginary;

            if ( last.toString().matches("\\[*-?1]*") ){
                imaginary = new TranslatedExpression( i_unit, last.getSign() );
            } else {
                translatedList.addTranslatedExpression(last);
                imaginary = new TranslatedExpression( MULTIPLY+i_unit , POSITIVE);
            }

            translatedList.addTranslatedExpression( imaginary );
            return true;
        } catch ( MapleException | IllegalArgumentException me ){
            LOG.fatal("Cannot translate complex number! " + me.getMessage(), me);
            return false;
        }
    }

    private TranslatedExpression parseComplexElement( MapleList list )
            throws IllegalArgumentException, MapleException {
        MapleInternal in = getAbstractInternal(list.select(1).toString());
        String name;

        switch ( in ){
            case name:
                name = list.select(2).toString();
                if ( !name.matches( INFINITY ) ){
                    failures.addFailure( "Complex _Inert_NAME is not infinity!", this.getClass(), name );
                    return null;
                } else return new TranslatedExpression(INFINITY, POSITIVE);
            case prod:
                MapleList l1 = MapleList.cast(list.select(2));
                MapleList l2 = MapleList.cast(list.select(3));
                Algebraic a = l1.select(2);
                if ( !(MString.isInstance(a)) ){
                    failures.addFailure( "Illegal argument for complex numbers.", this.getClass(), l1.toString() );
                    return null;
                }
                MString mString = MString.cast(a);
                if ( !mString.stringValue().matches( INFINITY ) ){
                    failures.addFailure( "Not allowed structure for -infinity. ", this.getClass(), l1.toString() );
                    return null;
                }
                MapleInternal i = getAbstractInternal( l2.select(1).toString() );
                if ( !i.equals(MapleInternal.intneg) ){
                    failures.addFailure( "Not allowed structure for -infinity. ", this.getClass(), l2.toString() );
                    return null;
                }
                return new TranslatedExpression(INFINITY, NEGATIVE);
            case intpos:
            case intneg:
            case floating:
            case rational:
                NumericalTranslator np = new NumericalTranslator( in, 2 );
                if (!np.translate(list))
                    return null;
                // TODO risky?
                return np.translatedList;
            default:
                LOG.warn("Illegal argument in complex number. " + in);
                failures.addFailure( "Unkown element in a complex number!", this.getClass(), list.toString() );
                return null;
        }
    }
}
