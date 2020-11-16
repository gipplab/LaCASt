package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.intellij.lang.annotations.Language;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class GenericFractionDerivFixer {

    private static class DerivArguments {
        private PrintablePomTaggedExpression degree;
        private PrintablePomTaggedExpression variable;

        DerivArguments(PrintablePomTaggedExpression degree, PrintablePomTaggedExpression variable) {
            this.degree = degree;
            this.variable = variable;
        }
    }

    private final PrintablePomTaggedExpression referencePTE;

    private final Map<PomTaggedExpression, DerivArguments> matchMemory;

    private boolean wasFixed;

    public GenericFractionDerivFixer(PrintablePomTaggedExpression pte) {
        this.referencePTE = pte;
        this.wasFixed = false;
        this.matchMemory = new HashMap<>();
    }

    public PrintablePomTaggedExpression fixGenericDeriv() {
        if ( wasFixed ) return referencePTE;

        List<PrintablePomTaggedExpression> derivs = PomTaggedExpressionUtility
                .findElements(referencePTE, this::isGenericDerivative);

        for ( PrintablePomTaggedExpression pte : derivs ) {
            replaceGenericDeriv(pte);
        }

        wasFixed = true;
        return referencePTE;
    }

    private void replaceGenericDeriv(PrintablePomTaggedExpression derivPte) {
        DerivArguments args = matchMemory.get(derivPte);

        List<PrintablePomTaggedExpression> newComponents = new LinkedList<>();

        MathTerm derivTerm = new MathTerm("\\deriv", MathTermTags.dlmf_macro.tag());
        derivTerm.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\deriv");
        SemanticMLPWrapper.getStandardInstance().loadFeatures(derivTerm);
        PrintablePomTaggedExpression derivExpression = new PrintablePomTaggedExpression(derivTerm);
        newComponents.add(derivExpression);

        if ( args.degree == null ) {
            args.degree = new PrintablePomTaggedExpression(new MathTerm("1", MathTermTags.numeric.tag()));
        }
        args.degree.makeBalancedOptionalArgumentString();
        newComponents.add(args.degree);

        PrintablePomTaggedExpression empty = FakeMLPGenerator.generateEmptyPPTE();
        empty.makeBalancedTexString();
        newComponents.add(empty);

        args.variable.makeBalancedTexString();
        newComponents.add(args.variable);

        derivPte.setTag( ExpressionTags.sequence.tag() );
        derivPte.removeNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY);
        derivPte.setPrintableComponents(newComponents);
    }

    public boolean isGenericDerivative(PrintablePomTaggedExpression pte) {
        if ( pte == null || pte.isEmpty() ) return false;

        ExpressionTags tag = ExpressionTags.getTag(pte);
        if ( !ExpressionTags.fraction.equals(tag) ) return false;

        PrintablePomTaggedExpression numerator = pte.getPrintableComponents().get(0);
        PrintablePomTaggedExpression denominator = pte.getPrintableComponents().get(1);

        PrintablePomTaggedExpression power = matchesNumerator(numerator);
        PrintablePomTaggedExpression variable = matchesDenominator(power, denominator);

        if ( variable != null ) {
            matchMemory.put(pte, new DerivArguments(power, variable));
        }
        return variable != null;
    }

    public static PrintablePomTaggedExpression matchesNumerator( PrintablePomTaggedExpression pte ) {
        if ( !PomTaggedExpressionUtility.isSequence(pte) ) return null;

        List<PrintablePomTaggedExpression> components = pte.getPrintableComponents();
        if ( !checkValidity(components, "d") ) return null;

        PrintablePomTaggedExpression power = getPower( components.get(1) );
        return power;
    }

    private static PrintablePomTaggedExpression getPower(PrintablePomTaggedExpression secondElement) {
        if ( MathTermTags.caret.equals( MathTermTags.getTagByExpression(secondElement) ) ) {
            return secondElement.getPrintableComponents().get(0);
        } else return null;
    }

    public static PrintablePomTaggedExpression matchesDenominator( PrintablePomTaggedExpression power, PrintablePomTaggedExpression denominator ) {
        if ( !PomTaggedExpressionUtility.isSequence(denominator) ) {
            if ( MathTermTags.alphanumeric.equals( MathTermTags.getTagByExpression(denominator) ) ) {
                MathTerm mt = denominator.getRoot();
                mt.setTermText( mt.getTermText().substring(1) );
                denominator.setRoot(mt);
                return power == null ? denominator : null;
            } else return null;
        }

        List<PrintablePomTaggedExpression> components = denominator.getPrintableComponents();
        if ( components.isEmpty() ) return null;

        PrintablePomTaggedExpression first = components.get(0);
        // first is either d or alphanumeric with d in beginning
        if ( !first.getRoot().getTermText().startsWith("d") ) return null;

        return getArgument(power, components);
    }

    private static PrintablePomTaggedExpression getArgument(PrintablePomTaggedExpression power, List<PrintablePomTaggedExpression> components) {
        PrintablePomTaggedExpression first = components.get(0);
        PrintablePomTaggedExpression argument = null;
        if ( MathTermTags.alphanumeric.equals( MathTermTags.getTagByExpression(first) ) ) {
            argument = first;
            MathTerm mt = argument.getRoot();
            mt.setTermText( mt.getTermText().substring(1) );
            first.setRoot(mt);
        }

        // until caret is argument
        List<PrintablePomTaggedExpression> argumentList = new LinkedList<>();
        if ( argument != null ) argumentList.add(argument);

        PrintablePomTaggedExpression identifiedPower = null;

        for ( int i = 1; i < components.size(); i++ ) {
            identifiedPower = getPower(components.get(i));
            if ( identifiedPower == null ) {
                argumentList.add(components.get(i));
            } else {
                break;
            }
        }

        if ( match(power, identifiedPower) ) {
            if ( argumentList.size() > 1 ) {
                PrintablePomTaggedExpression pte = FakeMLPGenerator.generateEmptySequencePPTE();
                pte.setPrintableComponents( argumentList );
                argument = pte;
            } else argument = argumentList.remove(0);
            return argument;
        } else return null;
    }

    private static boolean match(PrintablePomTaggedExpression one, PrintablePomTaggedExpression two) {
        if ( one == null || two == null ) return one == two;
        return one.getTexString().equals(two.getTexString());
    }

    private static boolean checkValidity(List<PrintablePomTaggedExpression> components, @Language("RegExp") String termMatch) {
        if ( components.isEmpty() ) return false;

        // first element must be d (or font manipulated d)
        if ( !components.get(0).getRoot().getTermText().matches(termMatch) ) return false;

        return components.size() == 2;
    }
}
