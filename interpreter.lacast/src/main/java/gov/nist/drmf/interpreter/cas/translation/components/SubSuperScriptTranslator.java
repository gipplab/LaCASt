package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.latex.FreeVariables;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SubSuperScriptTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(SubSuperScriptTranslator.class.getName());
    private final TranslatedExpression localTranslations;
    private final SymbolTranslator st;

    public SubSuperScriptTranslator(AbstractTranslator abstractTranslator) {
        super(abstractTranslator);
        this.localTranslations = new TranslatedExpression();
        this.st = this.getConfig().getSymbolTranslator();
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following_exp) {
        MathTerm term = exp.getRoot();
        String termTag = term.getTag();

        if ( ExpressionTags.sub_super_script.equalsPTE(exp) ) {
            return translateSubSuperScripts(exp);
        }

        MathTermTags tag = MathTermTags.getTagByKey(termTag);

        switch (tag) {
            case caret:
                return parseCaret(exp, following_exp);
            case underscore:
                return parseUnderscores(exp);
            default:
                throw TranslationException.buildException(
                        this,
                        "SubSuperScriptTranslator can only translate carets and underscores: " + term.getTermText(),
                        TranslationExceptionReason.IMPLEMENTATION_ERROR
                );
        }
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    private TranslatedExpression translateSubSuperScripts(PomTaggedExpression exp) {
        LinkedList<PomTaggedExpression> components = new LinkedList<>(exp.getComponents());

        translate(components.removeFirst(), components);
        localTranslations.clear();
        return translate(components.removeFirst(), components);
    }

    /**
     * Handle the caret function (power function). The given power
     * is a sub expression than. But there is definitely only one
     * sub expression. Of course, this could be a sequence anyway.
     *
     * @param exp the caret expression, it has one child for sure
     * @return true if everything was fine
     */
    private TranslatedExpression parseCaret(PomTaggedExpression exp, List<PomTaggedExpression> following_exp) {
        Brackets b = Brackets.left_parenthesis;

        boolean replaced = false;
        String base = getGlobalTranslationList().removeLastExpression();
        if (!Brackets.isEnclosedByBrackets(base)) {
            base = b.symbol + base + b.counterpart;
            replaced = true;
        }
        getGlobalTranslationList().addTranslatedExpression(base);

        // get the power
        PomTaggedExpression sub_exp = exp.getComponents().get(0);
        // and translate the power
        TranslatedExpression power = parseGeneralExpression(sub_exp, null);
        String powerStr = wrapParenthesesAndAddMultiplyToPower(power, following_exp);

        // the power becomes one big expression now.
        localTranslations.addTranslatedExpression(powerStr);
        localTranslations.addAutoMergeLast(1);

        // remove all elements added from the power process
        getGlobalTranslationList().removeLastNExps(power.clear());
        // and add the power with parenthesis (and the caret)
        getGlobalTranslationList().addTranslatedExpression(powerStr);
        // merges last 2 expression, because after ^ it is one phrase than
        getGlobalTranslationList().mergeLastNExpressions(2);

        if (replaced) {
            localTranslations.removeLastExpression();
        }
        return localTranslations;
    }

    private String wrapParenthesesAndAddMultiplyToPower(TranslatedExpression power, List<PomTaggedExpression> following_exp) {
        // now we need to wrap parenthesis around the power
        String powerStr = st.translateFromMLPKey(MathTermTags.caret.tag());
        if (!Brackets.isEnclosedByBrackets(power.toString())) {
            powerStr += Brackets.left_parenthesis.symbol + power.toString() + Brackets.left_parenthesis.counterpart;
        } else {
            powerStr += power.toString();
        }

        MathTerm m = FakeMLPGenerator.generateClosedParenthesesMathTerm();
        PomTaggedExpression last = new PomTaggedExpression(m);
        if (AbstractListTranslator.addMultiply(last, following_exp)) {
            powerStr += getConfig().getMULTIPLY();
        }

        return powerStr;
    }

    /**
     * Handle an underscore to indexing expressions.
     *
     * @param exp the underscore expression, it has child, the subscript expression.
     * @return true if everything was fine.
     */
    private TranslatedExpression parseUnderscores(PomTaggedExpression exp) {
        // first of all, remove the previous expression. It becomes a whole new block.
        String var = getGlobalTranslationList().removeLastExpression();
        boolean wasVariable = getGlobalTranslationList().getFreeVariables().removeLastVariable(var);

        // get the subscript expression and translate it.
        PomTaggedExpression subscript_exp = exp.getComponents().get(0);
        TranslatedExpression subscript = parseGeneralExpression(subscript_exp, null);

        // pack it into a list of arguments. The first one is the expression with an underscore
        // the second is the underscore expression itself.
        String[] args = new String[] {
                stripMultiParentheses(var),
                stripMultiParentheses(subscript.getTranslatedExpression())
        };

        // remove the mess from getGlobalTranslationList() and local_exp
        getGlobalTranslationList().removeLastNExps(subscript.clear());
        localTranslations.clear();

        // finally translate it as a function
        BasicFunctionsTranslator parser = getConfig().getBasicFunctionsTranslator();
        String translation = parser.translate(args, Keys.MLP_KEY_UNDERSCORE);

        // keep the localTranslations clean to show we need to take the getGlobalTranslationList()
        //localTranslations.addTranslatedExpression( translation );

        // add our final representation for subscripts to the global lexicon
        getGlobalTranslationList().addTranslatedExpression(translation);
        if ( wasVariable ) {
            mapPerform(TranslatedExpression::getFreeVariables, FreeVariables::addFreeVariable, translation);
        }
        return localTranslations;
    }
}
