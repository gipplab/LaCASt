package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * The function parser parses simple functions and not special functions.
 * These "simple" functions are functions without a DLMF macro.
 *
 * For instance if the global-lexicon doesn't contains the cosine function
 * it is just a simple function than.
 *
 * Like the MacroParser, the function parser should parse the start expression
 * as well (the function itself) and after that the argument.
 *
 * For instance: cos{2}
 *  1) parse the expression cos first
 *  2) after that the list of arguments, here 2
 *
 * @author Andre Greiner-Petter
 */
public class FunctionParser extends AbstractListParser {
    @Override
    public boolean parse(PomTaggedExpression exp){
        MathTerm term = exp.getRoot();

        if ( term.getTermText().startsWith("\\") )
            translatedExp += term.getTermText().substring(1);
        else translatedExp += term.getTermText();

        extraInformation +=
                "Found a function without DLMF-Definition " + term.getTermText() + ". " +
                        "We cannot translate it and keep it like it is (but delete prefix \\ if necessary)." +
                        System.lineSeparator();
        return true;
    }

    @Override
    public boolean parse(List<PomTaggedExpression> following_exp) {
        PomTaggedExpression first = following_exp.remove(0);
        String translation = parseGeneralExpression(first, following_exp);
        String startPatter = "\\s*" + Brackets.OPEN_PATTERN + ".*";
        if ( !translation.matches(startPatter) )
            translatedExp +=
                    Brackets.left_parenthesis.symbol +
                            translation +
                            Brackets.left_parenthesis.counterpart;
        else translatedExp += translation;
        return true;
    }
}
