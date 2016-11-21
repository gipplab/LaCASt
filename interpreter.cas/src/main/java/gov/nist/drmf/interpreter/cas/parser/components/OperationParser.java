package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class OperationParser extends AbstractListParser {
    @Override
    public boolean parse( PomTaggedExpression first_exp, List<PomTaggedExpression> following_exp ) {
        MathTerm top = first_exp.getRoot();
        String first = top.getTermText();

        if ( first.matches("[\\\\]?cdot") ){
            return parseSymbol( top );
        } else if ( !first.matches("[\\\\]?mod") ){
            ERROR_LOG.severe("Cannot parse unknown operation " + first);
            return false;
        }

        TranslatedExpression divisorExp =
                parseGeneralExpression( following_exp.remove(0), following_exp );
        if ( isInnerError() ) return false;

        // merge the divisor and get number of elements
        int num = divisorExp.mergeAllWithParenthesis();
        // delete those last elements from global list
        global_exp.removeLastNExps( num );

        // remove the previous one (which is the dividend)
        String dividend = global_exp.removeLastExpression();
        // and get the string of the divisor
        String divisor  = divisorExp.toString();

        BasicFunctionsTranslator fun = SemanticLatexParser.getBasicFunctionParser();
        String[] arguments = new String[]{dividend, divisor};
        String translatedMod = fun.translate( arguments, "modulo" );

        // the given translated expression is one complete phrase
        // so add it to the global lexicon
        global_exp.addTranslatedExpression( translatedMod );

        // since we replaced the last phrase in global_exp
        // global_exp and local_inner_exp are the same. Theoretically
        return true;
    }

    private boolean parseSymbol( MathTerm term ){
        SymbolTranslator sT = SemanticLatexParser.getSymbolsTranslator();
        String translation = sT.translate( term.getTermText() );
        if ( translation == null ){
            ERROR_LOG.warning("Cannot parse operation " + term.getTermText());
            return false;
        } else {
            INFO_LOG.addGeneralInfo(
                    term.getTermText(),
                    "was translated to " + translation);
            local_inner_exp.addTranslatedExpression( translation );
            global_exp.addTranslatedExpression( translation );
            return true;
        }
    }
}
