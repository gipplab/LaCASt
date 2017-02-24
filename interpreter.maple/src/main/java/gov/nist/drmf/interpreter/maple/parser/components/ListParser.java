package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;

import java.util.regex.Matcher;

/**
 * Created by AndreG-P on 22.02.2017.
 */
public class ListParser extends AbstractAlgebraicParser<List> {

    private int length;
    private MapleInternal root;

    public ListParser( String root, int length ) throws IllegalArgumentException {
        Matcher match = PATTERN.matcher(root);
        if ( !match.matches() )
            throw new IllegalArgumentException("Unknown name of maple object: " + root);

        this.root = MapleInternal.getInternal( match.group(1) );
        if ( root == null )
            throw new IllegalArgumentException("Not supported maple object: " + root);

        this.length = length;
    }

    /**
     *
     * @param list
     * @return
     */
    @Override
    public boolean parse( List list ){
        switch( root ){
            case sum:
            case prod:
            case exp:
                SequenceParser sparser = new SequenceParser( root, length );
                if (!sparser.parse( list )){
                    this.internalErrorLog = sparser.internalErrorLog;
                    return false;
                } else {
                    this.translatedExpression += sparser.translatedExpression;
                    return true;
                }
            case intpos:
            case intneg:
            case complex:
            case floating:
            case rational:
                NumericalParser nparser = new NumericalParser( root, length );
                if (!nparser.parse( list )){
                    this.internalErrorLog = nparser.internalErrorLog;
                    return false;
                } else {
                    this.translatedExpression += nparser.translatedExpression;
                    return true;
                }
            case power:
                break;
            case function:
                break;
            case ass_name:
                break;
            case name:
                break;
            case string:
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

        return false;
    }
}