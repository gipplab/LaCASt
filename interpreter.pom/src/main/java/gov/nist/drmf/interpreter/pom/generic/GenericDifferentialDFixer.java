package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MeomArgumentLimitChecker;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class GenericDifferentialDFixer {
    private static final Logger LOG = LogManager.getLogger(GenericDifferentialDFixer.class.getName());

    private final PrintablePomTaggedExpression referencePTE;

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

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
                if ( PomTaggedExpressionUtility.equals(pte, ExpressionTags.fraction) ) {
                    PomTaggedExpression numerator = pte.getComponents().get(0);
                    boolean result = false;
                    if ( PomTaggedExpressionUtility.isSequence(numerator) ) {
                        result = handleIntegralHit(numerator.getComponents(), 0);
                    } else {
                        result = handleIntegralHit(pte.getComponents(), 0);
                    }
                    if ( result ) return true;
                }
            }
        }

        return false;
    }

    private EndPosition findAlphanumericDifferentialD(List<PomTaggedExpression> components, int start) {
        LinkedList<Brackets> bracketStack = new LinkedList<>();
        for ( int i = start; i < components.size(); i++ ) {
            PomTaggedExpression node = components.get(i);

            Brackets bracket = Brackets.getBracket(node);
            if ( bracket != null ) {
                updateBracketStack(bracketStack, bracket, node);
                continue;
            }

            if ( bracketStack.isEmpty() ) {
                if ( MeomArgumentLimitChecker.isGeneralBreakPoint(node.getRoot()) ) return new EndPosition(State.BREAKPOINT, i);
                else if ( MeomArgumentLimitChecker.isPotentialLimitBreakpoint(node) ) return new EndPosition(State.DIFF, i);
            }
        }
        return new EndPosition(State.EMPTY, components.size()-1);
    }

    private void updateBracketStack(LinkedList<Brackets> bracketStack, Brackets bracket, PomTaggedExpression node) {
        if ( bracket.opened ) bracketStack.add(bracket);
        else if ( bracketStack.isEmpty() )
            LOG.warn("Encountered closing bracket but no bracket was opened before: " + node.getRoot().getTermText());
        else {
            if ( !bracketStack.getLast().isCounterPart(bracket) )
                LOG.warn("Non-Matching closing bracket encountered. Last opened " + bracketStack.getLast() + " but encountered " + bracket);
            bracketStack.removeLast();
        }
    }

    private static final Pattern D_PATTERN = Pattern.compile("d([^d]+)");

    private List<PomTaggedExpression> splitAlphanumericDifferentialD(PomTaggedExpression alphaNumericD) {
        List<PomTaggedExpression> elements = new LinkedList<>();
        MathTerm term = alphaNumericD.getRoot();
        String dText = term.getTermText();

        String varStr;
        Matcher m = D_PATTERN.matcher(dText);
        StringBuilder sb = new StringBuilder();
        if ( m.find() ) {
            varStr = m.group(1);
            m.appendReplacement(sb, "");
            m.appendTail(sb);
            dText = sb.toString();
        } else {
            varStr = dText.substring(1);
            dText = "";
        }

        PrintablePomTaggedExpression diffPte = buildDiff();
        elements.add(diffPte);

        // two possible outcomes
        // first, dText is not empty -> in this case, update dtext and leave it as it is
        // second, dText is empty -> varStr contains the string

        if ( !varStr.isBlank() ) {
            PrintablePomTaggedExpression ppte = buildElement(varStr);
            ppte.makeBalancedTexString();
            elements.add(ppte);
            if ( !dText.isBlank() ) {
                term.setTermText(dText);
                elements.add(alphaNumericD);
            }
        } else {
            wrapNextArg(alphaNumericD);
        }

        return elements;
    }

    private void wrapNextArg(PomTaggedExpression pte) {
        if ( pte == null || pte.getNextSibling() == null ) return;
        PomTaggedExpression arg = pte.getNextSibling();
        if ( arg instanceof PrintablePomTaggedExpression ) {
            ((PrintablePomTaggedExpression)arg).makeBalancedTexString();
        }
    }

    private PrintablePomTaggedExpression buildDiff() {
        MathTerm dTerm = new MathTerm("\\diff", MathTermTags.dlmf_macro.tag());
        dTerm.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\diff");
        mlp.loadFeatures(dTerm);
        return new PrintablePomTaggedExpression(dTerm);
    }

    private PrintablePomTaggedExpression buildElement(String termText) {
        MathTermTags tag = termText.length() > 1 ? MathTermTags.alphanumeric : MathTermTags.letter;
        MathTerm term = new MathTerm(termText, tag.tag());
        mlp.loadFeatures(term);
        return new PrintablePomTaggedExpression(term);
    }
}
