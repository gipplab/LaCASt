package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.cas.Constraints;
import gov.nist.drmf.interpreter.common.eval.Label;
import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.pom.common.CaseMetaData;
import gov.nist.drmf.interpreter.pom.common.SymbolTag;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class CaseTests {

    @Test
    void replaceTest() {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        CaseMetaData cmdDef = new CaseMetaData(
                2,
                new Label("http://dlmf.nist.gov/1.1.E2"),
                null,
                null
        );
        lib.add("C1.S2.i.m1bdec", "a", "repl", cmdDef);

        LinkedList<SymbolTag> used = new LinkedList<>();
        used.add(new SymbolTag("C1.S2.i.m1badec", "a"));
        CaseMetaData cmdCase = new CaseMetaData(
                1,
                new Label("http://dlmf.nist.gov/1.1.E1"),
                null,
                used
        );
        Case c = new Case("a", "b", Relations.EQUAL, cmdCase);

        assertEquals("a", c.getLHS());
        assertEquals("b", c.getRHS());

        c = c.replaceSymbolsUsed(lib);
        assertEquals("(r\\expe pl)", c.getLHS());
        assertEquals("b", c.getRHS());
    }

    @Resource("gammaConstraintTest.txt")
    void addConstraintTest(String testCasesStr) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        String[] lines = testCasesStr.split("\n");
        CaseAnalyzer.analyzeLine(lines[0], 0, lib);
        LinkedList<Case> cases = CaseAnalyzer.analyzeLine(lines[1], 1, lib);
        assertNotNull(cases);
        assertEquals(1, cases.size());

        Case c = cases.get(0);
        assertEquals("\\EulerGamma'@{1}", c.getLHS());
        assertEquals("-\\EulerConstant", c.getRHS());
        Constraints con = c.getConstraintObject();
        assertNull(con);

        c = c.replaceSymbolsUsed(lib);
        assertEquals("\\EulerGamma'@{1}", c.getLHS());
        assertEquals("-\\EulerConstant", c.getRHS());

        con = c.getConstraintObject();
        assertEquals(1, con.getTexConstraints().length);
        assertEquals("\\realpart@@{1} > 0", con.getTexConstraints()[0]);
    }

    @Resource("gammaConstraintTest.txt")
    void addMultiConstraintsTest(String testCasesStr) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        String[] lines = testCasesStr.split("\n");
        CaseAnalyzer.analyzeLine(lines[0], 0, lib);
        LinkedList<Case> cases = CaseAnalyzer.analyzeLine(lines[2], 1, lib);
        assertNotNull(cases);
        assertEquals(1, cases.size());

        Case c = cases.get(0);
        assertEquals("\\EulerGamma^{(n)}@{z}", c.getLHS());
        assertEquals("\\int_{0}^{\\infty}(\\ln@@{t})^{n}e^{-t}t^{z-1}\\diff{t}", c.getRHS());

        c = c.replaceSymbolsUsed(lib);
        assertEquals("\\EulerGamma^{(n)}@{z}", c.getLHS());
        assertEquals("\\int_{0}^{\\infty}(\\ln@@{t})^{n}\\expe ^{-t}t^{z-1}\\diff{t}", c.getRHS());

        Constraints con = c.getConstraintObject();
        assertEquals(2, con.getTexConstraints().length, con.toString());
        assertEquals("n \\geq 0", con.getTexConstraints()[0]);
        assertEquals("\\realpart@@{z} > 0", con.getTexConstraints()[1]);
    }

    @Resource("struve-11-5-2.txt")
    @DLMF("11.5.2")
    void struveConstraintsTest(String testCasesStr) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        String[] lines = testCasesStr.split("\n");
        int i = 0;
        LinkedList<Case> cases = null;
        for ( String line : lines ) {
            if ( cases != null && cases.size() > 0 ) {
                cases.get(0).replaceSymbolsUsed(lib);
            }
            cases = CaseAnalyzer.analyzeLine(line, i++, lib);
        }

        // the last list contains our struve test case
        assertNotNull(cases);
        assertEquals(1, cases.size());

        Case c = cases.get(0);
        assertEquals("\\StruveK{\\nu}@{z}", c.getLHS());
        assertEquals("\\frac{2(\\tfrac{1}{2}z)^{\\nu}}{\\sqrt{\\pi}\\EulerGamma@{\\nu+\\tfrac{1}{2}}}\\int_{0}^{\\infty}e^{-zt}(1+t^{2})^{\\nu-\\frac{1}{2}}\\diff{t}", c.getRHS());
        Constraints con = c.getConstraintObject();
        assertNotNull(con);
        assertEquals(1, con.getTexConstraints().length, con.toString());
        assertEquals("\\realpart@@{z} > 0", con.getTexConstraints()[0]);

        c = c.replaceSymbolsUsed(lib);
        con = c.getConstraintObject();
        assertEquals(5, con.getTexConstraints().length, con.toString());
        assertEquals("\\realpart@@{z} > 0", con.getTexConstraints()[0]);
        assertEquals("\\realpart@@{(\\nu+\\tfrac{1}{2})} > 0", con.getTexConstraints()[1]);
        assertEquals("\\realpart@@{(\\nu+k+1)} > 0", con.getTexConstraints()[2]);
        assertEquals("\\realpart@@{((-\\nu)+k+1)} > 0", con.getTexConstraints()[3]);
        assertEquals("\\realpart@@{(n+\\nu+\\tfrac{3}{2})} > 0", con.getTexConstraints()[4]);
    }
}
