package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.MeomArgumentLimitChecker;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class MeomArgumentLimitCheckerTest {
    @Test
    void isPotentialLimitBreakpointTest() {
        MathTerm mt = new MathTerm("dx", MathTermTags.alphanumeric.tag());
        PomTaggedExpression pte = new PomTaggedExpression(mt);
        assertTrue(MeomArgumentLimitChecker.isPotentialLimitBreakpoint(pte));
    }

    @Test
    void isNotPotentialLimitBreakpointTest() {
        MathTerm mt = new MathTerm("xy", MathTermTags.alphanumeric.tag());
        PomTaggedExpression pte = new PomTaggedExpression(mt);
        assertFalse(MeomArgumentLimitChecker.isPotentialLimitBreakpoint(pte));
    }

    @Test
    void isBreakpointTest() {
        MathTerm mt = new MathTerm("=", MathTermTags.equals.tag());
        assertTrue(MeomArgumentLimitChecker.isGeneralBreakPoint(mt));
        PomTaggedExpression pte = new PomTaggedExpression(mt);
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(pte));
    }

    @Test
    void allBreakpointStyleTest() {
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("<", MathTermTags.less_than.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm(">", MathTermTags.greater_than.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("=", MathTermTags.equals.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("<=", MathTermTags.relation.tag()))));

        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm(")", MathTermTags.right_parenthesis.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("]", MathTermTags.right_bracket.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("\\}", MathTermTags.right_brace.tag()))));

        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("\\right)", MathTermTags.right_parenthesis.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("\\right]", MathTermTags.right_bracket.tag()))));
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("\\right|", MathTermTags.right_brace.tag()))));

        assertTrue(MeomArgumentLimitChecker.isBreakPoint(new PomTaggedExpression(new MathTerm("\\right|", MathTermTags.right_brace.tag()))));

        // note, an empty expression is a breakpoint... while an empty math term alone, is not!
        assertTrue(MeomArgumentLimitChecker.isBreakPoint(FakeMLPGenerator.generateEmptyPPTE()));
        assertFalse(MeomArgumentLimitChecker.isGeneralBreakPoint(new MathTerm("")));
    }

    @Test
    void sequentialIsNotBreakpointTest() {
        PomTaggedExpression seq = FakeMLPGenerator.generateEmptySequencePPTE();
        assertFalse(MeomArgumentLimitChecker.isPotentialLimitBreakpoint(seq));
        assertFalse(MeomArgumentLimitChecker.isBreakPoint(seq));
        assertFalse(MeomArgumentLimitChecker.isGeneralBreakPoint(seq.getRoot()));
    }
}
