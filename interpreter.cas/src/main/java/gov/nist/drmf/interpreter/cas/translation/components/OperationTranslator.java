package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public class OperationTranslator extends AbstractListTranslator {

    private TranslatedExpression localTranslations;

    public OperationTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

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
                last = getGlobalTranslationList().getLastExpression();
                divisorExp.removeLastExpression();
            }
            extended_divisor.replaceLastExpression( last );
            divisorExp.addTranslatedExpression( extended_divisor );
        }

        // merge the divisor and get number of elements
        int num = divisorExp.mergeAll();

        // delete those last elements from global list
        getGlobalTranslationList().removeLastNExps( num );

        // remove the previous one (which is the dividend)
        String dividend = getGlobalTranslationList().removeLastExpression();
        Matcher m = DLMFPatterns.ENDS_ON_STAR_PATTERN.matcher(dividend);
        if ( m.matches() ){
            dividend = m.group(1);
        }

        // and get the string of the divisor
        String divisor  = divisorExp.toString();

        BasicFunctionsTranslator fun = getConfig().getBasicFunctionsTranslator();
        String[] arguments = new String[]{
                stripMultiParentheses(dividend),
                stripMultiParentheses(divisor)
        };
        String translatedMod = fun.translate( arguments, "modulo" );

        // the given translated expression is one complete phrase
        // so add it to the global lexicon
        getGlobalTranslationList().addTranslatedExpression( translatedMod );

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
        SymbolTranslator sT = getConfig().getSymbolTranslator();
        String translation = sT.translate( term.getTermText() );
        if ( handleNull( translation,
            "Cannot translate operation " + term.getTermText(),
            TranslationException.Reason.UNKNOWN_OPERATION,
            term.getTermText(),
            null) ){
            return true;
        } else {
            getInfoLogger().addGeneralInfo(
                    term.getTermText(),
                    "was translated to: " + translation);
            localTranslations.addTranslatedExpression( translation );
            getGlobalTranslationList().addTranslatedExpression( translation );
            return true;
        }
    }
}
