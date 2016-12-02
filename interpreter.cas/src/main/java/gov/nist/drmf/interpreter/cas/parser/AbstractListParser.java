package gov.nist.drmf.interpreter.cas.parser;

import com.sun.istack.internal.Nullable;
import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO
 *
 * @author Andre Greiner-Petter
 */
public abstract class AbstractListParser extends AbstractParser {
    // Array of parsed strings
    protected String[] components;

    /**
     * Use this method only when you know what you are doing.
     *
     * @param exp single expression gets wrapped into a list
     * @return true if the parsing process finished correctly
     */
    @Override
    public boolean parse(PomTaggedExpression exp){
        List<PomTaggedExpression> list = new LinkedList<>();
        list.add(exp);
        return parse(exp);
    }

    /**
     *
     * @param exp
     * @param following_exp
     * @return
     */
    public abstract boolean parse( PomTaggedExpression exp, List<PomTaggedExpression> following_exp );

    /**
     * The general method to parse a list of descendants.
     * The list should not contain the first element.
     *
     * For instance, let us assume our list contains:
     *      ( 2 + 3 )
     * Than this method should parse only the following commands:
     *        2 + 3 )
     *
     * @param following_exp the descendants of a previous expression
     * @return true if the parsing process finished successful
     */
    //public abstract boolean parse(List<PomTaggedExpression> following_exp);

    /**
     * Returns parsed components. Be aware this could be null or
     * could contain older results!
     * @return array of components.
     */
    @Nullable
    public String[] getComponents(){
        return components;
    }
}
