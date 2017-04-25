package gov.nist.drmf.interpreter.roundtrip;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the equation test cases.
 *
 * Created by AndreG-P on 21.04.2017.
 */
public class EquationTestCases {
    public static final Logger LOG = LogManager.getLogger(EquationTestCases.class.toString());

    private static final String FERRER_DEF_ASS = "assume(-1 < x, x < 1);";
    private static final String LEGENDRE_DEF_ASS = "assume(1 < x);";
    private static final String RESET = "restart;";

    private static MapleTranslator mapleT;
    private static LinkedList<TestCaseInLaTeX> testCases;

    @BeforeAll
    public static void init(){
        Integer[] ca = new Integer[]{1};
        testCases = new LinkedList<>();
        mapleT = new MapleTranslator();

        Path p = GlobalPaths.PATH_REFERENCE_DATA.resolve("TestCases.txt");
        //Path p = Paths.get("C:", "Users", "AndreG-P", "Uni", "MasterarbeitPRIVAT", "lessformulas.txt");

        try ( BufferedReader br = Files.newBufferedReader(p) ){
            mapleT.init();

            br.lines().limit(200)
                    .map( l -> analyzeLine( l, ca ))
                    .filter( Objects::nonNull )
                    .forEach( t -> {
                        testCases.add(t);
                        LOG.info("Created test case: " + t);
                    } );
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    private static final String LABEL = "\\\\label";
    private static final String CONSTRAINT = "\\\\constraint";

    private static final Pattern CONST_PATTERN =
            Pattern.compile("\\s*\\{\\$(.*)\\$}\\s*");

    private static final Pattern LABEL_PATTERN =
            Pattern.compile("\\s*\\{(.*)}\\s*");

    public static TestCaseInLaTeX analyzeLine( String line, Integer[] counter ){
        String eq, constraint = null, label = null;
        String[] constSplit = line.split( CONSTRAINT );
        String[] labelSplit = line.split( LABEL );

        // there is a constraint
        if ( constSplit.length > 1 ){
            labelSplit = constSplit[1].split( LABEL );
            eq = constSplit[0];
            constraint = labelSplit[0];
            if ( labelSplit.length > 1 )
                label = labelSplit[1];
        } else if ( labelSplit.length > 1 ){
            eq = labelSplit[0];
            label = labelSplit[1];
        } else {
            eq = line;
        }

        eq = eq.replaceAll("[\\s,;.]*$","");

        String[] lrHS = eq.split("=");
        if ( constraint != null ){
            Matcher m = CONST_PATTERN.matcher(constraint);
            if ( m.matches() ){
                constraint = m.group(1);
            }
        }

        if ( label != null ){
            Matcher m = LABEL_PATTERN.matcher(label);
            if ( m.matches() ){
                label = m.group(1);
            }
        }

        if ( lrHS.length > 1 ){
            return new TestCaseInLaTeX( lrHS[0], lrHS[1], constraint, label, counter[0]++ );
        }
        else {
            LOG.info("Cannot handle inequalities right now. Line: " + counter[0]++);
            return null;
        }
    }

    @TestFactory
    Iterable<DynamicTest> iterateAllTestCases(){
        LinkedList<DynamicTest> tests = new LinkedList<>();

        while( !testCases.isEmpty() ){
            TestCaseInLaTeX test = testCases.remove(0);
            tests.add(
                    DynamicTest.dynamicTest(
                            "Test Line: " + test.line,
                            createTestCase( test )
                    )
            );
        }

        return tests;
    }

    private Executable createTestCase( TestCaseInLaTeX test ){
        return () -> testFunction( test, test.assumption );
    }

    private void testFunction( TestCaseInLaTeX test, String assumption ) throws MapleException{
        String mapleAss = null;
        if ( assumption != null ){
            LOG.info("Found assumption: " + assumption);
            mapleAss = mapleT.translateFromLaTeXToMapleClean(assumption);
            LOG.info("Translated assumption to: " + mapleAss);
        }

        LOG.info(test.toString());
        String mapleLHS = mapleT.translateFromLaTeXToMapleClean(test.LHS);
        String mapleRHS = mapleT.translateFromLaTeXToMapleClean(test.RHS);
        LOG.info("Translated to: " + mapleLHS + " = " + mapleRHS);

        String prev_command = null;
        String after_command = null;

        if ( test.RHS.contains("\\Ferrer") || test.LHS.contains("\\Ferrer") ){
            prev_command = MapleConstants.ENV_VAR_LEGENDRE_CUT_FERRER;
            prev_command += System.lineSeparator();
            after_command = MapleConstants.ENV_VAR_LEGENDRE_CUT_LEGENDRE;
            after_command += System.lineSeparator() + RESET;
        } else if ( test.RHS.contains("\\Legendre") || test.LHS.contains("\\Legendre") ){
            prev_command = LEGENDRE_DEF_ASS;
            after_command = RESET;
        }

        if ( prev_command != null )
            prev_command += mapleAss == null ? "" : "assume("+mapleAss+");" + System.lineSeparator();
        if ( prev_command == null && mapleAss != null )
            prev_command = "assume("+mapleAss+");" + System.lineSeparator();

        try {
            assertTrue(
                    simplifyTest(
                            mapleLHS,
                            mapleRHS,
                            prev_command,
                            after_command
                    ),
                    "Simplification fails: " + test.line
            );
        } catch ( MapleException me ){
            throw me;
        }
    }

    private boolean simplifyTest( String mapleLHS, String mapleRHS,
                                  String preCommand, String afterCommand )
            throws MapleException{
        if ( preCommand != null ){
            LOG.info("Previous command: " + preCommand);
            mapleT.enterMapleCommand(preCommand);
        }

        boolean resultB = mapleT.simplificationTester(mapleLHS, mapleRHS);
        if ( !resultB ) {
            resultB = mapleT.simplificationTesterWithConversion(mapleLHS, mapleRHS, "exp");
        }
        if ( !resultB )
            resultB = mapleT.simplificationTesterWithConversion(mapleLHS, mapleRHS, "hypergeom");

        if ( !resultB )
            resultB = mapleT.simplificationTesterWithExpension(mapleLHS, mapleRHS, null);

        if ( afterCommand != null ){
            LOG.info("After command: " + afterCommand);
            mapleT.enterMapleCommand(afterCommand);
        }

        return resultB;
    }

    public static class TestCaseInLaTeX{
        String LHS, RHS;
        int line;
        String assumption;
        String label;

        public TestCaseInLaTeX( String LHS, String RHS, String assumption, String label, int line ){
            this.LHS = LHS;
            this.RHS = RHS;
            this.assumption = assumption;
            this.label = label;
            this.line = line;
        }

        @Override
        public String toString(){
            return line + ": " + LHS + "=" + RHS + " CONSTR=" + assumption + " LABEL=" + label;
        }
    }

    public static class Statistics{
        int line;

        public Statistics( int line ){
            this.line = line;
        }
    }

    private enum TestStatus {
        SUCCESSFUL,
        SUCCESSFUL_EXP,
        SUCCESSFUL_HYPER,
        SUCCESSFUL_EXPAND,
        NOT_SUCCESSFUL,
        ERROR_FORWARD_TRANS_UNKOWN,
        ERROR_FORWARD_TRANS_MACRO,
        ERROR_IN_MAPLE
    }
}
