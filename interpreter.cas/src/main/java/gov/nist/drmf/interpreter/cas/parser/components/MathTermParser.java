package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.SemanticToCASInterpreter;
import gov.nist.drmf.interpreter.cas.parser.AbstractInnerParser;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;

import java.util.List;
import java.util.SortedSet;

/**
 * The math term parser parses only math terms.
 * It is a inner parser and switches through all different
 * kinds of math terms. All registered math terms can be
 * found in {@link MathTermTags}.
 *
 * @see gov.nist.drmf.interpreter.cas.parser.AbstractParser
 * @see AbstractInnerParser
 * @see MathTermTags
 * @author Andre Greiner-Petter
 */
public class MathTermParser extends AbstractInnerParser {
    @Override
    public boolean parse( MathTerm term ) {
        String tagExp = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(tagExp);
        FeatureSet constantSet =
                FeatureSetUtility.getSetByFeatureValue(
                        term,
                        Keys.FEATURE_ROLE,
                        Keys.FEATURE_VALUE_CONSTANT
                );

        // if the tag doesn't exists in the system now
        if ( tag == null ){
            ERROR_LOG.warning("Unknown tag: " + tagExp);
            return false;
        }

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
                //  4) any not supported macro -> error

                // is it a greek letter?
                if ( FeatureSetUtility.isGreekLetter(term) ){
                    if ( constantSet != null ){
                        constantVsLetter( constantSet, term );
                    }
                    if ( parseGreekLetter(term.getTermText()) )
                        return true;
                    else return false;
                }

                // or is it a constant?
                if ( constantSet != null ){
                    if ( parseMathematicalConstant( constantSet, term.getTermText() ) )
                        return true;
                    else return false;
                }

                // no it is a DLMF macro or unknown command
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
                ERROR_LOG.severe("MathTermParser cannot parse functions alone: "
                        + term.getTermText());
                return false;
            case letter:
                // lol a constant
                if ( constantSet != null ){
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
                translatedExp = term.getTermText();
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
                String alphanum = term.getTermText();
                if ( FeatureSetUtility.isGreekLetter( term ) ){
                    if ( constantSet != null ){
                        constantVsLetter( constantSet, term );
                    }
                    if ( parseGreekLetter( alphanum ) )
                        return true;
                    else return false;
                }

                // a constant
                if ( constantSet != null ){
                    if ( parseMathematicalConstant( constantSet, alphanum ) ){
                        return true;
                    } else return false;
                }

                // maple, simply add spaces between all letters
                String output = "";
                for ( int i = 0; i < alphanum.length()-1; i++ )
                    output += alphanum.charAt(i) + " ";
                translatedExp = output + alphanum.charAt(alphanum.length()-1);
                return true;
            case comma:
                // ignore?
                return true;
            case mod:
                ERROR_LOG.warning(
                        "Well, mod is pretty hard to handle right now... " +
                                "not supported yet.");
                return false;
            case macro:
                ERROR_LOG.warning(
                        "A macro? What is it? Please inform " +
                                "Andre about this crazy shit: " +
                                term.getTermText());
                return false;
            default:
                ERROR_LOG.warning("Unknown MathTerm Tag: "
                        + term.getTag());
                return false;
        }
    }

    private void constantVsLetter( FeatureSet constantSet, MathTerm term ){
        String dlmf = translateToDLMF(term.getTermText());
        if ( dlmf == null ){
            INFO_LOG.addGeneralInfo(
                    term.getTermText(),
                    "Could be " + DLMFFeatureValues.meaning.getFeatureValue(constantSet) + "."
                            + System.lineSeparator() +
                            "But this system don't know how to translate it as a constant. " +
                            "It was translated as a general letter."
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
                        dlmf + " to translate " + term.getTermText() + " as a constant."
        );
    }

    private String translateToDLMF( String constant ){
        Constants c = SemanticToCASInterpreter.CONSTANTS;
        if ( !constant.startsWith("\\") ) constant = "\\" + constant;
        String translated_const = c.translate( Keys.KEY_LATEX, Keys.KEY_DLMF, constant );
        return translated_const;
    }

    private boolean parseMathematicalConstant( FeatureSet set, String constant ){
        Constants c = SemanticToCASInterpreter.CONSTANTS;
        String translated_const = c.translate( constant );

        if ( translated_const == null )
            translated_const = c.translate( Keys.KEY_LATEX, Keys.CAS_KEY, constant );

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
                                    constant + " - " + set.getFeature(Keys.FEATURE_MEANING)
                    );
                    return false;
                }
            } catch ( NullPointerException npe ){/* ignore it */}
        }

        translatedExp += translated_const;
        INFO_LOG.addGeneralInfo(
                constant,
                DLMFFeatureValues.meaning.getFeatureValue(set) + " was translated to: " + translated_const
        );
        return true;
    }

    private boolean parseGreekLetter( String greekLetter ){
        GreekLetters l = SemanticToCASInterpreter.GREEK;
        String translated_letter = l.translate(greekLetter);

        if ( translated_letter == null ){
            if ( !greekLetter.startsWith("\\") ){
                greekLetter = "\\" + greekLetter;
                translated_letter = l.translate(greekLetter);
            }
        }

        if ( translated_letter == null ){
            ERROR_LOG.warning("Cannot translate greek letter "
                    + greekLetter);
            return false;
        }

        translatedExp += translated_letter;
        return true;
    }
}
