package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import mlp.FeatureSet;
import mlp.MathTerm;

import java.util.List;
import java.util.SortedSet;

/**
 * @author Andre Greiner-Petter
 */
public class MathTermParser extends AbstractInnerParser {
    @Override
    public boolean parse( MathTerm term ) {
        String tagExp = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(tagExp);

        if ( tag == null ){
            errorMessage += "Could not find term tag: " + tagExp;
            return false;
        }

        switch( tag ){
            case command: // could be a DLMF-macro
                FeatureSet macro = term.getNamedFeatureSet("dlmf-macro");
                if ( macro != null ){
                    String areas = DLMFFeatureValues.areas.getFeatureValue(macro);
                    if ( areas.matches("constants") ){
                        translatedExp = Constants.getConstantsInstance().translate(
                                GlobalConstants.KEY_DLMF,
                                GlobalConstants.KEY_MAPLE,
                                term.getTermText()
                        );
                        return true;
                    } else {
                        translatedExp = macro.getFeature("Maple Representation").first();
                        return true;
                    }
                }

                List<FeatureSet> fsets = term.getAlternativeFeatureSets();
                while ( !fsets.isEmpty() ){
                    FeatureSet set = fsets.remove(0);
                    SortedSet<String> sset = set.getFeature("Alphabet");
                    if ( sset != null ){
                        String alphabet = sset.first();
                        GreekLetters greek = GreekLetters.getGreekLetterInstance();
                        translatedExp = greek.translate(
                                GlobalConstants.KEY_LATEX,
                                GlobalConstants.KEY_MAPLE,
                                term.getTermText());
                        return true;
                    }
                }
                errorMessage += "This should not happen. MathTermParser cannot handle a command... " +
                        term.getTermText();
                return false;
            case function:
                // TODO what are functions?...
                translatedExp = term.getTermText().substring(1);
                extraInformation += "Don't know how to handle 'functions' like "
                        + term.getTermText()
                        + System.lineSeparator();
                return true;
            case letter:
            case digit:
            case numeric:
            case minus:
            case plus:
            case equals:
            case multiply:
            case divide: // all above should translated directly, right?
            case less_than:
            case greater_than:
                translatedExp = term.getTermText();
                return true;
            case left_parenthesis:
            case left_bracket:
            case left_brace:
                translatedExp = "(";
                return true;
            case right_parenthesis:
            case right_bracket:
            case right_brace:
                translatedExp = ")";
                return true;
            case at:
                // simply ignore it...
                translatedExp = SPACE;
                return true;
            case alphanumeric:
                // maple, simply add spaces between all letters
                String alphanum = term.getTermText();
                String output = "";
                for ( int i = 0; i < alphanum.length()-1; i++ )
                    output += alphanum.charAt(i) + " ";
                translatedExp = output + alphanum.charAt(alphanum.length()-1);
                return true;
            case comma:
                // ignore?
                return true;
            case mod:
            case macro:
                // dont knonw what to do here now...
            default:
                return false;
        }
    }

    private boolean handleCommand( MathTerm term ){
        FeatureSet dlmfSet = term.getNamedFeatureSet("dlmf-macro");

        if ( dlmfSet != null ){

        }

        return false;
    }
}
