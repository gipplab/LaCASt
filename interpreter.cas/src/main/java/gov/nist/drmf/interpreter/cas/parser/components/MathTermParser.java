package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;

/**
 * @author Andre Greiner-Petter
 */
public class MathTermParser extends AbstractInnerParser {
    @Override
    public boolean parse( MathTerm term ) {
        String tagExp = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(tagExp);

        if ( tag == null ){
            errorMessage += "Could not find term tag: " + tagExp;
            return false;
        }

        switch( tag ){
            case command:
                break;
            case function:
                break;
            case letter:
                break;
            case digit:
                break;
            case numeric:
                break;
            case minus:
                break;
            case plus:
                break;
            case equals:
                break;
            case multiply:
                break;
            case divide:
                break;
            case left_parenthesis:
                break;
            case right_parenthesis:
                break;
            case left_bracket:
                break;
            case right_bracket:
                break;
            case left_brace:
                break;
            case right_brace:
                break;
            case at:
                break;
            case alphanumeric:
                break;
            case comma:
                break;
            case less_than:
                break;
            case greater_than:
                break;
            case mod:
                break;
            case macro:
                break;
        }
        return false;
    }
}
