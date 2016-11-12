package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.parser.components.EmptyExpressionParser;
import gov.nist.drmf.interpreter.cas.parser.components.MathTermParser;
import gov.nist.drmf.interpreter.common.grammar.IParser;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * The abstract parser provides {@link #getTranslatedExpression()}
 * and {@link #getErrorMessage()} for each parser object.
 *
 * A parser object needs to set the {@link #translatedExp} and
 * {@link #errorMessage} due the parse method.
 *
 * @author Andre Greiner-Petter
 */
public abstract class AbstractParser implements IParser {
    public static final String SPACE = " ";

    /**
     * Translated expression.
     */
    protected String translatedExp = "";

    /**
     * Error message
     */
    protected String errorMessage = "";

    private String[] elems;

    private String sequence;

    private boolean innerError = false;

    protected boolean extractMultipleSubExpressions( PomTaggedExpression topExpression ){
        List<PomTaggedExpression> list = topExpression.getComponents();
        elems = new String[list.size()];
        for ( int i = 0; i < list.size(); i++ ){
            elems[i] = parseGeneralExpression(list.get(i));
        }
        return !innerError;
    }

    protected boolean parseSequence( PomTaggedExpression topExpression ){
        sequence = "";
        List<PomTaggedExpression> exp_list = topExpression.getComponents();

        while ( !exp_list.isEmpty() ){
            translatedExp += parseGeneralExpression(exp_list.get(0));
            if ( exp_list.size() > 1 )
                translatedExp += SPACE;
        }
        return !innerError;
    }

    protected String parseGeneralExpression( PomTaggedExpression exp ){
        MathTerm root = exp.getRoot();
        AbstractParser inner_parser;

        inner_parser = (root != null && !root.isEmpty()) ?
                new MathTermParser() :
                new EmptyExpressionParser();

        if ( inner_parser.parse(exp) ){
            return inner_parser.translatedExp;
        } else {
            innerError = true;
            errorMessage += "Cannot parse inner expression! " +
                    inner_parser.getErrorMessage() +
                    System.lineSeparator();
            return SPACE;
        }
    }

    @Override
    public abstract boolean parse(PomTaggedExpression expression);

    @Override
    public String getTranslatedExpression() {
        return translatedExp;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    protected String[] getElements(){
        return elems;
    }

    protected String getSequence(){
        return sequence;
    }

    protected boolean isInnerError(){
        return innerError;
    }
}
