package gov.nist.drmf.interpreter.maple.parser.components;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.MAPLE_INTERNAL_PATTERN;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import java.util.regex.Matcher;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class NumericalParser extends AbstractAlgebraicParser<List> {

    private MapleInternal root;
    private int length;

    public NumericalParser(MapleInternal internal, int length )
            throws IllegalArgumentException {
        if ( length < 2 || length > 3 )
            throw new IllegalArgumentException(
                    "Numerical objects are only 2 or 3 elements long. This has "
                            + length);

        this.length = length;
        this.root = internal;
    }

    @Override
    public boolean parse( List list ) {
        try {
            Algebraic a;
            switch (root) {
                case intpos:
                    a = list.select(2);
                    this.translatedExpression += a.toString();
                    return true;
                case intneg:
                    a = list.select(2);
                    this.translatedExpression += "-" + a.toString() + "";
                    return true;
                case complex:
                    Algebraic first = list.select(2);
                    if ( length == 3 ){
                        Algebraic second = list.select(3);
                        String im = parseGeneralExpression( second ) + "\\iunit";
                        this.translatedExpression += parseGeneralExpression(first) + " + " + im;
                    } else {
                        this.translatedExpression += parseGeneralExpression(first) + "\\iunit";
                    }
                    return true;
                case floating:
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
                case rational:
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
                default:
                    internalErrorLog += "Not a numerical element! " + root;
                    return false;
            }
        } catch ( MapleException | NullPointerException e ){
            internalErrorLog += e.getMessage();
            return false;
        }
    }
}
