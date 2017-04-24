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
import java.util.Arrays;
import java.util.LinkedList;

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
        Integer[] ca = new Integer[]{0};
        testCases = new LinkedList<>();
        mapleT = new MapleTranslator();
        Path p = GlobalPaths.PATH_REFERENCE_DATA.resolve("TestCases.txt");
        try ( BufferedReader br = Files.newBufferedReader(p) ){
            mapleT.init();

            br.lines()
                    .filter( l -> !l.startsWith(";") )
                    .map( l -> l.split(";") )
                    .map( e -> {
                        String[] eq = e[0].split("=");
                        String ass = null;
                        if ( e.length > 1 ) ass = e[1];

                        ca[0]++;
                        if ( eq.length > 2 ){
                            for ( int i = 0; i < eq.length-1; i++ ){
                                testCases.add(
                                        new TestCaseInLaTeX(eq[i], eq[i+1],ass,ca[0])
                                );
                            }
                            return new TestCaseInLaTeX(eq[eq.length-1], eq[0], ass, ca[0]);
                        } else return new TestCaseInLaTeX(eq[0], eq[1], ass, ca[0]);
                    } )
                    .forEach( testCases::add );
        } catch ( Exception e ){
            e.printStackTrace();
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

        public TestCaseInLaTeX( String LHS, String RHS, String assumption, int line ){
            this.LHS = LHS;
            this.RHS = RHS;
            this.line = line;
            this.assumption = assumption;
        }

        @Override
        public String toString(){
            return line + ": " + LHS + "=" + RHS;
        }
    }
}
