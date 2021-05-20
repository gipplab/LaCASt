package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.cas.Constraints;
import gov.nist.drmf.interpreter.common.eval.Label;
import gov.nist.drmf.interpreter.common.latex.Relations;
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
        assertEquals(2, con.getTexConstraints().length);
        assertEquals("n \\geq 0", con.getTexConstraints()[0]);
        assertEquals("\\realpart@@{z} > 0", con.getTexConstraints()[1]);
    }
}
