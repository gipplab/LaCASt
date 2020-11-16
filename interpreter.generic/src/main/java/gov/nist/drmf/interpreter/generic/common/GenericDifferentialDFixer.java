package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MeomArgumentLimitChecker;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class GenericDifferentialDFixer {
    private final PrintablePomTaggedExpression referencePTE;

    private boolean wasFixed;

    private enum State {BREAKPOINT, EMPTY, DIFF}

    private static class EndPosition {
        private State state;
        private int position;

        EndPosition(State state, int position) {
            this.state = state;
            this.position = position;
        }
    }

    public GenericDifferentialDFixer(PrintablePomTaggedExpression ppte){
        this.referencePTE = ppte;
        this.wasFixed = !referencePTE.getRootTexString().matches(".*\\\\i{1,4}nt[^a-zA-Z].*");
    }

    public PrintablePomTaggedExpression fixDifferentialD() {
        if ( wasFixed ) return referencePTE;

        List<PomTaggedExpression> components = referencePTE.getComponents();
        if ( !components.isEmpty() ) {
            fixDifferentialD(components);
        }

        wasFixed = true;
        return referencePTE;
    }

    private void fixDifferentialD(List<PomTaggedExpression> components) {
        for ( int i = 0; i < components.size(); i++ ) {
            PomTaggedExpression node = components.get(i);
            if ( LimitedExpressions.isIntegral(node.getRoot()) ) {
                handleIntegralHit(components, i);
            } else if ( !node.getComponents().isEmpty() ) {
                // depth search the hard way...
                fixDifferentialD(node.getComponents());
            }
        }
    }

    private boolean handleIntegralHit(List<PomTaggedExpression> components, int i) {
        EndPosition pos = findAlphanumericDifferentialD(components, i);
        if ( State.DIFF.equals(pos.state) ) {
            PomTaggedExpression alphanumericD = components.get(pos.position);
            List<PomTaggedExpression> newDSeq = splitAlphanumericDifferentialD(alphanumericD);

            List<PomTaggedExpression> newComponents = new LinkedList<>(components.subList(0, pos.position));

            PomTaggedExpression parent = alphanumericD.getParent();
            if ( !PomTaggedExpressionUtility.isSequence(parent) ) {
                PrintablePomTaggedExpression newSequence = FakeMLPGenerator.generateEmptySequencePPTE();
                newSequence.setPrintableComponents(newDSeq);
                newSequence.makeBalancedTexString();
                newComponents.add(newSequence);
            } else {
                newComponents.addAll( newDSeq );
            }

            newComponents.addAll( components.subList(pos.position+1, components.size()) );

            if ( parent instanceof PrintablePomTaggedExpression ) {
                ((PrintablePomTaggedExpression) parent).setPrintableComponents(newComponents);
            } else parent.setComponents(newComponents);
            return true;
        } else if ( State.EMPTY.equals(pos.state) ) {
            for ( PomTaggedExpression pte : components ) {
                ExpressionTags tag = ExpressionTags.getTag(pte);
                if ( ExpressionTags.fraction.equals(tag) ) {
                    if (handleIntegralHit(pte.getComponents(), 0)) return true;
                }
            }
        }
        return false;
    }

    private EndPosition findAlphanumericDifferentialD(List<PomTaggedExpression> components, int start) {
        for ( int i = start; i < components.size(); i++ ) {
            PomTaggedExpression node = components.get(i);
            if ( MeomArgumentLimitChecker.isBreakPoint(node) ) return new EndPosition(State.BREAKPOINT, i);
            else if ( MeomArgumentLimitChecker.isPotentialLimitBreakpoint(node) ) return new EndPosition(State.DIFF, i);
        }
        return new EndPosition(State.EMPTY, components.size()-1);
    }

    private List<PomTaggedExpression> splitAlphanumericDifferentialD(PomTaggedExpression alphaNumericD) {
        List<PomTaggedExpression> elements = new LinkedList<>();
        MathTerm term = alphaNumericD.getRoot();
        String dText = term.getTermText();
        term.setTermText(dText.substring(1));
        alphaNumericD.setRoot(term);
        if ( alphaNumericD instanceof PrintablePomTaggedExpression ) {
            ((PrintablePomTaggedExpression) alphaNumericD).makeBalancedTexString();
        }

        MathTerm dTerm = new MathTerm("\\diff", MathTermTags.dlmf_macro.tag());
        dTerm.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\diff");
        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        mlp.loadFeatures(dTerm);

        PrintablePomTaggedExpression ppte = new PrintablePomTaggedExpression(dTerm);

        elements.add(ppte);
        elements.add(alphaNumericD);
        return elements;
    }
}
