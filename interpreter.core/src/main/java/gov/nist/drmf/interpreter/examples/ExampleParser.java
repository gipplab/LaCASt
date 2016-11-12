package gov.nist.drmf.interpreter.examples;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import mlp.*;

import java.util.LinkedList;
import java.util.List;

/**
 * This parser is a test suite for further computations.
 * It is designed to handle JacobiP in semantic latex.
 * It is also necessary that the "global-lexicon" already contains
 * the JacobiP definition.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
@SuppressWarnings("all")
public class ExampleParser {
    // last parsed equation
    private String last_equation = "";

    // the maple representation of last parsed equation
    private String maple = "";

    // the link to the DLMF and Maple definition of the last parsed equation
    private String link_dlmf = "";
    private String link_maple = "";

    // list of constraints, basic in string style
    private List<String> constraints;

    // the parser itself
    private PomParser parser;

    private GreekLetters greek;

    /**
     * Simple constructor.
     */
    public ExampleParser(){
        constraints = new LinkedList<String>();
        greek = GreekLetters.getGreekLetterInstance();

        // initialize parser
        parser = new PomParser(GlobalConstants.PATH_REFERENCE_DATA.toString());
    }

    /**
     *
     * @param formula
     */
    public void parse(String formula) throws ParseException {
        this.last_equation = formula;
        PomTaggedExpression top_expression = parser.parse(formula);

        if ( top_expression.getRoot() != null && !top_expression.getRoot().isEmpty() ){
            System.out.println("Root term: " + top_expression.getRoot().getTermText());
        }

        maple = handleTopExpression(top_expression);
        System.out.println("Your given formula is translated to Maple:");
        System.out.println(maple);
    }

    int numOfParams = 0,
        numOfVars   = 0,
        numOfAts    = 0;
    private String[] storage;
    private String maplePatter;
    private boolean atPasses = false;
    private int atCounter = 0;

    private static String fillVars( String pattern, String[] storage ){
        for ( int i = 0; i < storage.length; i++ ){
            if ( storage[i] != null ) //storage[i] = "NULL";
                pattern = pattern.replace("$"+i, storage[i]);
        }
        return pattern;
    }

    private String handleTopExpression( PomTaggedExpression exp ){
        List<PomTaggedExpression> exp_list = exp.getComponents();

        // first, only take 1 macro (no prefix, no suffix)
        PomTaggedExpression top_exp = exp_list.remove(0);
        MathTerm root = top_exp.getRoot();
        if ( root == null || root.isEmpty() ){
            System.out.println("Something went wrong, first element should be a term.");
            return "";
        }

        // extract infos
        extractMacroInfo( root );
        storage = new String[numOfParams + numOfVars];

        // walk through tree (only neighbors)
        for ( int i = 0; i < numOfParams + numOfVars; ){
            PomTaggedExpression curr_exp = exp_list.remove(0);
            MathTerm term = curr_exp.getRoot();
            if ( term != null && !term.isEmpty() ){
                // ignore ats
                if ( term.getTag() == "at" ) continue;

                storage[i] = handleMathTerm(term);
                i++;
            } else {
                storage[i] = handleExpression( curr_exp );
                i++;
            }
        }

        while ( !exp_list.isEmpty() ){
            PomTaggedExpression curr_exp = exp_list.remove(0);
            maplePatter += handleGeneralExpression(curr_exp);
        }

        return fillVars( maplePatter, storage );
    }

    /**
     * ROOT TERM MUST BE NULL!
     * @param exp
     * @return
     */
    private String handleExpression( PomTaggedExpression exp ){
        /**
         * Possible PomTaggedExpressions Tags:
         *      frac -> 2 children
         *      cfrac -> 2 children (continued fractions)
         *      ifrac, dfrag, tfrac, icfrac (need to check)
         *      binom -> 2 children
         *      stackrel -> 2 children (NOT SUPPORTED)
         *      sqrt -> 1 children (or 2)
         */

        String tag = exp.getTag();
        if ( tag == null || tag.isEmpty() ){
            System.out.println("Unknown expression..." + exp);
            return "";
        }

        if ( tag == "sequence" ){
            return handleSequence(exp);
        } else if ( tag == "fraction" ){
            String[] tmp = handle2ArgExp(exp);
            return "(" + tmp[0] + ")/(" + tmp[1] + ")";
        } else if ( tag.contains("binomial") ){
            String[] tmp = handle2ArgExp(exp);
            return "binomial(" + tmp[0] + "," + tmp[1] + ")";
        } else if ( tag == "square root" ){
            return "sqrt(" + handleGeneralExpression(exp.getComponents().get(0)) + ")";
        } else if ( tag.contains("radical") ){
            String[] tmp = handle2ArgExp(exp);
            return "surd(" + tmp[1] + "," + tmp[0] + ")";
        } else {
            System.out.println("Not yet supported expression: " + exp);
            return "";
        }
    }

    private String[] handle2ArgExp(PomTaggedExpression e){
        String[] s = new String[2];
        List<PomTaggedExpression> fracComps = e.getComponents();
        s[0] = handleGeneralExpression(fracComps.get(0));
        s[1] = handleGeneralExpression(fracComps.get(1));
        return s;
    }

    private String handleGeneralExpression(PomTaggedExpression e){
        MathTerm term = e.getRoot();
        if ( term != null && !term.isEmpty() ){
            return handleMathTerm(term);
        } else {
            return handleExpression(e);
        }
    }

    /**
     * TAG MUST BE SEQUENCE!
     * @param topExp
     * @return
     */
    private String handleSequence(PomTaggedExpression topExp){
        List<PomTaggedExpression> exps = topExp.getComponents();
        String sequence = "";
        while ( !exps.isEmpty() ){
            PomTaggedExpression exp = exps.remove(0);
            sequence += handleGeneralExpression(exp);
            if ( exps.size() > 0 ) {
                if (    ( !exps.get(0).getRoot().isEmpty() &&
                            exps.get(0).getRoot().getTag().contains("parenthesis"))
                        ||
                        ( !exp.getRoot().isEmpty() &&
                                exp.getRoot().getTag().contains("parenthesis"))
                        ){
                    // do nothing
                } else {
                    sequence += " ";
                }
            }
        }
        return sequence;
    }

    private void extractMacroInfo( MathTerm macro ){
        FeatureSet macroSet = macro.getNamedFeatureSet("macro");
        if ( macroSet != null ){
            this.link_dlmf = "http://"+macroSet.getFeature("DLMF-Link").first();
            this.link_maple = "https://"+macroSet.getFeature("Maple-Link").first();
            this.numOfVars = Integer.parseInt(macroSet.getFeature("Number of Variables").first());
            this.numOfParams = Integer.parseInt(macroSet.getFeature("Number of Parameters").first());
            this.numOfAts = Integer.parseInt(macroSet.getFeature("Number of optional ats").first());
            this.maplePatter = macroSet.getFeature("Maple Representation").first();
            //System.out.println(numOfParams + ":" + numOfAts + ":" + numOfVars);
            //System.out.println(maple);
        }
    }

    private String handleMathTerm( MathTerm term ){
        String tag = term.getTag();
        if ( tag == null ) return "";

        if ( tag == "latex-command" ){
            // Greek Letter or other macro
            return handleLatexCommand(term);
        } else if ( tag.matches("function") ){
            // ... hmm
            return handleFunction(term);
        } else if (
                tag == "letter" || tag == "digit"   || tag == "numeric" ||
                tag == "minus"  || tag == "plus"    || tag == "equals" ||
                tag == "star"   || tag == "forward slash" ||
                tag.contains("parenthesis")) {
            // don't need to translate these
            return term.getTermText();
        } else if ( tag == "at" ){
            // ignore...
            return "";
        } else if ( tag == "alphanumeric"){
            String multi = "";
            String input = term.getTermText();
            for ( int i = 0; i < input.length(); i++ )
                multi += input.charAt(i) + " ";
            return multi;
        } else {
            System.out.println("Found not yet supported tag: " + tag);
            return "";
        }
    }

    private String handleLatexCommand( MathTerm term ){
        List<FeatureSet> featureSets = term.getAlternativeFeatureSets();
        while ( !featureSets.isEmpty() ){
            FeatureSet set = featureSets.remove(0);
            String alphabet = set.getFeature("Alphabet").first();
            if ( alphabet != null && !alphabet.isEmpty() && alphabet.matches("Greek") ){
                // its a greek letter, so translate the greek letter
                return greek.translate(GlobalConstants.KEY_LATEX, GlobalConstants.KEY_MAPLE, term.getTermText());
            }
        }
        System.err.println("Wasn't able to translate latex-command: " + term.getTermText());
        return "";
    }

    /**
     * What could it be? ...
     * @param term
     * @return
     */
    private String handleFunction( MathTerm term ){
        // first approach, just delete the "\" in front of a function...
        return term.getTermText().substring(1);
    }

    public static void main(String[] args){
        ExampleParser p = new ExampleParser();
        try{
            String formula = "";
            if ( args != null ) {
                for (int i = 0; i < args.length; i++) {
                    formula += args[i];
                    formula += " ";
                }
            }
            p.parse(formula);
            //p.parse("\\HypergeoF@@{1+0.3}{\\sqrt[5]{1}}{\\frac{1}{2}}{x}+2-\\cos(\\pi)");
            //p.parse("\\JacobiP{\\cos{a}}{\\beta+2}{\\frac{x+2}{2}}@{\\cos(a\\Theta)}");
        } catch ( Exception e ){
            System.err.println("Error occured!");
            e.printStackTrace();
        }
    }

    public String getMapleRepresentation(){
        return maple;
    }

    public List<String> getConstraints(){
        return constraints;
    }

    public String getDLMFDefinition(){
        return link_dlmf;
    }

    public String getMapleDefinition(){
        return link_maple;
    }
}
