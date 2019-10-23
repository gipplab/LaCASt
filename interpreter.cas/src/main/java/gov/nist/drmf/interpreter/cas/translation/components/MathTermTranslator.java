package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TranslationException;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * The math term translation parses only math terms.
 * It is a inner translation and switches through all different
 * kinds of math terms. All registered math terms can be
 * found in {@link MathTermTags}.
 *
 * @see AbstractTranslator
 * @see Constants
 * @see GreekLetters
 * @see MathTermTags
 * @author Andre Greiner-Petter
 */
public class MathTermTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(MathTermTranslator.class.getName());

    @Override
    public boolean translate(PomTaggedExpression exp ){
        return translate( exp, new LinkedList<>() );
    }

    /**
     * This translation only parses MathTerms. Only use this
     * when your expression has a non-empty term and
     * cannot translate by any other specialized translation!
     *
     * @param exp has a not empty term!
     * @param following_exp
     * @return true when everything is fine and there was no error
     */
    @Override
    public boolean translate( PomTaggedExpression exp, List<PomTaggedExpression> following_exp ) {
        // it has to be checked before that this exp has a not empty term
        // get the MathTermTags object
        MathTerm term = exp.getRoot();
        String termTag = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(termTag);
        SymbolTranslator sT = SemanticLatexTranslator.getSymbolsTranslator();

        // if the tag doesn't exists in the system -> stop
        if ( handleNull( tag,
            "Unknown MathTerm tag: ",
            TranslationException.Reason.UNKNOWN_MATHTERM_TAG,
            termTag,
            null )){
            return  true;
        }

        // get the feature set for a constant, if this expression has one
        // could be null of course
        @Nullable
        FeatureSet constantSet =
                FeatureSetUtility.getSetByFeatureValue(
                        term,
                        Keys.FEATURE_ROLE,
                        Keys.FEATURE_VALUE_CONSTANT
                );

        String translation;
        // otherwise switch due all cases
        switch( tag ){
            case dlmf_macro:
                // a dlmf-macro at this state will be simple translated as a command
                // so do nothing here and switch to command:
            case command:
                // a latex-command could be:
                //  1) Greek letter -> translate via GreekLetters.translate
                //  2) constant     -> translate via Constants.translate
                //  3) A DLMF Macro -> this translation cannot handle DLMF-Macros
                //  4) a function   -> Should parsed by FunctionTranslator and not here!

                // is it a Greek letter?
                if ( FeatureSetUtility.isGreekLetter(term) ){
                    // is this Greek letter also known constant?
                    if ( constantSet != null ){
                        // inform the user about our choices
                        constantVsLetter( constantSet, term );
                    } // if not, simply translate it as a Greek letter
                    return parseGreekLetter(term.getTermText());
                }

                // or is it a constant but not a Greek letter?
                if ( constantSet != null ){
                    // simply try to translate it as a constant
                    return parseMathematicalConstant( constantSet, term.getTermText() );
                }

                String t = sT.translate( term.getTermText() );
                if ( t != null ) {
                    INFO_LOG.addGeneralInfo(
                            term.getTermText(),
                            "was translated to " + t);
                    local_inner_exp.addTranslatedExpression( t );
                    global_exp.addTranslatedExpression( t );
                    return true;
                }

                // no it is a DLMF macro or function
                FeatureSet macro = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
                if ( macro != null ){
                    throw new TranslationException(
                            "MathTermTranslator cannot translate DLMF-Macro: " +
                            term.getTermText(),
                            TranslationException.Reason.UNKNOWN_MACRO,
                            term.getTermText());
                }

                // not all greek letters are in the global lexicon
                // so try again to translate it as a greek letter, just try...
                try {
                    return parseGreekLetter(term.getTermText());
                } catch ( TranslationException te ){
                    if ( handleNull( null,
                        "Reached unknown latex-command " + term.getTermText(),
                        TranslationException.Reason.UNKNOWN_LATEX_COMMAND,
                        term.getTermText(),
                        te ) ) {
                        return true;
                    }
                }
            case special_math_letter:
            case symbol:
                String sym = sT.translate( term.getTermText() );
                if ( sym != null ) {
                    INFO_LOG.addGeneralInfo(
                            term.getTermText(),
                            "was translated to " + sym);
                    local_inner_exp.addTranslatedExpression( sym );
                    global_exp.addTranslatedExpression( sym );
                    return true;
                } else {
                    FeatureSet fset = term.getNamedFeatureSet( Keys.KEY_DLMF_MACRO );
                    if ( fset != null ){
                        String trans = DLMFFeatureValues.CAS.getFeatureValue(fset);
                        if ( trans != null ){
                            INFO_LOG.addMacroInfo(
                                    term.getTermText(), "was translated to " + trans );
                            local_inner_exp.addTranslatedExpression(trans);
                            global_exp.addTranslatedExpression(trans);
                            return true;
                        }
                    }

                    throw new TranslationException(
                            "Unknown symbol reached: " + term.getTermText(),
                            TranslationException.Reason.UNKNOWN_SYMBOL);
                }
            case function:
                LOG.error("MathTermTranslator cannot translate functions. Use the FunctionTranslator instead: "
                        + term.getTermText());
                return false;
            case multiply:
                local_inner_exp.addTranslatedExpression(MULTIPLY);
                global_exp.addTranslatedExpression(MULTIPLY);
                return true;
            case letter:
                // a letter can be one constant, but usually translate it simply
                if ( constantSet != null ){
                    // lol a constant
                    if ( parseMathematicalConstant( constantSet, term.getTermText() ) ){
                        return true;
                    }
                }
            case digit:
            case numeric:
            case minus:
            case plus:
            case equals:
            case divide:
            case less_than:
            case greater_than: // all above should translated directly, right?
                local_inner_exp.addTranslatedExpression(term.getTermText());
                global_exp.addTranslatedExpression(term.getTermText());
                return true;
            case left_parenthesis: // the following should not reached!
            case left_bracket:
            case left_brace:
            case right_parenthesis:
            case right_bracket:
            case right_brace:
                LOG.error("MathTermTranslator don't expected brackets but found "
                        + term.getTermText());
                return false;
            case at:
                // simply ignore it...
                return true;
            case constant:
                // a constant in this state is simply not a command
                // so there is no \ in front of the text.
                // that's why a constant here is the same like a alphanumeric expression
                // ==> do nothing and switch to alphanumeric
            case alphanumeric:
                // check first, if it is a constant or Greek letter
                String alpha = term.getTermText();
                if ( FeatureSetUtility.isGreekLetter( term ) ){
                    if ( constantSet != null ){
                        constantVsLetter( constantSet, term );
                    }
                    return parseGreekLetter( alpha );
                }

                if ( constantSet != null ){
                    try {
                        PomTaggedExpression next = following_exp.get(0);
                        MathTerm possibleCaret = next.getRoot();
                        MathTermTags tagPossibleCaret =
                                MathTermTags.getTagByKey(possibleCaret.getTag());
                        if ( alpha.matches("\\\\expe") &&
                                MathTermTags.caret.equals(tagPossibleCaret) ){
                            // translate as exo(...) and not exp(1)^{...}
                            return parseExponentialFunction( following_exp );
                        } else return parseMathematicalConstant( constantSet, alpha );
                    } catch ( Exception e ){
                        return parseMathematicalConstant( constantSet, alpha );
                    }
                }

                String output;
                // add multiplication symbol between all letters
                for ( int i = 0; i < alpha.length()-1; i++ ) {
                    output = alpha.charAt(i) + MULTIPLY;
                    // add it to local and global
                    local_inner_exp.addTranslatedExpression( output );
                    global_exp.addTranslatedExpression(output);
                }

                // add the last one, but without space
                output = ""+alpha.charAt(alpha.length()-1);
                local_inner_exp.addTranslatedExpression( output );
                global_exp.addTranslatedExpression(output);
                return true;
            case comma:
                // in general, translate them directly
                local_inner_exp.addTranslatedExpression(term.getTermText());
                global_exp.addTranslatedExpression(term.getTermText());
                return true;
            case operation:
                OperationTranslator opParser = new OperationTranslator();
                // well, maybe not the best choice
                if ( opParser.translate( exp, following_exp ) ){
                    local_inner_exp.addTranslatedExpression( opParser.getTranslatedExpressionObject() );
                    return true;
                } else return false;
            case factorial:
                String last = global_exp.removeLastExpression();
                BasicFunctionsTranslator translator = SemanticLatexTranslator.getBasicFunctionParser();

                String prefix = "";
                try {
                    PomTaggedExpression next = following_exp.get(0);
                    MathTermTags nextTag = MathTermTags.getTagByKey( next.getRoot().getTag() );
                    if ( nextTag != null && nextTag.equals( tag ) ){
                        following_exp.remove(0);
                        prefix = "double ";
                    }
                } catch ( Exception e ){
                    prefix = "";
                }
                translation = translator.translate(new String[]{last}, prefix+tag.tag());

                // probably we don't have to do anything with the local exp
                //local_inner_exp.addTranslatedExpression( translation );
                global_exp.addTranslatedExpression( translation );
                return true;
            case caret:
                return parseCaret( exp, following_exp );
            case underscore:
                return parseUnderscores( exp );
            case ordinary:
            case ellipsis:
                String symbol;
                if ( tag.equals(MathTermTags.ordinary) )
                    symbol = sT.translate( term.getTermText() );
                else symbol = sT.translateFromMLPKey( tag.tag() );
                local_inner_exp.addTranslatedExpression( symbol );
                global_exp.addTranslatedExpression( symbol );
                return true;
            case macro:
                LOG.warn(
                        "A macro? What is it? Please inform " +
                                "Andre about this crazy shit: " +
                                term.getTermText());
                return false;
            case abbreviation:
                throw new TranslationException(
                        "This program cannot translate abbreviations like " + term.getTermText(),
                        TranslationException.Reason.ABBREVIATION
                );
            case spaces:
            case non_allowed:
                LOG.debug( "Skip controlled space, such as \\!" );
                return true;
            case relation:
                if ( !term.getTermText().matches( ABSOLUTE_VAL_TERM_TEXT_PATTERN ) ){
                    translation = sT.translate( term.getTermText() );
                    if ( translation == null ){
                        LOG.error("Unknown relation. Cannot translate: " + term.getTermText());
                        return false;
                    }
                    local_inner_exp.addTranslatedExpression(translation);
                    global_exp.addTranslatedExpression(translation);
                    return true;
                }
            case left_delimiter:
                if ( !term.getTermText().matches( ABSOLUTE_VAL_TERM_TEXT_PATTERN ) ){
                    LOG.error("Cannot handle delimiters here! Found: " + term.getTermText());
                    return false;
                }
            case fence:
                Brackets start = Brackets.left_latex_abs_val;
                SequenceTranslator sq = new SequenceTranslator(start);
                boolean result = sq.translate( following_exp );
                this.local_inner_exp = sq.getTranslatedExpressionObject();
                return result;
            case right_delimiter:
                LOG.error("Should not reach right-delimiters in MathTermTranslator!");
                return false;
            default:
                throw new TranslationException(
                        "Unknown MathTerm Tag: "
                                + term.getTag(),
                        TranslationException.Reason.UNKNOWN_MATHTERM_TAG
                );
        }
    }

    /**
     * Inform the user about the fact of decision we made to translate this
     * constant or Greek letter.
     *
     * This method only will invoke, if the term is a Greek letter and maybe
     * a constant. So it is maybe a constant we know how to translate but don't
     * want to (like pi but the user should use \cpi instead)
     * or it is a completely unknown constant, then we can translate it as a Greek
     * letter anyway (for instance \alpha, could be the 2nd Feigenbaum constant).
     *
     * @param constantSet the constant feature set
     * @param term the Greek letter term
     */
    private void constantVsLetter( FeatureSet constantSet, MathTerm term ){
        String dlmf = translateToDLMF(term.getTermText());
        if ( dlmf == null ){
            INFO_LOG.addGeneralInfo(
                    term.getTermText(),
                    "Could be " + DLMFFeatureValues.meaning.getFeatureValue(constantSet) + "."
                            + System.lineSeparator() +
                            "But this system don't know how to translate it as a constant. " +
                            "It was translated as a general letter." + System.lineSeparator()
            );
            return;
        }

        INFO_LOG.addGeneralInfo(
                term.getTermText(),
                "Could be " + DLMFFeatureValues.meaning.getFeatureValue(constantSet) + "."
                        + System.lineSeparator() +
                        "But it is also a Greek letter. " +
                        "Be aware, that this program translated the letter " +
                        "as a normal Greek letter and not as a constant!" + System.lineSeparator() +
                        "Use the DLMF-Macro " +
                        dlmf + " to translate " + term.getTermText() + " as a constant." + System.lineSeparator()
        );
    }

    /**
     * Translate a given LaTeX constant (starts with \ or not) to
     * the DLMF macro if possible.
     * @param constant in LaTeX
     * @return null or the DLMF macro for the given latex constant
     */
    @Nullable
    private String translateToDLMF( String constant ){
        Constants c = SemanticLatexTranslator.getConstantsParser();
        if ( !constant.startsWith(CHAR_BACKSLASH) ) constant = CHAR_BACKSLASH + constant;
        return c.translate( Keys.KEY_LATEX, Keys.KEY_DLMF, constant );
    }

    /**
     * Parse the mathematical constant. If there are some problems we try
     * some typical other ways to translate them. Like add \ and translate
     * it to a CAS or to DLMF macro and inform the user about all of them.
     *
     * @param set the feature set with information about the constant
     * @param constant the constant itself
     * @return true if everything was fine
     */
    private boolean parseMathematicalConstant( FeatureSet set, String constant ){
        // get the translation first and try to translate it
        Constants c = SemanticLatexTranslator.getConstantsParser();
        String translated_const = c.translate( constant );

        // if it wasn't translated try some other stuff
        if ( translated_const == null ) {
            // try from LaTeX to CAS
            translated_const = c.translate(Keys.KEY_LATEX, GlobalConstants.CAS_KEY, constant);
            if ( translated_const != null ){
                // if this works, inform the user, that we use this translation now!
                String dlmf = c.translate( Keys.KEY_LATEX, Keys.KEY_DLMF, constant );
                INFO_LOG.addGeneralInfo(
                        constant,
                        "You use a typical letter for a constant [" +
                                DLMFFeatureValues.meaning.getFeatureValue(set) + "]." + System.lineSeparator() +
                                "We keep it like it is! But you should know that " + GlobalConstants.CAS_KEY +
                                " uses " + translated_const + " for this constant." + System.lineSeparator() +
                                "If you want to translate it as a constant, use the corresponding DLMF macro " +
                                dlmf + System.lineSeparator()
                );
                // and now, use this translation
                translated_const = constant;
            }
        }

        // still null? try to translate it as a Greek letter than if possible
        if ( translated_const == null ){
            try {
                String alphabet = set.getFeature( Keys.FEATURE_ALPHABET ).first();
                if ( alphabet.contains( Keys.FEATURE_VALUE_GREEK ) ){
                    INFO_LOG.addGeneralInfo(
                            constant,
                            "Unable to translate " + constant + " [" +
                                    DLMFFeatureValues.meaning.getFeatureValue(set) +
                                    "]. But since it is a Greek letter we translated it to a Greek letter in "
                                    + GlobalConstants.CAS_KEY + "."
                    );
                    return parseGreekLetter( constant );
                } else {
                    throw new TranslationException("Cannot translate mathematical constant " +
                            constant + " - " + set.getFeature(Keys.FEATURE_MEANINGS),
                            TranslationException.Reason.UNKNOWN_MATH_CONSTANT);
                }
            } catch ( NullPointerException npe ){/* ignore it */}
        }

        // anyway, finally we translated it...
        local_inner_exp.addTranslatedExpression( translated_const );
        // add global_exp as well
        global_exp.addTranslatedExpression( translated_const );

        // if there wasn't a translation at all, return true
        if ( translated_const == null ) return true;

        //noinspection ConstantConditions
        if ( translated_const.equals( constant ) ) return true;

        // otherwise inform the user about the translation
        INFO_LOG.addGeneralInfo(
                constant,
                DLMFFeatureValues.meaning.getFeatureValue(set) + " was translated to: " + translated_const
        );
        return true;
    }

    /**
     * Parsing a given Greek letter.
     * @param GreekLetter the Greek letter
     * @return true if it was parsed
     */
    private boolean parseGreekLetter( String GreekLetter ) throws TranslationException {
        // try to translate
        GreekLetters l = SemanticLatexTranslator.getGreekLettersParser();
        String translated_letter = l.translate(GreekLetter);

        // if it's null, maybe a \ is missing
        if ( translated_letter == null ){
            if ( !GreekLetter.startsWith(CHAR_BACKSLASH) ){
                GreekLetter = CHAR_BACKSLASH + GreekLetter;
                translated_letter = l.translate(GreekLetter);
            }
        }

        // still null? inform the user, we cannot do more here
        if ( translated_letter == null ){
            throw new TranslationException("Cannot translate Greek letter "
                    + GreekLetter,
                    TranslationException.Reason.UNKNOWN_GREEK_LETTER);
        }

        // otherwise add all
        local_inner_exp.addTranslatedExpression(translated_letter);
        global_exp.addTranslatedExpression(translated_letter);
        return true;
    }

    private static final String EXPONENTIAL_MLP_KEY = "exponential";

    private boolean parseExponentialFunction( List<PomTaggedExpression> following_exp ){
        PomTaggedExpression caretPomExp = following_exp.remove(0);
        PomTaggedExpression caretChild = caretPomExp.getComponents().get(0);

        TranslatedExpression power = parseGeneralExpression(caretChild, null);
        BasicFunctionsTranslator ft = SemanticLatexTranslator.getBasicFunctionParser();
        String translation = ft.translate( new String[]{power.toString()}, EXPONENTIAL_MLP_KEY );
        global_exp.removeLastNExps( power.clear() );

        local_inner_exp.addTranslatedExpression( translation );
        global_exp.addTranslatedExpression( translation );

        INFO_LOG.addMacroInfo( "\\expe", "Recognizes e with power as the exponential function. " +
                "It was translated as a function." );

        return true;
    }

    /**
     * Handle the caret function (power function). The given power
     * is a sub expression than. But there is definitely only one
     * sub expression. Of course, this could be a sequence anyway.
     *
     * @param exp the caret expression, it has one child for sure
     * @return true if everything was fine
     */
    private boolean parseCaret( PomTaggedExpression exp, List<PomTaggedExpression> following_exp ){
        Brackets b = Brackets.left_parenthesis;

        boolean replaced = false;
        String base = global_exp.removeLastExpression();
        if ( !testBrackets(base) ){
            base = b.symbol + base + b.counterpart;
            replaced = true;
        }
        global_exp.addTranslatedExpression(base);

        // get the power
        PomTaggedExpression sub_exp = exp.getComponents().get(0);
        // and translate the power
        TranslatedExpression power = parseGeneralExpression(sub_exp, null);

        // now we need to wrap parenthesis around the power
        String powerStr = GlobalConstants.CARET_CHAR;
        if ( !testBrackets( power.toString() ) )
                powerStr += b.symbol + power.toString() + b.counterpart;
        else powerStr += power.toString();

        MathTerm m = new MathTerm(")", MathTermTags.right_parenthesis.tag());
        PomTaggedExpression last = new PomTaggedExpression(m);
        if ( AbstractListTranslator.addMultiply( last, following_exp ) )
            powerStr += MULTIPLY;

        // the power becomes one big expression now.
        local_inner_exp.addTranslatedExpression( powerStr );
        local_inner_exp.addAutoMergeLast(1);

        // remove all elements added from the power process
        global_exp.removeLastNExps( power.clear() );
        // and add the power with parenthesis (and the caret)
        global_exp.addTranslatedExpression( powerStr );
        // merges last 2 expression, because after ^ it is one phrase than
        global_exp.mergeLastNExpressions(2);

        if ( replaced ) local_inner_exp.removeLastExpression();

        return !isInnerError();
    }

    /**
     * Handle an underscore to indexing expressions.
     *
     * @param exp the underscore expression, it has child, the subscript expression.
     * @return true if everything was fine.
     */
    private boolean parseUnderscores( PomTaggedExpression exp ){
        // first of all, remove the previous expression. It becomes a whole new block.
        String var = global_exp.removeLastExpression();

        // get the subscript expression and translate it.
        PomTaggedExpression subscript_exp = exp.getComponents().get(0);
        TranslatedExpression subscript = parseGeneralExpression(subscript_exp, null);

        // pack it into a list of arguments. The first one is the expression with an underscore
        // the second is the underscore expression itself.
        String[] args = new String[]{var, subscript.getTranslatedExpression()};

        // remove the mess from global_exp and local_exp
        global_exp.removeLastNExps( subscript.clear() );
        local_inner_exp.clear();

        // finally translate it as a function
        BasicFunctionsTranslator parser = SemanticLatexTranslator.getBasicFunctionParser();
        String translation = parser.translate(args, Keys.MLP_KEY_UNDERSCORE);

        // keep the local_inner_exp clean to show we need to take the global_exp
        //local_inner_exp.addTranslatedExpression( translation );

        // add our final representation for subscripts to the global lexicon
        global_exp.addTranslatedExpression( translation );
        return !isInnerError();
    }
}
