package gov.nist.drmf.interpreter.roundtrip;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
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
import static org.junit.jupiter.api.Assertions.fail;

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
        TestStatus.reset();

        //Path p = GlobalPaths.PATH_REFERENCE_DATA.resolve("TestCases.txt");
        Path p = Paths.get("C:", "Users", "AndreG-P", "Uni", "MasterarbeitPRIVAT", "lessformulas.txt");

        try ( BufferedReader br = Files.newBufferedReader(p) ){
            mapleT.init();

            br.lines()//.limit(500)
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

    @AfterAll
    public static void end(){
        LOG.info("Results:"+System.lineSeparator() + TestStatus.stats());
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
            TestStatus.INEQUALITY.add( counter[0] );
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
        String mapleLHS = null, mapleRHS = null;
        try {
            mapleLHS = mapleT.translateFromLaTeXToMapleClean(test.LHS);
            mapleRHS = mapleT.translateFromLaTeXToMapleClean(test.RHS);
        } catch ( TranslationException te ){
            TranslationException.Reason r = te.getReason();
            if ( r != null && r.equals(TranslationException.Reason.UNKNOWN_MACRO) )
                TestStatus.ERROR_UNKNOWN_MACRO.add(test.line);
            else TestStatus.ERROR_FORWARD_TRANS_UNKOWN.add(test.line);
            fail("Cannot translate equation: "
                    + test.LHS + "=" + test.RHS + System.lineSeparator()
                    + "Reason: " + te.getMessage()
            );
        }
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
                            after_command,
                            test.line
                    ),
                    "Simplification fails: " + test.line
            );
        } catch ( MapleException me ){
            LOG.debug("Maple Error occurred, line " + test.line, me);
            TestStatus.ERROR_IN_MAPLE.add(test.line);
        }
    }

    private boolean simplifyTest( String mapleLHS, String mapleRHS,
                                  String preCommand, String afterCommand,
                                  int line )
            throws MapleException{
        if ( preCommand != null ){
            LOG.info("Previous command: " + preCommand);
            mapleT.enterMapleCommand(preCommand);
        }

        boolean resultB = mapleT.simplificationTester(mapleLHS, mapleRHS);
        if ( resultB )
            TestStatus.SUCCESSFUL.add( line );
        else {
            resultB = mapleT.simplificationTesterWithConversion(mapleLHS, mapleRHS, "exp");
            if ( resultB )
                TestStatus.SUCCESSFUL_EXP.add(line);
        }

        if ( !resultB ) {
            resultB = mapleT.simplificationTesterWithConversion(mapleLHS, mapleRHS, "hypergeom");
            if ( resultB )
                TestStatus.SUCCESSFUL_HYPER.add(line);
        }

        if ( !resultB ) {
            resultB = mapleT.simplificationTesterWithExpension(mapleLHS, mapleRHS, null);
            if ( resultB )
                TestStatus.SUCCESSFUL_EXPAND.add(line);
        }

        if ( !resultB )
            TestStatus.NOT_SUCCESSFUL.add(line);

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

    private enum TestStatus {
        SUCCESSFUL(0),
        SUCCESSFUL_EXP(0),
        SUCCESSFUL_HYPER(0),
        SUCCESSFUL_EXPAND(0),
        NOT_SUCCESSFUL(0),
        ERROR_FORWARD_TRANS_UNKOWN(0),
        ERROR_UNKNOWN_MACRO(0),
        ERROR_IN_MAPLE(0),
        INEQUALITY(0);

        int num;
        LinkedList<Integer> lines;

        TestStatus(int num){
            this.num = num;
            this.lines = new LinkedList<>();
        }

        public void add( int line ){
            num++;
            lines.add(line);
        }

        public static void reset(){
            for ( TestStatus t : TestStatus.values() ){
                t.num = 0;
                t.lines = new LinkedList<>();
            }
        }

        private String state(){
            return num + ", Lines: " + lines.toString();
        }

        public static String stats(){
            String nl = System.lineSeparator();
            String out =
                    "Number of ignored Inequalities: " + INEQUALITY.state() + nl;
            out += "Total Number of Success: " +
                    (SUCCESSFUL.num+SUCCESSFUL_EXP.num+SUCCESSFUL_HYPER.num+SUCCESSFUL_EXPAND.num)
                    + nl;
            out += "Total Number of Failues: " +
                    (NOT_SUCCESSFUL.num+ERROR_UNKNOWN_MACRO.num+ERROR_FORWARD_TRANS_UNKOWN.num+ERROR_IN_MAPLE.num)
                    + nl+nl;
            out += "Successful Tests after Simplify:       " + SUCCESSFUL.state() + nl;
            out += "Successful Tests after Exp-Conv:       " + SUCCESSFUL_EXP.state() + nl;
            out += "Successful Tests after Hypergeom-Conv: " + SUCCESSFUL_HYPER.state() + nl;
            out += "Successful Tests after Expansion:      " + SUCCESSFUL_EXPAND.state() + nl+nl;
            out += "Not Successful Tests:    " + NOT_SUCCESSFUL.state() + nl;
            out += "Unknown DLMF/DRMF Macro: " + ERROR_UNKNOWN_MACRO.state() + nl;
            out += "Error in Forward Trans:  " + ERROR_FORWARD_TRANS_UNKOWN.state() + nl;
            out += "Error in Maple:          " + ERROR_IN_MAPLE.state() + nl;
            return out;
        }
    }
}
