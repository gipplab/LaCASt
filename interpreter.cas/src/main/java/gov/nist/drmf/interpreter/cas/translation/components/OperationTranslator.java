package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class OperationTranslator extends AbstractListTranslator {
    @Override
    public boolean translate( PomTaggedExpression first_exp, List<PomTaggedExpression> following_exp ) {
        MathTerm top = first_exp.getRoot();
        String first = top.getTermText();

        if ( first.matches("[\\\\]?mod") )
            return parseModulo( following_exp );
        else return parseSymbol( top );
    }

    private boolean parseModulo( List<PomTaggedExpression> following_exp ){
        TranslatedExpression divisorExp =
                parseGeneralExpression( following_exp.remove(0), following_exp );
        if ( isInnerError() ) return false;

        while ( !following_exp.isEmpty() && forceNext( following_exp.get(0) ) ){
            TranslatedExpression extended_divisor =
                    parseGeneralExpression( following_exp.remove(0), following_exp );
            String last = extended_divisor.getLastExpression();
            if ( last == null ) {
                last = global_exp.getLastExpression();
                divisorExp.removeLastExpression();
            }
            extended_divisor.replaceLastExpression( last );
            divisorExp.addTranslatedExpression( extended_divisor );
        }

        // merge the divisor and get number of elements
        int num = divisorExp.mergeAll();

        // delete those last elements from global list
        global_exp.removeLastNExps( num );

        // remove the previous one (which is the dividend)
        String dividend = global_exp.removeLastExpression();
        // and get the string of the divisor
        String divisor  = divisorExp.toString();

        BasicFunctionsTranslator fun = SemanticLatexTranslator.getBasicFunctionParser();
        String[] arguments = new String[]{dividend, divisor};
        String translatedMod = fun.translate( arguments, "modulo" );

        // the given translated expression is one complete phrase
        // so add it to the global lexicon
        global_exp.addTranslatedExpression( translatedMod );

        // since we replaced the last phrase in global_exp
        // global_exp and local_inner_exp are the same. Theoretically
        return true;
    }

    private boolean forceNext( PomTaggedExpression next_exp ){
        if ( containsTerm(next_exp) ){
            String termTag = next_exp.getRoot().getTag();
            MathTermTags tag = MathTermTags.getTagByKey(termTag);
            return tag.equals( MathTermTags.caret ) || tag.equals( MathTermTags.factorial );
        } else return false;
    }

    private boolean parseSymbol( MathTerm term ){
        SymbolTranslator sT = SemanticLatexTranslator.getSymbolsTranslator();
        String translation = sT.translate( term.getTermText() );
        if ( handleNull( translation,
            "Cannot translate operation " + term.getTermText(),
            TranslationException.Reason.UNKNOWN_OPERATION,
            term.getTermText(),
            null) ){
            return true;
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
