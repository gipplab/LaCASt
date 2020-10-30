package gov.nist.drmf.interpreter.evaluation.constraints;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.MLPWrapper;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class ConstraintsMatcherTests {

    private static MLPWrapper wrapper;

    @BeforeAll
    static void init() throws IOException {
        wrapper = SemanticMLPWrapper.getStandardInstance();
    }

    private static void generalCheck(String blueprint, String constraint, String[] vars, String[] vals) throws ParseException {
        MLPBlueprintTree bt = new MLPBlueprintTree(vals);
        bt.setBlueprint(blueprint);

        MLPBlueprintNode constraintTree = MLPBlueprintTree.parseTree(constraint);
        assertTrue(bt.matches(constraintTree));

        String[][] v = bt.getConstraintVariablesAndValues();
        assertArrayEquals(v[0], vars);
        assertArrayEquals(v[1], vals);
    }

    @Test
    public void parseTest() throws ParseException {
        PomTaggedExpression pte = wrapper.parse("a+b");
        assertNotNull(pte);
        assertEquals(pte.getComponents().size(), 3);
    }

    @Test
    public void createBlueprintTest() throws ParseException {
        String blueprintConstraint = "var = 1,2";

        MLPBlueprintNode bt = MLPBlueprintTree.parseTree(blueprintConstraint);
        assertNotNull(bt);
    }

    @Test
    public void singleVariableMatchTest() throws ParseException {
        String blueprintConstraint = "var = 1,2";
        String actualConstraint = "n = 1,2";

        generalCheck(blueprintConstraint, actualConstraint, new String[] {"n"}, new String[] {"1"});
    }

    @Test
    public void multipleVariableMatchTest() throws ParseException {
        String blueprintConstraint = "var1,var2,var3 > 0";
        String actualConstraint = "a, b, c > 0";

        generalCheck(blueprintConstraint, actualConstraint, new String[] {"a", "b", "c"}, new String[] {"1", "1", "1"});
    }

    @Test
    public void differentValuesMatchTest() throws ParseException {
        String blueprintConstraint = "var1-var2 even";
        String actualConstraint = "v - w even";

        generalCheck(blueprintConstraint, actualConstraint, new String[] {"v", "w"}, new String[] {"2", "0"});
    }

    @Test
    public void complexMatchTest() throws ParseException {
        String blueprintConstraint = "var \\in \\Complex \\setminus [0,\\infty)";
        String actualConstraint = "z \\in \\Complex \\setminus [0, \\infty)";

        generalCheck(blueprintConstraint, actualConstraint, new String[] {"z"}, new String[] {"-1"});
    }

    @Test
    public void greekMatchTest() throws ParseException {
        String blueprint = "2 var \\neq -1,-2,-3, \\dotsc";
        String constraint = "2\\nu\\neq -1, -2, -3, \\dotsc";

        generalCheck(blueprint, constraint, new String[] {"\\nu"}, new String[] {"1/4"});
    }

    @Test
    public void uniformMatchTest() throws ParseException {
        String blueprint = "\\realpart{var} < \\frac{1}{2}, \\frac{3}{2}, \\dots";
        String constraint = "\\realpart{m} < \\ifrac{1}{2}, \\tfrac{3}{2}, \\ldots";

        generalCheck(blueprint, constraint, new String[] {"m"}, new String[] {"3/2"});
    }

    @Test
    public void nonMatchTest() throws ParseException {
        String blueprint = "var \\neq 0,1";
        String constraint = "x\\neq 0";

        MLPBlueprintTree bt = new MLPBlueprintTree(new String[]{"3/2"});
        bt.setBlueprint(blueprint);

        MLPBlueprintNode constraintTree = MLPBlueprintTree.parseTree(constraint);
        assertFalse(bt.matches(constraintTree));
    }

    @Test
    public void noMatchTest() throws ParseException {
        String blueprint = "\\realpart{var} > 1";
        String constraint = "n = 1,2";

        MLPBlueprintTree bt = new MLPBlueprintTree(new String[] {});
        bt.setBlueprint(blueprint);

        MLPBlueprintNode constraintTree = MLPBlueprintTree.parseTree(constraint);
        assertFalse(bt.matches(constraintTree));

        String[][] v = bt.getConstraintVariablesAndValues();
        assertEquals(v[0].length, 0);
        assertEquals(v[1].length, 0);
    }
}
