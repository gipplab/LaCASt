package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
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

        String output;
        if ( term.getTermText().startsWith("\\") )
            output = term.getTermText().substring(1);
        else output = term.getTermText();

        innerTranslatedExp.addTranslatedExpression(output);
        last_exp = innerTranslatedExp.getLastExpression();
        global_exp.addTranslatedExpression(output);

        INFO_LOG.addGeneralInfo(
                term.getTermText(),
                "Function without DLMF-Definition. " +
                        "We cannot translate it and keep it like it is (but delete prefix \\ if necessary)."
        );
        return true;
    }

    @Override
    public boolean parse(List<PomTaggedExpression> following_exp) {
        PomTaggedExpression first = following_exp.remove(0);
        boolean caret = false;

        if ( containsTerm(first) ){
            MathTermTags tag = MathTermTags.getTagByKey(first.getRoot().getTag());
            if (tag.equals( MathTermTags.caret )){
                caret = true;
                parseGeneralExpression(first, following_exp);
            }
        }

        first = following_exp.remove(0);
        TranslatedExpression translation = parseGeneralExpression(first, following_exp);

        String startPatter = "\\s*" + Brackets.OPEN_PATTERN + ".*";
        if ( !translation.toString().matches(startPatter) ){
            innerTranslatedExp.addTranslatedExpression(
                    Brackets.left_parenthesis.symbol +
                            translation.toString() +
                            Brackets.left_parenthesis.counterpart
            );
        }
        else innerTranslatedExp.addTranslatedExpression(translation.toString());

        if ( caret ){
            String arg = global_exp.removeLastExpression();
            String power = global_exp.removeLastExpression();
            global_exp.addTranslatedExpression(innerTranslatedExp.getLastExpression() + power);
        }

        last_exp = innerTranslatedExp.getLastExpression();
        return true;
    }
}
