package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This translation parses all of the DLMF macros. A DLMF macro
 * has always a feature set named dlmf-macro {@link Keys#KEY_DLMF_MACRO}.
 * This feature set has a lot of important features, like the number of
 * variables and links and so on.
 *
 * This parsers parses first all of the components of the DLMF macro.
 * For instance, JacobiP has 3 parameter and 1 variable. It parses the
 * following 4 continuous expressions and store them in an array.
 * After that, it replaces all placeholder in the translation by these
 * stored expressions.
 *
 * @see Keys
 * @see AbstractTranslator
 * @see gov.nist.drmf.interpreter.cas.logging.TranslatedExpression
 * @see InformationLogger
 * @author Andre Greiner-Petter
 */
public class MacroTranslator extends AbstractListTranslator {
    private static final Pattern optional_params_pattern =
            Pattern.compile("\\s*\\[(.*)]\\s*\\*?\\s*");

    // the number of parameters, ats and variables
    private int
            numOfParams,
            numOfAts,
            numOfVars;

    private String DLMF_example;

    private String constraints;

    private String description;

    private String meaning;

    private String def_dlmf, def_cas;

    private String translation_pattern, alternative_pattern;

    private String branch_cuts, cas_branch_cuts;

    private String cas_comment;

    private MathTerm macro_term;

    public MacroTranslator(){}

    @Override
    public boolean translate( PomTaggedExpression exp, List<PomTaggedExpression> following ){
        return translate(exp) && parse(following);
    }

    @Override
    public boolean translate(PomTaggedExpression root_exp) {
        // first of all, get the feature set named dlmf-macro
        macro_term = root_exp.getRoot();

        /*
        FeatureSet fset = term.getNamedFeatureSet( Keys.KEY_DLMF_MACRO );

        // if this set is null, it is simply not a dlmf-macro
        if ( fset == null ){
            LOG.warn("You should not use MacroTranslator when the PomTaggedExpression is " +
                    "not a dlmf-macro!");
            return false;
        }

        storeInfos(fset);

        // put all information to the info log
        INFO_LOG.addMacroInfo(
                term.getTermText(),
                createFurtherInformation()
        );
        */

        return true;
    }

    private void storeInfos( FeatureSet fset ){
        // now store all additional information
        // first of all number of parameters, ats and vars
        numOfParams = Integer.parseInt(DLMFFeatureValues.params.getFeatureValue(fset));
        numOfAts    = Integer.parseInt(DLMFFeatureValues.ats.getFeatureValue(fset));
        numOfVars   = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset));

        // now store additional information about the translation
        // Meaning: name of the function (defined by DLMF)
        // Description: same like meaning, but more rough. Usually there is only one of them defined (meaning|descreption)
        // Constraints: of the DLMF definition
        // Branch Cuts: of the DLMF definition
        // DLMF: its the plain, smallest version of the macro. Like \JacobiP{a}{b}{c}@{d}
        //      we can reference our Constraints to a, b, c and d now. That makes it easier to read
        meaning     = DLMFFeatureValues.meaning.getFeatureValue(fset);
        description = DLMFFeatureValues.description.getFeatureValue(fset);
        constraints = DLMFFeatureValues.constraints.getFeatureValue(fset);
        branch_cuts = DLMFFeatureValues.branch_cuts.getFeatureValue(fset);
        DLMF_example= DLMFFeatureValues.DLMF.getFeatureValue(fset);

        // Translation information
        translation_pattern = DLMFFeatureValues.CAS.getFeatureValue(fset);
        alternative_pattern = DLMFFeatureValues.CAS_Alternatives.getFeatureValue(fset);
        cas_comment         = DLMFFeatureValues.CAS_Comment.getFeatureValue(fset);
        cas_branch_cuts     = DLMFFeatureValues.CAS_BranchCuts.getFeatureValue(fset);

        // links to the definitions
        def_dlmf    = DLMFFeatureValues.dlmf_link.getFeatureValue(fset);
        def_cas     = DLMFFeatureValues.CAS_Link.getFeatureValue(fset);

        // maybe the alternative pattern got multiple alternatives
        if ( !alternative_pattern.isEmpty() ){
            try{ alternative_pattern = alternative_pattern.split( GlobalConstants.ALTERNATIVE_SPLIT )[0]; }
            catch ( Exception e ){}
        }
    }

    private boolean parse(List<PomTaggedExpression> following_exps){
        LinkedList<String> optional_paras = new LinkedList<>();
        PomTaggedExpression moveToEnd = null;

        while ( !following_exps.isEmpty() ){
            PomTaggedExpression first = following_exps.get(0);
            MathTerm first_term = first.getRoot();

            if ( first_term != null && !first_term.isEmpty() ){
                MathTermTags tag = MathTermTags.getTagByKey( first_term.getTag() );
                if ( tag == null ) break;
                else if ( tag.equals(MathTermTags.caret) ){
                    moveToEnd = following_exps.remove(0);
                    //continue;
                } else if ( tag.equals(MathTermTags.left_bracket) ){
                    TranslatedExpression inner_exp =
                            parseGeneralExpression(
                                    following_exps.remove(0),
                                    following_exps
                            );
                    String optional = inner_exp.toString();
                    global_exp.removeLastNExps( inner_exp.getLength() );
                    Matcher m = optional_params_pattern.matcher(optional);
                    if ( m.matches() )
                        optional_paras.add( m.group(1) );
                    else optional_paras.add( optional );
                } else {
                    break;
                }
            } else break;
        }

        FeatureSet fset = null;
        if ( optional_paras.size() == 0 ){
            fset = macro_term.getNamedFeatureSet( Keys.KEY_DLMF_MACRO );
        } else {
            fset = macro_term.getNamedFeatureSet(
                    Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX+optional_paras.size() );
            if ( fset == null ){
                LOG.error("Cannot find feature set with optional parameters.");
                return false;
            }
        }

        int start = optional_paras.size();
        storeInfos(fset);

        String info_key = macro_term.getTermText();
        if ( start != 0 )
            info_key += start;
        // put all information to the info log
        INFO_LOG.addMacroInfo(
                info_key,
                createFurtherInformation()
        );

        components = new String[ start + numOfParams + numOfVars ];
        for ( int i = 0; !optional_paras.isEmpty() && i < components.length; i++ )
            components[i] = optional_paras.removeFirst();

        int inner_at_counter = 0;
        for ( int i = start; !following_exps.isEmpty() && i < components.length; ){
            // get first expression
            PomTaggedExpression exp = following_exps.remove(0);

            if ( containsTerm(exp) ){
                MathTerm term = exp.getRoot();
                if ( inner_at_counter > numOfAts ){
                    LOG.error("Not valid number of @s in a DLMF-macro. " + DLMF_example);
                    return false;
                } else if ( term.getTag().matches(Keys.FEATURE_SET_AT) ){
                    inner_at_counter++;
                    continue;
                }
            }

            TranslatedExpression inner_exp = parseGeneralExpression(exp, following_exps);
            components[i] = inner_exp.toString();
            global_exp.removeLastNExps( inner_exp.getLength() );

            i++;
            if ( isInnerError() )
                return false;
        }

        if ( moveToEnd != null ){
            following_exps.add(0, moveToEnd);
        }

        // finally fill the placeholders by values
        fillVars();
        return true;
    }

    /**
     *
     */
    private void fillVars(){
        // when the alternative mode is activated, it tries to translate
        // the alternative translation
        String pattern = (GlobalConstants.ALTERNATIVE_MODE && !alternative_pattern.isEmpty()) ?
                alternative_pattern : translation_pattern;

        for ( int i = 0; i < components.length; i++ ){
            pattern = pattern.replace(
                    GlobalConstants.POSITION_MARKER + Integer.toString(i),
                    components[i]
            );
        }
        local_inner_exp.addTranslatedExpression(pattern);
        global_exp.addTranslatedExpression(pattern);
    }

    private String createFurtherInformation(){
        String extraInformation = "";
        if ( !meaning.isEmpty() )
            extraInformation += meaning;
        else if ( !description.isEmpty() )
            extraInformation += description;

        extraInformation += "; Example: " + DLMF_example + System.lineSeparator();

        if ( !cas_comment.isEmpty() )
            extraInformation += "Translation Information: " + cas_comment + System.lineSeparator();

        if ( !constraints.isEmpty() )
            extraInformation += "Constraints: " + constraints + System.lineSeparator();

        if ( !branch_cuts.isEmpty() )
            extraInformation += "Branch Cuts: " + branch_cuts + System.lineSeparator();

        if ( !cas_branch_cuts.isEmpty() )
            extraInformation += GlobalConstants.CAS_KEY + " uses other branch cuts: " + cas_branch_cuts
                    + System.lineSeparator();

        String TAB = SemanticLatexTranslator.TAB;
        String tab = TAB.substring(0, TAB.length()-("DLMF: ").length());
        extraInformation += "Relevant links to definitions:" + System.lineSeparator() +
                "DLMF: " + tab + def_dlmf + System.lineSeparator();
        tab = TAB.substring(0,
                ((GlobalConstants.CAS_KEY+": ").length() >= TAB.length() ?
                        0 : (TAB.length()-(GlobalConstants.CAS_KEY+": ").length()))
        );
        extraInformation += GlobalConstants.CAS_KEY + ": " + tab + def_cas;
        return extraInformation;
    }
}
