package gov.nist.drmf.interpreter.cas.parser.components;

import com.sun.istack.internal.Nullable;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
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
 * @see gov.nist.drmf.interpreter.cas.parser.AbstractParser
 * @see Constants
 * @see GreekLetters
 * @see MathTermTags
 * @author Andre Greiner-Petter
 */
public class MathTermParser extends AbstractListParser {
    // some special characters which are useful for this parser
    // the caret uses for powers
    public static final String CHAR_CARET = "^";

    @Override
    public boolean parse( PomTaggedExpression exp ){
        return parse( exp, new LinkedList<>() );
    }

    /**
     * This parser only parses MathTerms. Only use this
     * when your expression has a non-empty term and
     * cannot parse by any other specialized parser!
     *
     * @param exp has a not empty term!
     * @param following_exp
     * @return true when everything is fine and there was no error
     */
    @Override
    public boolean parse( PomTaggedExpression exp, List<PomTaggedExpression> following_exp ) {
        // it has to be checked before that this exp has a not empty term
        // get the MathTermTags object
        MathTerm term = exp.getRoot();
        String termTag = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(termTag);

        // if the tag doesn't exists in the system -> stop
        if ( tag == null ){
            ERROR_LOG.warning("Unknown tag: " + termTag);
            return false;
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

        // otherwise switch due all cases
        switch( tag ){
            case dlmf_macro:
                // a dlmf-macro at this state will be simple translated as a command
                // so do nothing here and switch to command:
            case command:
                // a latex-command could be:
                //  1) greek letter -> translate via GreekLetters.translate
                //  2) constant     -> translate via Constants.translate
                //  3) A DLMF Macro -> this parser cannot handle DLMF-Macros
                //  4) a function   -> Should parsed by FunctionParser and not here!

                // is it a greek letter?
                if ( FeatureSetUtility.isGreekLetter(term) ){
                    // is this greek letter also known constant?
                    if ( constantSet != null ){
                        // inform the user about our choices
                        constantVsLetter( constantSet, term );
                    } // if not, simply translate it as a greek letter
                    return parseGreekLetter(term.getTermText());
                }

                // or is it a constant but not a greek letter?
                if ( constantSet != null ){
                    // simply try to translate it as a constant
                    return parseMathematicalConstant( constantSet, term.getTermText() );
                }

                // no it is a DLMF macro or function
                FeatureSet macro = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
                if ( macro != null ){
                    ERROR_LOG.severe("MathTermParser cannot parse DLMF-Macro: " +
                            term.getTermText());
                } else {
                    ERROR_LOG.severe("Reached unknown latex-command " +
                            term.getTermText());
                }
                return false;
            case function:
                ERROR_LOG.severe("MathTermParser cannot parse functions. Use the FunctionParser instead: "
                        + term.getTermText());
                return false;
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
            case multiply:
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
                ERROR_LOG.severe("MathTermParser don't expected brackets but found "
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
                // check first, if it is a constant or greek letter
                String alpha = term.getTermText();
                if ( FeatureSetUtility.isGreekLetter( term ) ){
                    if ( constantSet != null ){
                        constantVsLetter( constantSet, term );
                    }
                    return parseGreekLetter( alpha );
                }
                if ( constantSet != null ){
                    return parseMathematicalConstant( constantSet, alpha );
                }

                // maple, simply add spaces between all letters
                // maybe there has to be other ways for other CAS?
                String output;
                // add space to all objects except the last one
                for ( int i = 0; i < alpha.length()-1; i++ ) {
                    output = alpha.charAt(i) + " ";
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
                // ignore?
                return true;
            case operation:
                OperationParser opParser = new OperationParser();
                // well, maybe not the best choice
                if ( opParser.parse( exp, following_exp ) ){
                    /*String transExp = opParser.getTranslatedExpressionObject().removeLastExpression();
                    if ( transExp == null ){
                        transExp = global_exp.getLastExpression();
                    }*/
                    local_inner_exp.addTranslatedExpression( opParser.getTranslatedExpressionObject() );
                    return true;
                } else return false;
            case factorial:
                String last = global_exp.removeLastExpression();
                BasicFunctionsTranslator translator = SemanticLatexParser.getBasicFunctionParser();
                String translation;

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
                return parseCaret( exp );
            case ellipsis:
                SymbolTranslator sT = SemanticLatexParser.getSymbolsTranslator();
                String symbol = sT.translateFromMLPKey( tag.tag() );
                local_inner_exp.addTranslatedExpression( symbol );
                global_exp.addTranslatedExpression( symbol );
                return true;
            case macro:
                ERROR_LOG.warning(
                        "A macro? What is it? Please inform " +
                                "Andre about this crazy shit: " +
                                term.getTermText());
                return false;
            case abbreviation:
                ERROR_LOG.warning(
                        "This program cannot parse abbreviations like " + term.getTermText()
                );
                return false;
            default:
                ERROR_LOG.warning("Unknown MathTerm Tag: "
                        + term.getTag());
                return false;
        }
    }

    /**
     * Inform the user about the fact of decision we made to translate this
     * constant or greek letter.
     *
     * This method only will invoke, if the term is a greek letter and maybe
     * a constant. So it is maybe a constant we know how to translate but don't
     * want to (like pi but the user should use \cpi instead)
     * or it is a completely unknown constant, then we can translate it as a greek
     * letter anyway (for instance \alpha, could be the 2nd Feigenbaum constant).
     *
     * @param constantSet the constant feature set
     * @param term the greek letter term
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
                        "But it is also a greek letter. " +
                        "Be aware, that this program translated the letter " +
                        "as a normal greek letter and not as a constant!" + System.lineSeparator() +
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
        Constants c = SemanticLatexParser.getConstantsParser();
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
        // get the parser first and try to parse it
        Constants c = SemanticLatexParser.getConstantsParser();
        String translated_const = c.translate( constant );

        // if it wasn't translated try some other stuff
        if ( translated_const == null ) {
            // try from LaTeX to CAS
            translated_const = c.translate(Keys.KEY_LATEX, Keys.CAS_KEY, constant);
            if ( translated_const != null ){
                // if this works, inform the user, that we use this translation now!
                String dlmf = c.translate( Keys.KEY_LATEX, Keys.KEY_DLMF, constant );
                INFO_LOG.addGeneralInfo(
                        constant,
                        "You use a typical letter for a constant [" +
                                DLMFFeatureValues.meaning.getFeatureValue(set) + "]." + System.lineSeparator() +
                                "We keep it like it is! But you should know that " + Keys.CAS_KEY +
                                " uses " + translated_const + " for this constant." + System.lineSeparator() +
                                "If you want to translate it as a constant, use the corresponding DLMF macro " +
                                dlmf + System.lineSeparator()
                );
                // and now, use this translation
                translated_const = constant;
            }
        }

        // still null? try to translate it as a greek letter than if possible
        if ( translated_const == null ){
            try {
                String alphabet = set.getFeature( Keys.FEATURE_ALPHABET ).first();
                if ( alphabet.contains( Keys.FEATURE_VALUE_GREEK ) ){
                    INFO_LOG.addGeneralInfo(
                            constant,
                            "Unable to translate " + constant + " [" +
                                    DLMFFeatureValues.meaning.getFeatureValue(set) +
                                    "]. But since it is a greek letter we translated it to a greek letter in "
                                    + Keys.CAS_KEY + "."
                    );
                    return parseGreekLetter( constant );
                } else {
                    ERROR_LOG.warning(
                            "Cannot translate mathematical constant " +
                                    constant + " - " + set.getFeature(Keys.FEATURE_MEANINGS)
                    );
                    return false;
                }
            } catch ( NullPointerException npe ){/* ignore it */}
        }

        // anyway, finally we translated it...
        local_inner_exp.addTranslatedExpression( translated_const );
        // add global_exp as well
        global_exp.addTranslatedExpression( translated_const );

        // if there wasn't a translation at all, return true

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
     * Parsing a given greek letter.
     * @param greekLetter the greek letter
     * @return true if it was parsed
     */
    private boolean parseGreekLetter( String greekLetter ){
        // try to translate
        GreekLetters l = SemanticLatexParser.getGreekLettersParser();
        String translated_letter = l.translate(greekLetter);

        // if it's null, maybe a \ is missing
        if ( translated_letter == null ){
            if ( !greekLetter.startsWith(CHAR_BACKSLASH) ){
                greekLetter = CHAR_BACKSLASH + greekLetter;
                translated_letter = l.translate(greekLetter);
            }
        }

        // still null? inform the user, we cannot do more here
        if ( translated_letter == null ){
            ERROR_LOG.warning("Cannot translate greek letter "
                    + greekLetter);
            return false;
        }

        // otherwise add all
        local_inner_exp.addTranslatedExpression(translated_letter);
        global_exp.addTranslatedExpression(translated_letter);
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
    private boolean parseCaret( PomTaggedExpression exp ){
        // get the power
        PomTaggedExpression sub_exp = exp.getComponents().get(0);
        // and parse the power
        TranslatedExpression power = parseGeneralExpression(sub_exp, null);

        // now we need to wrap parenthesis around the power
        Brackets b = Brackets.left_parenthesis;
        String powerStr = CHAR_CARET;
        if ( !testBrackets( power.toString() ) )
                powerStr += b.symbol + power.toString() + b.counterpart;
        else powerStr += power.toString();

        // the power becomes one big expression now.
        local_inner_exp.addTranslatedExpression( powerStr );
        local_inner_exp.addAutoMergeLast(1);

        // remove all elements added from the power process
        global_exp.removeLastNExps( power.clear() );
        // and add the power with parenthesis (and the caret)
        global_exp.addTranslatedExpression( powerStr );
        // merges last 2 expression, because after ^ it is one phrase than
        global_exp.mergeLastNExpressions(2);
        return !isInnerError();
    }
}
