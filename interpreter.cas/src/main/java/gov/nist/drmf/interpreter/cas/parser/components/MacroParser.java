package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * This parser parses all of the DLMF macros. A DLMF macro
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
 * @see gov.nist.drmf.interpreter.cas.parser.AbstractParser
 * @see gov.nist.drmf.interpreter.cas.logging.TranslatedExpression
 * @see gov.nist.drmf.interpreter.cas.logging.InformationLogger
 * @author Andre Greiner-Petter
 */
public class MacroParser extends AbstractListParser {
    // the positioning character used for placeholders.
    private static final String position_char = "$";

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

    private String translation_pattern;

    private String branch_cuts;

    public MacroParser(){}

    @Override
    public boolean parse( PomTaggedExpression exp, List<PomTaggedExpression> following ){
        return parse(exp) && parse(following);
    }

    @Override
    public boolean parse(PomTaggedExpression root_exp) {
        MathTerm term = root_exp.getRoot();
        FeatureSet fset = term.getNamedFeatureSet( Keys.KEY_DLMF_MACRO );

        if ( fset == null ){
            ERROR_LOG.warning("You should not use MacroParser when the PomTaggedExpression is " +
                    "not a dlmf-macro!");
            return false;
        }

        numOfParams = Integer.parseInt(DLMFFeatureValues.params.getFeatureValue(fset));
        numOfAts    = Integer.parseInt(DLMFFeatureValues.ats.getFeatureValue(fset));
        numOfVars   = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset));

        meaning     = DLMFFeatureValues.meaning.getFeatureValue(fset);
        description = DLMFFeatureValues.description.getFeatureValue(fset);
        constraints = DLMFFeatureValues.constraints.getFeatureValue(fset);
        branch_cuts = DLMFFeatureValues.branch_cuts.getFeatureValue(fset);
        DLMF_example= DLMFFeatureValues.DLMF.getFeatureValue(fset);

        def_dlmf    = DLMFFeatureValues.dlmf_link.getFeatureValue(fset);
        def_cas     = DLMFFeatureValues.CAS_Link.getFeatureValue(fset);

        translation_pattern = DLMFFeatureValues.CAS.getFeatureValue(fset);

        INFO_LOG.addMacroInfo(
                term.getTermText(),
                createFurtherInformation()
        );
        components = new String[numOfParams + numOfVars];
        return true;
    }

    private boolean parse(List<PomTaggedExpression> following_exps){
        if ( components == null ) return false;

        for ( int i = 0; !following_exps.isEmpty() && i < components.length; ){
            // get first expression
            PomTaggedExpression exp = following_exps.remove(0);


            if ( containsTerm(exp) ){
                MathTerm term = exp.getRoot();
                if ( term.getTag().matches(Keys.FEATURE_SET_AT) ){
                    continue;
                }
                /*
                if ( isSubSequence(term) ){
                    Brackets bracket = Brackets.getBracket(term.getTermText());
                    SequenceParser sp = new SequenceParser(bracket);

                    if (!sp.parse(following_exps)){
                        return false;
                    }

                    String translation = sp.getTranslatedExpression();
                    if ( translation.matches( Brackets.OPEN_PATTERN + ".*" ) )
                        components[i] = translation.substring(1,translation.length()-1);
                    else components[i] = translation;
                    i++;
                    continue;
                } else if ( term.getTag().matches(Keys.FEATURE_SET_AT) ){
                    continue;
                }
                */
            }

            TranslatedExpression inner_exp = parseGeneralExpression(exp, following_exps);
            components[i] = inner_exp.toString();
            global_exp.removeLastNExps( inner_exp.getLength() );

            i++;
            if ( isInnerError() )
                return false;
        }

        // finally fill the placeholders by values
        fillVars();
        return true;
    }

    /**
     *
     */
    private void fillVars(){
        for ( int i = 0; i < components.length; i++ ){
            translation_pattern =
                    translation_pattern.replace(position_char + Integer.toString(i), components[i]);
        }
        local_inner_exp.addTranslatedExpression(translation_pattern);
        global_exp.addTranslatedExpression(translation_pattern);
    }

    private String createFurtherInformation(){
        String extraInformation = "";
        if ( !description.isEmpty() )
            extraInformation += description;
        if ( !description.isEmpty() && !meaning.isEmpty() )
            extraInformation += " / " + meaning;
        else if ( !meaning.isEmpty() )
            extraInformation += meaning;
        extraInformation += "; Example: " + DLMF_example + System.lineSeparator();
        extraInformation += "Constraints: " + constraints + System.lineSeparator();
        extraInformation += "Branch Cuts: " + branch_cuts + System.lineSeparator();
        extraInformation += "Link to definitions: " + System.lineSeparator() +
                def_dlmf + System.lineSeparator() + def_cas;
        return extraInformation;
    }
}
