package gov.nist.drmf.interpreter.examples;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.core.GreekLetterInterpreter;
import mlp.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

/**
 * This parser is a test suite for further computations.
 * It is designed to handle JacobiP in semantic latex.
 * It is also necessary that the "global-lexicon" already contains
 * the JacobiP definition.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
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

    /**
     * Simple constructor.
     */
    public ExampleParser(){
        constraints = new LinkedList<String>();

        // initialize parser
        parser = new PomParser(GlobalConstants.REFERENCE_DATA_PATH.toString());
    }

    /**
     *
     * @param equation
     */
    public void parse(String equation) throws ParseException {
        this.last_equation = equation;
        PomTaggedExpression top_expression = parser.parse(equation);

        if ( top_expression.getTag() != "sequence" ){
            System.out.println("Given formula is not a sequence!");
            return;
        }

        if ( top_expression.getRoot() != null && !top_expression.getRoot().isEmpty() ){
            System.out.println("Root term:" + top_expression.getRoot().getTermText());
        }

        handleExpression(top_expression);
    }

    private int numOfParams = 0, numOfVars = 0, numOfAts = 0;
    private int index = 0;
    private String[] storage;
    private boolean atPasses = false;
    private int atCounter = 0;

    private String handleExpression( PomTaggedExpression exp ){
        List<PomTaggedExpression> exp_list = exp.getComponents();

        while ( !exp_list.isEmpty() ){
            PomTaggedExpression exp_curr = exp_list.remove(0);
            MathTerm root = exp_curr.getRoot();
            if ( root.isEmpty() ){
                String inner = handleExpression(exp_curr);
                System.out.println("FOUND INNER: " + inner);
            } else {
                handleMathTerm(root);
            }
        }

        for ( int i = 0; i < storage.length; i++ ){
            if ( storage[i] == null ) storage[i] = "NULL";
            maple = maple.replace("$"+i, storage[i]);
        }

        System.out.println("Maybe translated to Maple: " + maple);
        return maple;
    }

    private void handleMathTerm( MathTerm term ){
        FeatureSet macroSet = term.getNamedFeatureSet("macro");
        if ( macroSet != null ){
            this.link_dlmf = "http://"+macroSet.getFeature("DLMF-Link").first();
            this.link_maple = "https://"+macroSet.getFeature("Maple-Link").first();
            this.numOfVars = Integer.parseInt(macroSet.getFeature("Number of Variables").first());
            this.numOfParams = Integer.parseInt(macroSet.getFeature("Number of Parameters").first());
            this.numOfAts = Integer.parseInt(macroSet.getFeature("Number of optional ats").first());
            this.maple = macroSet.getFeature("Maple Representation").first();
            this.storage = new String[numOfParams+numOfVars];
            this.index = 0;
            this.atPasses = false;
            System.out.println(numOfParams + ":" + numOfAts + ":" + numOfVars);
            System.out.println(maple);
            return;
        }

        String tag = term.getTag();
        if ( tag == "letter" && index < storage.length){
            storage[index] = term.getTermText();
            index++;
        } else if ( tag == "at" ){
            if ( atCounter < numOfAts )
                atCounter++;
            else {
                System.err.println("illegal number of ats");
            }
        } else if ( tag == "latex-command" && index < storage.length ){
            String translation = translateCommand(term);
            storage[index] = translation;
            index++;
        }
    }

    private String translateCommand( MathTerm term ){
        String t = term.getTermText();
        /*
        System.out.println("TRANSLATION TIME: " + t);
        System.out.println("GetNamedFeatures");
        System.out.println(Arrays.toString(term.getNamedFeatures().keySet().toArray()));
        System.out.println("GetFeatureValue(Alphabet): " + term.getFeatureValue("Alphabet"));
        */
        List<FeatureSet> sets = term.getAlternativeFeatureSets();
        for ( FeatureSet feature : sets ){
            SortedSet<String> f = feature.getFeature("Alphabet");
            if ( f != null && !f.isEmpty() && f.first() == "Greek" ){
                return GreekLetterInterpreter.convertTexToMaple(t);
            }
        }
        System.err.println("Wasn't able to translate this command");
        return t;
    }

    public static void main(String[] args){
        ExampleParser p = new ExampleParser();
        try{
            p.parse("\\JacobiP{\\alpha}{\\beta}{n}@{\\cos{a\\Theta}}");
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
