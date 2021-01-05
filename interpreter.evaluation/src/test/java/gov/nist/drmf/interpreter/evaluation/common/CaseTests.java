package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.eval.Label;
import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.pom.common.CaseMetaData;
import gov.nist.drmf.interpreter.pom.common.SymbolTag;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
