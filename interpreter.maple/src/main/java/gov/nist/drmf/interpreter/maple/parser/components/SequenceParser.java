package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;

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
        String translation = "";
        switch ( internal ){
            case sum:
                for ( int i = 2; i < length; i++ ){

                }
                break;
            case prod:
                break;
            case exp:
                break;
            default:
                return false;
        }

        return false;
    }

    @Override
    public String getTranslatedExpression() {
        return null;
    }
}
