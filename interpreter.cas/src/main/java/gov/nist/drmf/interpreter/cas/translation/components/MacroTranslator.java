package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.TEMPORARY_VARIABLE_NAME;

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
    private static final Logger LOG = LogManager.getLogger(MacroTranslator.class.getName());

    private static final Pattern optional_params_pattern =
            Pattern.compile("\\s*\\[(.*)]\\s*\\*?\\s*");

    private static final Pattern leibniz_notation_pattern =
            Pattern.compile("\\s*\\(([^@]*)\\)\\s*");

    private static final String deriv_special_case = "\\\\p?deriv";

    // the number of parameters, ats, and variables
    private int
            numOfParams           = Integer.MIN_VALUE,
            numOfAts              = Integer.MIN_VALUE,
            numOfVars             = Integer.MIN_VALUE,
            deriv_order_num       = 0;

    private int slotOfDifferentiation = 1; //Integer.MIN_VALUE;

    private String DLMF_example;

    private String constraints;

    private String description;

    private String meaning;

    private String def_dlmf, def_cas;

    private String translation_pattern, alternative_pattern;

    private String branch_cuts, cas_branch_cuts;

    private String cas_comment;

    private String deriv_order;

    private String varOfDiff;

    private boolean isWronskian;

    private MathTerm macro_term;

    private PomTaggedExpression moveToEnd;

    private LinkedList<String> optional_paras;

    private TranslatedExpression localTranslations;

    private String[] components;

    private final String CAS;

    public MacroTranslator(AbstractTranslator superTranslator){
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.CAS = getConfig().getTO_LANGUAGE();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public boolean translate( PomTaggedExpression exp, List<PomTaggedExpression> following ){
        isWronskian = exp.getRoot().getTermText().equals("\\Wronskian");
        if( isWronskian ) splitComma(following);
        return translate(exp) && parse(following);
    }

    @Override
    public boolean translate(PomTaggedExpression root_exp) {
        // first of all, get the feature set named dlmf-macro
        macro_term = root_exp.getRoot();
        return true;
    }

    // Works for 2 argument Wronskians, can be expanded to more arguments
    private void splitComma( List<PomTaggedExpression> following ){ // reads \Wronskian@{f1, f2} as if it were \Wronskian@{f1}{f2}
        PomTaggedExpression sequence = following.remove(1); // first element is "@"
        PomTaggedExpression firstHalf = new PomTaggedExpression();
        firstHalf.setTag("sequence");
        PomTaggedExpression secondHalf = new PomTaggedExpression();
        secondHalf.setTag("sequence");
        boolean passedComma = false;
        for( PomTaggedExpression exp : sequence.getComponents() ){
            MathTermTags tag = MathTermTags.getTagByKey(exp.getRoot().getTag());
            if( tag != null && tag.equals(MathTermTags.comma) ){
                passedComma = true;
                continue;
            }
            (passedComma ? secondHalf : firstHalf).addComponent(exp);
        }
        following.add(firstHalf);
        following.add(secondHalf);
    }

    private void storeInfos( FeatureSet fset ) throws TranslationException {
        //LOG.info("Extract information for " + macro_term.getTermText());
        // now store all additional information
        // first of all number of parameters, ats and vars
        numOfParams = Integer.parseInt(DLMFFeatureValues.params.getFeatureValue(fset, CAS));
        numOfAts    = Integer.parseInt(DLMFFeatureValues.ats.getFeatureValue(fset, CAS));
        numOfVars   = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset, CAS));

        try { // true slot is argument slot + numOfParams
            slotOfDifferentiation = Integer.parseInt(DLMFFeatureValues.slot.getFeatureValue(fset, CAS)) + numOfParams;
        } catch(NumberFormatException e) {
            slotOfDifferentiation = 1; // if slot isn't in lexicon, value is null
        }

        // now store additional information about the translation
        // Meaning: name of the function (defined by DLMF)
        // Description: same like meaning, but more rough. Usually there is only one of them defined (meaning|descreption)
        // Constraints: of the DLMF definition
        // Branch Cuts: of the DLMF definition
        // DLMF: its the plain, smallest version of the macro. Like \JacobiP{a}{b}{c}@{d}
        //      we can reference our Constraints to a, b, c and d now. That makes it easier to read
        meaning     = DLMFFeatureValues.meaning.getFeatureValue(fset, CAS);
        description = DLMFFeatureValues.description.getFeatureValue(fset, CAS);
        constraints = DLMFFeatureValues.constraints.getFeatureValue(fset, CAS);
        branch_cuts = DLMFFeatureValues.branch_cuts.getFeatureValue(fset, CAS);
        DLMF_example= DLMFFeatureValues.DLMF.getFeatureValue(fset, CAS);

        // Translation information
        translation_pattern = DLMFFeatureValues.CAS.getFeatureValue(fset, CAS);
        alternative_pattern = DLMFFeatureValues.CAS_Alternatives.getFeatureValue(fset, CAS);
        cas_comment         = DLMFFeatureValues.CAS_Comment.getFeatureValue(fset, CAS);
        cas_branch_cuts     = DLMFFeatureValues.CAS_BranchCuts.getFeatureValue(fset, CAS);

        // links to the definitions
        def_dlmf    = DLMFFeatureValues.dlmf_link.getFeatureValue(fset, CAS);
        def_cas     = DLMFFeatureValues.CAS_Link.getFeatureValue(fset, CAS);

        // maybe the alternative pattern got multiple alternatives
        if ( !alternative_pattern.isEmpty() ){
            try{ alternative_pattern = alternative_pattern.split( MacrosLexicon.SIGNAL_INLINE )[0]; }
            catch ( Exception e ){
                throw new TranslationException("Cannot split alternative macro pattern!",
                        TranslationException.Reason.DLMF_MACRO_ERROR);
            }

            if ( translation_pattern.isEmpty() ){
                LOG.trace("No direct translation! Switch to alternative mode for " + macro_term.getTermText());
                translation_pattern = alternative_pattern;
            }
        }

        if ( translation_pattern == null || translation_pattern.isEmpty() ){
            handleNull( null,
                "DLMF macro cannot be translated: " + macro_term.getTermText(),
                TranslationException.Reason.UNKNOWN_MACRO,
                macro_term.getTermText(),
                null
                );
        }
    }

    private boolean parse(List<PomTaggedExpression> following_exps){
        optional_paras = new LinkedList<>();
        moveToEnd = null;
        deriv_order = null;
        varOfDiff = null;

        FeatureSet fset = macro_term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if (fset != null) {
            storeInfos(fset);
            int sum = numOfAts + numOfVars + numOfParams;
            if (sum == 0) { // its a symbol
                super.getInfoLogger().addMacroInfo(
                        macro_term.getTermText(),
                        createFurtherInformation()
                );

                localTranslations.addTranslatedExpression(translation_pattern);
                super.getGlobalTranslationList()
                        .addTranslatedExpression(translation_pattern);
                return true;
            }
        }

        parseMacroModifiers(following_exps);

        if ( ( deriv_order == null || deriv_order.isEmpty() ) && deriv_order_num > 0 ) {
            deriv_order = Integer.toString(deriv_order_num);
        }

        if ( deriv_order != null && !deriv_order.isEmpty() ){
            if ( numOfParams > 0 ){
                throw new TranslationException(
                        "Differentiation occurs after parameters",
                        TranslationException.Reason.DLMF_MACRO_ERROR
                );
            }
        }

        if (optional_paras.size() > 0) {
            fset = macro_term.getNamedFeatureSet(
                    Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX + optional_paras.size());
            if (handleNull(fset,
                    "Cannot find feature set with optional parameters.",
                    TranslationException.Reason.UNKNOWN_MACRO,
                    macro_term.getTermText(), null)) {
                return true;
            }
        }

        int start = optional_paras.size();
        try {
            storeInfos(fset);
        } catch (NullPointerException npe) {
            handleNull(null,
                    "Cannot extract information from feature set: " + macro_term.getTermText(),
                    TranslationException.Reason.NULL,
                    macro_term.getTermText(),
                    npe);
        }

        String info_key = macro_term.getTermText();
        if (start != 0)
            info_key += start;
        // put all information to the info log
        getInfoLogger().addMacroInfo(
                info_key,
                createFurtherInformation()
        );

        // TODO bug
        if(!optional_paras.isEmpty()) slotOfDifferentiation += optional_paras.size();
        components = new String[start + numOfParams + numOfVars];
        for (int i = 0; !optional_paras.isEmpty() && i < components.length; i++)
            components[i] = optional_paras.removeFirst();

        PomTaggedExpression exp;
        boolean null_deriv_case = false;
        if (!following_exps.isEmpty()) {
            exp = following_exps.get(0);
            if (exp.isEmpty() && macro_term.getTermText().matches(deriv_special_case)) {
                following_exps.remove(0);
                start++;
                null_deriv_case = true;
            }
        }

        if( !parseComponents(following_exps, start) )
            return false;
        if (null_deriv_case && !following_exps.isEmpty()) {
            exp = following_exps.remove(0);
            TranslatedExpression following_argument =
                    parseGeneralExpression(exp, following_exps);
            components[start - 1] = following_argument.toString();

            if (checkForce(following_exps)) {
                exp = following_exps.remove(0);
                TranslatedExpression tmp = parseGeneralExpression(exp, following_exps);
                if (tmp.getLastExpression() == null) {
                    following_argument.removeLastExpression();
                    following_argument.addTranslatedExpression(
                            getGlobalTranslationList().removeLastExpression()
//                            global_exp.removeLastExpression()
                    );
                    components[start - 1] = following_argument.toString();
                }
            }

            getGlobalTranslationList().removeLastNExps(
                    following_argument.getLength()
            );
//            global_exp.removeLastNExps(following_argument.getLength());
        }

        if (moveToEnd != null) {
            following_exps.add(0, moveToEnd);
        }

        // finally fill the placeholders by values
        fillVars();
        return true;
    }

    // parses terms after macro and before regular parameters
    private void parseMacroModifiers(List<PomTaggedExpression> following_exps){
        deriv_order_num = 0;
        while (!following_exps.isEmpty()) {
            PomTaggedExpression first = following_exps.get(0);
            if (first.isEmpty()) break;
            MathTerm first_term = first.getRoot();

            if (first_term != null && !first_term.isEmpty()) {
                MathTermTags tag = MathTermTags.getTagByKey(first_term.getTag());
                if (tag == null) break;
                else if (tag.equals(MathTermTags.prime)) {
                    if (slotOfDifferentiation < 1) {
                        throwSlotError();
                    } else if ( deriv_order != null && !deriv_order.isEmpty() )
                        throwDifferentiationException();
                    deriv_order_num++;
                    following_exps.remove(0);
                } else if (tag.equals(MathTermTags.caret)) {
                    if (isLeibnizNotation(following_exps)) {
                        if( ( deriv_order == null || deriv_order.isEmpty() ) && deriv_order_num == 0 )
                            parseLeibnizNotation(following_exps);
                        else throwDifferentiationException();
                    } else {
                        moveToEnd = following_exps.remove(0); // regular exponentiation
                    }
                    //continue;
                } else if (tag.equals(MathTermTags.left_bracket)) {
                    String optional = translateInnerExp(following_exps.remove(0), following_exps);
                    Matcher m = optional_params_pattern.matcher(optional);
                    if (m.matches())
                        optional_paras.add(m.group(1));
                    else optional_paras.add(optional);
                } else {
                    break;
                }
            } else break;
        }
    }

    // parses components of macro
    private boolean parseComponents( List<PomTaggedExpression> following_exps, int start ){
        boolean passAts = false;
        int inner_at_counter = 0;
        for (int i = start; !following_exps.isEmpty() && i < components.length; ) {
            // get first expression
            PomTaggedExpression exp = following_exps.remove(0);

            if (containsTerm(exp)) {
                MathTerm term = exp.getRoot();
                MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
                if (inner_at_counter > numOfAts) {
                    throw new TranslationException(
                            "Not valid number of @s in a DLMF-macro. " + DLMF_example,
                            TranslationException.Reason.DLMF_MACRO_ERROR
                    );
                } else if (term.getTag().matches(Keys.FEATURE_SET_AT)) {
                    passAts = true;
                    inner_at_counter++;
                    continue;
                } else if (tag != null && tag.equals(MathTermTags.caret) && !passAts) {
                    if (moveToEnd != null) {
                        throw new TranslationException(
                                "Two times an exponent? That's not really allowed! " + macro_term.getTermText(),
                                TranslationException.Reason.DLMF_MACRO_ERROR
                        );
                    }
                    moveToEnd = exp;
                    continue;
                }
            }

            // TODO parseDifferentiation is not correct
//            parseDifferentiation(following_exps); // if there is differentiation after a parameter, it will get parsed here

            if ( ( deriv_order == null || deriv_order.isEmpty() ) && deriv_order_num > 0 ) {
                deriv_order = Integer.toString(deriv_order_num);
            }

            // if the macro term is \Wronskian, infer the variable of differentiation from the arguments of the Wronskian
            if( ( varOfDiff == null || varOfDiff.isEmpty() ) && isWronskian ) {
                extractVariableOfDiff(exp);
            }
            TranslatedExpression inner_exp = parseGeneralExpression(exp, following_exps);
            components[i] = inner_exp.toString();
            getGlobalTranslationList().removeLastNExps(inner_exp.getLength());

            i++;
            if (isInnerError())
                return false;
        }
        return true;
    }

    // looks only for differentiation after any parameters
    private void parseDifferentiation( List<PomTaggedExpression> following_exps ){
        deriv_order_num = 0;
        while (!following_exps.isEmpty()) {
            PomTaggedExpression first = following_exps.get(0);
            if (first.isEmpty()) break;
            MathTerm first_term = first.getRoot();

            if (first_term != null && !first_term.isEmpty()) {
                MathTermTags tag = MathTermTags.getTagByKey(first_term.getTag());
                if (tag == null) break;
                else if (tag.equals(MathTermTags.prime)) {
                    if (slotOfDifferentiation < 1) {
                        throwSlotError();
                    } else if ( deriv_order != null && !deriv_order.isEmpty() )
                        throwDifferentiationException();
                    deriv_order_num++;
                    following_exps.remove(0);
                } else if (tag.equals(MathTermTags.caret)) {
                    if (isLeibnizNotation(following_exps)) {
                        if( ( deriv_order == null || deriv_order.isEmpty() ) && deriv_order_num == 0 )
                            parseLeibnizNotation(following_exps);
                        else throwDifferentiationException();
                    } else {
                        throwDifferentiationException();
                    }
                    //continue;
                } else break;
            } else break;
        }
    }

    private void extractVariableOfDiff(PomTaggedExpression exp){
        while( !exp.isEmpty() ) { // look for a macro term in the expression and infer variable of diff based on that
            if (exp.getTag() != null && exp.getTag().equals("sequence")) {
                for (PomTaggedExpression expression : exp.getComponents()) {
                    if (isDLMFMacro(expression.getRoot())) {
                        exp = expression;
                    }
                }
            }
            if (isDLMFMacro(exp.getRoot())) { // found macro term
                FeatureSet fset = exp.getRoot().getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
                // get variable of differentiation from variable used in dlmf expression of macro
                String dlmf_expression = DLMFFeatureValues.DLMF.getFeatureValue(fset, CAS);
                int args = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset, CAS));
                int slot = 0;
                try {
                    slot = Integer.parseInt(DLMFFeatureValues.slot.getFeatureValue(fset, CAS));
                } catch (NumberFormatException e) {
                    throwSlotError();
                }
                String arg_extractor_single = "\\{([^{}]*)\\}";
                String arg_extractor_string = "@";
                for (int i = 0; i < args; i++) {
                    arg_extractor_string += arg_extractor_single;
                }
                Pattern arg_extractor_pattern = // capture all arguments of dlmf expression
                        Pattern.compile(arg_extractor_string);
                Matcher m = arg_extractor_pattern.matcher(dlmf_expression);
                if (m.find()) {
                    varOfDiff = m.group(slot); // extract argument that matches slot
                    return;
                } else {
                    throw new TranslationException(
                            "Unable to extract argument from " + dlmf_expression,
                            TranslationException.Reason.DLMF_MACRO_ERROR
                    );
                }
            } else {
                exp = exp.getNextSibling();
            }
        }
    }

    private String translateInnerExp( PomTaggedExpression expression, List<PomTaggedExpression> following_exps ){
        TranslatedExpression inner_exp =
                parseGeneralExpression(
                        expression,
                        following_exps
                );
        getGlobalTranslationList().removeLastNExps( inner_exp.getLength() );
        return inner_exp.toString();
    }

    // checks whether term after caret is a left parenthesis, meaning there is Leibniz notation
    private boolean isLeibnizNotation( List<PomTaggedExpression> following_exps ){
        if( slotOfDifferentiation < 1 ) return false;
        MathTerm term;
        try{
            PomTaggedExpression caret    = following_exps.get(0);
            PomTaggedExpression sequence = caret.getComponents().get(0);
            term                = sequence.getComponents().get(0).getRoot();
        } catch( IndexOutOfBoundsException e ){
            return false;
        }
        return term.getTag().equals(MathTermTags.left_parenthesis.tag());
    }

    // in \<macro>^{(<order>)}@{...}, extracts the <order> as the order of differentiation for the macro
    private void parseLeibnizNotation( List<PomTaggedExpression> following_exps ) {
        PomTaggedExpression expression = following_exps.remove(0).getComponents().get(0);
        String order = translateInnerExp(expression, following_exps);
        Matcher m = leibniz_notation_pattern.matcher(order);
        if (m.matches()) {
            deriv_order = m.group(1);
        } else {
            throw new TranslationException(
                    "Correct Leibniz notation is '\\<macro>^{(<order>)}@<args>'.",
                    TranslationException.Reason.WRONG_PARENTHESIS
            );
        }
    }

    private void throwSlotError() throws TranslationException{
        throw new TranslationException(
                "No information in lexicon for slot of differentiation of macro.",
                TranslationException.Reason.DLMF_MACRO_ERROR
        );
    }

    private void throwDifferentiationException() throws TranslationException{
        throw new TranslationException(
                "Cannot combine prime differentiation notation with Leibniz notation differentiation ",
                TranslationException.Reason.DLMF_MACRO_ERROR
        );
    }

    private boolean checkForce( List<PomTaggedExpression> following_exps ){
        if ( following_exps.isEmpty() ) return false;
        PomTaggedExpression next = following_exps.get(0);
        if ( next.isEmpty() ) return false;
        if ( next.getRoot().isEmpty() ) return false;

        MathTermTags tag = MathTermTags.getTagByKey( next.getRoot().getTag() );
        switch ( tag ){
            case caret:
            case factorial:
            case underscore:
                return true;
            default: return false;
        }
    }

    /**
     *
     */
    private void fillVars(){
        // when the alternative mode is activated, it tries to translate
        // the alternative translation
        String pattern = (getConfig().isAlternativeMode() && !alternative_pattern.isEmpty()) ?
                alternative_pattern : translation_pattern;

        String subbedExpression = null;
        if( deriv_order != null && !deriv_order.isEmpty() ){ // substitute out argument in slot of differentiation
            subbedExpression = components[slotOfDifferentiation - 1];
            components[slotOfDifferentiation - 1] = TEMPORARY_VARIABLE_NAME;
        }
        if( isWronskian ){ // plugs in variable of differentiation
            String[] newComponents = new String[ components.length + 1 ];
            newComponents[0] = varOfDiff;
            for( int i = 0; i < components.length; i++ ){
                newComponents[i + 1] = components[i];
            }
            components = newComponents;
        }
        for ( int i = 0; i < components.length; i++ ){
            LOG.info("Fill pattern: " + pattern);
            try {
                pattern = pattern.replace(
                        GlobalConstants.POSITION_MARKER + Integer.toString(i),
                        stripMultiParentheses(components[i])
                );
            } catch ( NullPointerException npe ){
                throw new TranslationException("Argument of macro seems to be missing for " + macro_term, TranslationException.Reason.NULL_ARGUMENT);
            }
        }
        // apply derivative and plug in the subbed out expression to replace temp during execution in CAS
        if ( deriv_order != null && !deriv_order.isEmpty() ){
            String[] args = new String[]{pattern, subbedExpression, deriv_order};
            pattern = getConfig().getBasicFunctionsTranslator().translate( args, "derivative" );
        }

        localTranslations.addTranslatedExpression(pattern);
        getGlobalTranslationList().addTranslatedExpression(pattern);
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
            extraInformation += CAS + " uses other branch cuts: " + cas_branch_cuts
                    + System.lineSeparator();

        String TAB = getConfig().getTAB();
        String tab = TAB.substring(0, TAB.length()-("DLMF: ").length());
        extraInformation += "Relevant links to definitions:" + System.lineSeparator() +
                "DLMF: " + tab + def_dlmf + System.lineSeparator();
        tab = TAB.substring(0,
                ((CAS+": ").length() >= TAB.length() ?
                        0 : (TAB.length()-(CAS+": ").length()))
        );
        extraInformation += CAS + ": " + tab + def_cas;
        return extraInformation;
    }
}
