package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;

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
                    this.translatedExpression += "(-" + a.toString() + ")";
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
                    break;
                case rational:

                    break;
                default:
                    internalErrorLog += "Not a numerical element! " + root;
                    return false;
            }
        } catch ( MapleException | NullPointerException e ){
            internalErrorLog += e.getMessage();
            return false;
        }
        return false;
    }
}
