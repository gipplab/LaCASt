package gov.nist.drmf.core.tests;

import gov.nist.drmf.interpreter.examples.ExampleParser;
import mlp.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A test suite for the Jacobi polynomial function.
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class JacobiPTests {
    // the example parser
    private static ExampleParser parser;

    private static final String
            prefix = "",
            suffix = "";

    private static final String
            maple_param1 = "alpha",
            maple_param2 = "beta",
            maple_param3 = "n",
            maple_var1 = "a Theta";

    private static final String
            tex_param1 = "\\alpha",
            tex_param2 = "\\beta",
            tex_param3 = "n",
            tex_var1 = "a\\Theta";

    private static final String SIMPLE_TEST_EQ =
            prefix +
            "\\JacobiP{" + tex_param1 + "}" +
                    "{" + tex_param2 + "}" +
                    "{" + tex_param3 + "}"+ "@" +
                    "{" + tex_var1 + "}" +
            suffix;

    @BeforeAll
    static void init(){
        parser = new ExampleParser();
        try {
            parser.parse(SIMPLE_TEST_EQ);
        } catch ( ParseException pe ){
            System.err.println("Cannot parse given equation. Tests stopped.");
            pe.printStackTrace();
        }
    }

    @Test
    void dlmfDefinitionTest(){
        String dlmf_def = "http://dlmf.nist.gov/18.3#T1.t1.r2";
        assertEquals(dlmf_def, parser.getDLMFDefinition(), "Wrong DLMF-Definition Link.");
    }

    @Test
    void mapleDefinitionTest(){
        String maple_def = "https://www.maplesoft.com/support/help/maple/view.aspx?path=JacobiP";
        assertEquals(maple_def, parser.getMapleDefinition(), "Wrong Maple Definition Link.");
    }

    @Test
    void translateToMapleTest(){
        String maple = prefix + "JacobiP(" +
                maple_param3 + "," +
                maple_param1 + "," +
                maple_param2 + "," +
                maple_var1 + ")" +
                suffix;

        assertEquals(
                maple,
                parser.getMapleRepresentation(),
                "Failed to translate from LaTeX to Maple!"
        );
    }

    @Test
    void constraintTests() {
        LinkedList<String> constraints = new LinkedList();
        constraints.add(tex_param1 + ">" + (-1));
        constraints.add(tex_param2 + ">" + (-1));
        List<String> parserConstraints = parser.getConstraints();

        if (parserConstraints == null) fail("Constraint are null");
        else {
            assertEquals(2, parserConstraints.size(), "Wrong number of constraints!");
            assertTrue(parserConstraints.contains(constraints.get(0)), "Contains wrong constraints.(1)");
            assertTrue(parserConstraints.contains(constraints.get(1)), "Contains wrong constraints.(2)");
        }
    }
}
