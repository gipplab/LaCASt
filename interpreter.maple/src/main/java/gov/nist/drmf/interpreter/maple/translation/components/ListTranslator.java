package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class ListTranslator extends AbstractAlgebraicTranslator<List> {

    private int length;
    private MapleInternal root;

    public ListTranslator(MapleInternal in, int length ) {
        this.root = in;
        this.length = length;
    }

    /**
     *
     * @param list
     * @return
     */
    @Override
    public boolean translate(List list ){
        AbstractAlgebraicTranslator generalParser = null;
        switch( root ){
            case sum:
            case prod:
            case exp:
                generalParser = new SequenceTranslator( root, length );
                break;
            case intpos:
            case intneg:
            case complex:
            case floating:
            case rational:
                generalParser = new NumericalTranslator( root, length );
                break;
            case function:
            case power:
            case name:
            case string:
            case ass_name:
                generalParser = new FunctionAndVariableTranslator( root );
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

        if (!generalParser.translate( list )){
            this.internalErrorLog = generalParser.internalErrorLog;
            return false;
        } else {
            this.translatedList.addTranslatedExpression( generalParser.translatedList );
            return true;
        }
    }
}
