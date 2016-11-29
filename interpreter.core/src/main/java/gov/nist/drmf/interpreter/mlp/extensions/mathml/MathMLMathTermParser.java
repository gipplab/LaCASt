package gov.nist.drmf.interpreter.mlp.extensions.mathml;

import com.sun.istack.internal.Nullable;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;

/**
 * The math term parser parses only math terms.
 * It is a inner parser and switches through all different
 * kinds of math terms. All registered math terms can be
 * found in {@link MathTermTags}.
 *
 * @author Andre Greiner-Petter
 */
public class MathMLMathTermParser {
    // some special characters which are useful for this parser
    // the caret uses for powers
    public static final String CHAR_CARET = "^";

    public String parse( PomTaggedExpression exp, List<PomTaggedExpression> following_exp ) {
        // it has to be checked before that this exp has a not empty term
        // get the MathTermTags object
        MathTerm term = exp.getRoot();
        String termTag = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(termTag);

        if ( tag == null ) return null;

        // otherwise switch due all cases
        switch( tag ){
            case dlmf_macro:
                break;
            case constant:
                break;
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
            case left_delimiter:
                break;
            case right_delimiter:
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
            case macro:
                break;
            case caret:
                break;
            case factorial:
                break;
            case operation:
                break;
            case ellipsis:
                break;
            case abbreviation:
                break;
        }
        return "";
    }
}
