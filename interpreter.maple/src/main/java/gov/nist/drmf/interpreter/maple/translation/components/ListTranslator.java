package gov.nist.drmf.interpreter.maple.translation.components;

import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.wrapper.MapleList;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class ListTranslator extends AbstractAlgebraicTranslator<MapleList> {

    protected int length;
    protected MapleInternal root;

    ListTranslator( MapleInternal in, int length ) {
        this.root = in;
        this.length = length;
    }

    /**
     *
     * @param list
     * @return
     */
    @Override
    public Boolean translate( MapleList list ) throws TranslationException {
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
            case divide:
                generalParser = new FunctionAndVariableTranslator( root, length );
                break;
            case equation:
            case ineq:
            case lesseq:
            case lessthan:
                generalParser = new RelationTranslator( root, length );
                break;
            case imply:
            case not:
            case or:
            case xor:
            case set:
            default:
                String message = "Found a not yet supported algebraic object: " + root;
                LOG.debug(message);
                failures.addFailure( message, ListTranslator.class, root.toString() );
                return false;
        }

        @SuppressWarnings("unchecked")
        boolean b = generalParser.translate(list);
        translatedList.addTranslatedExpression( generalParser.translatedList );
        return b;
    }
}
