package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class ListParser extends AbstractAlgebraicParser<List> {

    private int length;
    private MapleInternal root;

    public ListParser( MapleInternal in, int length ) {
        this.root = in;
        this.length = length;
    }

    /**
     *
     * @param list
     * @return
     */
    @Override
    public boolean parse( List list ){
        AbstractAlgebraicParser generalParser = null;
        switch( root ){
            case sum:
            case prod:
            case exp:
                generalParser = new SequenceParser( root, length );
                break;
            case intpos:
            case intneg:
            case complex:
            case floating:
            case rational:
                generalParser = new NumericalParser( root, length );
                break;
            case function:
            case power:
            case name:
            case string:
            case ass_name:
                generalParser = new FunctionAndVariableParser( root );
                break;
            case equation:
                break;
            case ineq:
                break;
            case lesseq:
                break;
            case lessthan:
                break;
            case imply:
                break;
            case not:
                break;
            case or:
                break;
            case xor:
                break;
            case set:
                break;
        }

        if ( generalParser == null )
            return false;

        if (!generalParser.parse( list )){
            this.internalErrorLog = generalParser.internalErrorLog;
            return false;
        } else {
            this.translatedList.addTranslatedExpression( generalParser.translatedList );
            return true;
        }
    }
}
