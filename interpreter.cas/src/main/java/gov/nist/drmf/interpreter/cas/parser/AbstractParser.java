package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.parser.components.EmptyExpressionParser;
import gov.nist.drmf.interpreter.cas.parser.components.MathTermParser;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.IParser;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.FeatureSet;
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

    public static final String PARANTHESIS_PATTERN =
            "(right|left)[-\\s](parenthesis|bracket|brace)";

    /**
     * Translated expression.
     */
    protected String translatedExp = "";

    /**
     *
     */
    protected String extraInformation = "";

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
            elems[i] = parseGeneralExpression(list.get(i), null); // TODO
        }
        return !innerError;
    }

    protected boolean parseSequence( PomTaggedExpression topExpression ){
        sequence = "";
        List<PomTaggedExpression> exp_list = topExpression.getComponents();

        while ( !exp_list.isEmpty() ){
            PomTaggedExpression exp = exp_list.remove(0);
            translatedExp += parseGeneralExpression( exp, exp_list );
            if ( exp_list.size() >= 1 ){
                if ( ( !exp_list.get(0).getRoot().isEmpty() &&
                        exp_list.get(0).getRoot().getTag().matches(PARANTHESIS_PATTERN) )
                    ||
                        ( !exp.getRoot().isEmpty() &&
                        exp.getRoot().getTag().matches(PARANTHESIS_PATTERN) ) ){

                } else translatedExp += SPACE;
            }
        }
        return !innerError;
    }

    protected String parseGeneralExpression(
            PomTaggedExpression exp,
            List<PomTaggedExpression> exp_list){
        MathTerm root = exp.getRoot();
        AbstractParser inner_parser;


        if ( root != null && !root.isEmpty() ){
            if ( root.getTag().matches(MathTermTags.command.tag()) &&
                    root.getNamedFeatureSet(MacroParser.DLMF_FEATURE_NAME) != null ){
                FeatureSet fset = root.getNamedFeatureSet(MacroParser.DLMF_FEATURE_NAME);
                String areas = DLMFFeatureValues.areas.getFeatureValue(fset);
                if ( !areas.matches("special functions") ) {
                    inner_parser = new MathTermParser();
                } else {
                    MacroParser macroP = new MacroParser();
                    boolean a = macroP.parse(exp);
                    boolean b = macroP.parse(exp_list);
                    if ( a && b ) {
                        extraInformation += macroP.extraInformation;
                        return macroP.getTranslatedExpression();
                    } else {
                        innerError = true;
                        errorMessage += macroP.getErrorMessage() +
                                System.lineSeparator();
                        return SPACE;
                    }
                }
            } else inner_parser = new MathTermParser();
        } else inner_parser = new EmptyExpressionParser();

        if ( inner_parser.parse(exp) ){
            extraInformation += inner_parser.extraInformation;
            return inner_parser.translatedExp;
        } else {
            innerError = true;
            errorMessage += inner_parser.getErrorMessage() +
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

    @Override
    public String getExtraInformation() {
        return this.extraInformation;
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
