package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.parser.components.EmptyExpressionParser;
import gov.nist.drmf.interpreter.cas.parser.components.MathTermParser;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MacroParser extends AbstractParser {
    public static final String DLMF_FEATURE_NAME = "dlmf-macro";

    private final String position_char = "$";

    private int
            numOfParams,
            numOfAts,
            numOfVars;

    private String constraints;

    private String description;

    private String def_dlmf, def_maple;

    private String translation_pattern;

    private String[] macro_parts;

    public MacroParser(){}

    public boolean parse(PomTaggedExpression root_exp) {
        MathTerm term = root_exp.getRoot();
        FeatureSet fset = term.getNamedFeatureSet( DLMF_FEATURE_NAME );

        if ( fset == null ){
            errorMessage += "You should not use MacroParser when the PomTaggedExpression is" +
                    "not a dlmf-macro!" + System.lineSeparator();
            return false;
        }

        String areas = DLMFFeatureValues.areas.getFeatureValue(fset);
        //if ( !areas.matches("special functions") ) {
        //    errorMessage += "Not a special function!" + System.lineSeparator();
        //    return false;
        //}

        numOfParams = Integer.parseInt(DLMFFeatureValues.params.getFeatureValue(fset));
        numOfAts = Integer.parseInt(DLMFFeatureValues.ats.getFeatureValue(fset));
        numOfVars = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset));

        description = DLMFFeatureValues.description.getFeatureValue(fset);
        constraints = DLMFFeatureValues.constraints.getFeatureValue(fset);

        def_dlmf = DLMFFeatureValues.dlmf_link.getFeatureValue(fset);
        def_maple = DLMFFeatureValues.CAS_Link.getFeatureValue(fset);

        translation_pattern = DLMFFeatureValues.CAS.getFeatureValue(fset);

        createFurtherInformation();
        macro_parts = new String[numOfParams + numOfVars];
        return true;
    }

    public boolean parse(List<PomTaggedExpression> following_exps){
        if ( macro_parts == null ) return false;

        for ( int i = 0; !following_exps.isEmpty() && i < macro_parts.length; ){
            PomTaggedExpression exp = following_exps.remove(0);
            MathTerm term = exp.getRoot();
            AbstractParser p;

            //macro_parts[i] = parseGeneralExpression( exp, following_exps );

            if ( term != null && !term.isEmpty() ){
                String tag = term.getTag();
                if ( tag.matches(PARANTHESIS_PATTERN) ||
                        tag.matches("at"))
                    continue;
                p = new MathTermParser();
                if ( p.parse(exp) ){
                    extraInformation += p.extraInformation;
                    macro_parts[i] = p.getTranslatedExpression();
                    i++;
                } else {
                    extraInformation += p.extraInformation;
                    errorMessage += p.errorMessage;
                    return false;
                }
            } else {
                p = new EmptyExpressionParser();
                if ( p.parse( exp ) ) {
                    extraInformation += p.extraInformation;
                    macro_parts[i] = p.getTranslatedExpression();
                    i++;
                } else {
                    extraInformation += p.extraInformation;
                    errorMessage += p.errorMessage;
                    return false;
                }
            }
        }

        fillVars();
        return true;
    }

    private void fillVars(){
        for ( int i = 0; i < macro_parts.length; i++ ){
            translation_pattern =
                    translation_pattern.replace(position_char + Integer.toString(i), macro_parts[i]);
        }
        translatedExp = translation_pattern;
    }

    private void createFurtherInformation(){
        extraInformation += "Found DLMF-Macro: " + description + System.lineSeparator();
        extraInformation += "Constraints: " + constraints + System.lineSeparator();
        extraInformation += "Definitions:" + System.lineSeparator()
                + def_dlmf + System.lineSeparator()
                + def_maple + System.lineSeparator() + System.lineSeparator();
    }
}
