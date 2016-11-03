package gov.nist.drmf.interpreter.examples;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.io.IOException;
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

        // TODO
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
