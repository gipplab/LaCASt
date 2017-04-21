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
    private static final String X_RESET = "x := 'x';";

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
                    .map( l -> l.split("=") )
                    .map( e ->
                    {
                        ca[0]++;
                        if ( e.length > 2 ){
                            for ( int i = 0; i < e.length-1; i++ ){
                                testCases.add(
                                        new TestCaseInLaTeX(e[i], e[i+1],ca[0])
                                );
                            }
                            return new TestCaseInLaTeX(e[e.length-1], e[0],ca[0]);
                        } else return new TestCaseInLaTeX(e[0], e[1],ca[0]);
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
                            createTestCase( test, null )
                    )
            );
        }

        return tests;
    }

    private Executable createTestCase( TestCaseInLaTeX test, String assumptions ){
        return () -> testFunction( test, assumptions );
    }

    private void testFunction( TestCaseInLaTeX test, String assumption ){
        LOG.info(test.toString());
        String mapleLHS = mapleT.translateFromLaTeXToMapleClean(test.LHS);
        String mapleRHS = mapleT.translateFromLaTeXToMapleClean(test.RHS);
        LOG.info("Translated to: " + mapleLHS + " = " + mapleRHS);

        String prev_command = null;
        String after_command = null;

        if ( test.RHS.contains("\\Ferrer") || test.LHS.contains("\\Ferrer") ){
            prev_command = MapleConstants.ENV_VAR_LEGENDRE_CUT_FERRER;
            prev_command += System.lineSeparator();
            prev_command += FERRER_DEF_ASS + System.lineSeparator();
            after_command = MapleConstants.ENV_VAR_LEGENDRE_CUT_LEGENDRE;
            after_command += System.lineSeparator() + X_RESET;
        } else if ( test.RHS.contains("\\Legendre") || test.LHS.contains("\\Legendre") ){
            prev_command = LEGENDRE_DEF_ASS;
            after_command = X_RESET;
        }

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
            me.printStackTrace();
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

        if ( afterCommand != null ){
            LOG.info("After command: " + afterCommand);
            mapleT.enterMapleCommand(afterCommand);
        }

        return resultB;
    }

    public static class TestCaseInLaTeX{
        String LHS, RHS;
        int line;
        public TestCaseInLaTeX( String LHS, String RHS, int line ){
            this.LHS = LHS;
            this.RHS = RHS;
            this.line = line;
        }

        @Override
        public String toString(){
            return line + ": " + LHS + "=" + RHS;
        }
    }
}
