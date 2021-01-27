package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class GenericFractionDerivFixer {
    private static final Logger LOG = LogManager.getLogger(GenericFractionDerivFixer.class.getName());

    private static class DerivArguments {
        private PrintablePomTaggedExpression degree;
        private PrintablePomTaggedExpression variable;
        private List<PrintablePomTaggedExpression> arguments;

        DerivArguments(
                PrintablePomTaggedExpression degree,
                PrintablePomTaggedExpression variable,
                List<PrintablePomTaggedExpression> arguments
        ) {
            this.degree = degree;
            this.variable = variable;
            this.arguments = arguments;
        }
    }

    private static class NumeratorInformation {
        private PrintablePomTaggedExpression degree;
        private List<PrintablePomTaggedExpression> arguments;

        NumeratorInformation( PrintablePomTaggedExpression degree, List<PrintablePomTaggedExpression> arguments ) {
            this.degree = degree;
            this.arguments = arguments == null ? new LinkedList<>() : arguments;
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

        // create a list of new components which will replace the old derivPTE
        LinkedList<PrintablePomTaggedExpression> newComponents = new LinkedList<>();

        // at first, we add the elements prior to deriv to the new components list
        PrintablePomTaggedExpression previous = (PrintablePomTaggedExpression) derivPte.getPreviousSibling();
        while ( previous != null ) {
            newComponents.addFirst(previous);
            previous = (PrintablePomTaggedExpression) previous.getPreviousSibling();
        }

        // now the deriv elements and arguments
        MathTerm derivTerm = new MathTerm("\\deriv", MathTermTags.dlmf_macro.tag());
        derivTerm.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\deriv");
        SemanticMLPWrapper.getStandardInstance().loadFeatures(derivTerm);
        PrintablePomTaggedExpression derivExpression = new PrintablePomTaggedExpression(derivTerm);
        newComponents.addLast(derivExpression);

        // add degree (optional argument)
        if ( args.degree == null ) {
            args.degree = new PrintablePomTaggedExpression(new MathTerm("1", MathTermTags.numeric.tag()));
        }
        newComponents.addLast(Brackets.left_brackets.getPPTE());
        newComponents.addLast(args.degree);
        newComponents.addLast(Brackets.right_brackets.getPPTE());

        // if there is an argument we add it here as a sequence.
        if ( args.arguments == null || args.arguments.isEmpty() ) {
            PrintablePomTaggedExpression empty = FakeMLPGenerator.generateEmptyPPTE();
            empty.makeBalancedTexString();
            newComponents.addLast(empty);
        } else if (args.arguments.size() == 1) {
            args.arguments.get(0).makeBalancedTexString();
            newComponents.addLast( args.arguments.get(0) );
        } else {
            PrintablePomTaggedExpression sequence = FakeMLPGenerator.generateEmptySequencePPTE();
            sequence.setPrintableComponents( args.arguments );
            sequence.makeBalancedTexString();
            newComponents.addLast( sequence );
        }

        // add the variable of differentiation as the last argument of deriv
        args.variable.makeBalancedTexString();
        newComponents.addLast(args.variable);

        // finally, add all following nodes inline
        PrintablePomTaggedExpression following = (PrintablePomTaggedExpression) derivPte.getNextSibling();
        while ( following != null ) {
            newComponents.addLast( following );
            following = (PrintablePomTaggedExpression) following.getNextSibling();
        }

        // finally we have the list elements complete. Now we need to update the parent node
        PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression) derivPte.getParent();
        if ( !ExpressionTags.sequence.equalsPTE(parent) ) {
            // if there was no parent at all, we use derivPte itself and make it a sequence.
            // if the parent is not a sequence, we do the same, change derivPte to a sequence and append the elements
            derivPte.setTag( ExpressionTags.sequence.tag() );
            derivPte.removeNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY);
            derivPte.setPrintableComponents(newComponents);
        } else {
            // otherwise the parent is a sequence so we change the elements of that sequence directly
            parent.setPrintableComponents(newComponents);
        }
    }

    public boolean isGenericDerivative(PrintablePomTaggedExpression pte) {
        if ( pte == null || pte.isEmpty() ) return false;

        ExpressionTags tag = ExpressionTags.getTag(pte);
        if ( !ExpressionTags.fraction.equals(tag) ) return false;

        PrintablePomTaggedExpression numerator = pte.getPrintableComponents().get(0);
        PrintablePomTaggedExpression denominator = pte.getPrintableComponents().get(1);

        NumeratorInformation numeratorInformation = matchesNumerator(numerator);
        if ( numeratorInformation == null ) return false;

        PrintablePomTaggedExpression variable = matchesDenominator(numeratorInformation, denominator);

        if ( variable != null ) {
            matchMemory.put(pte, new DerivArguments(numeratorInformation.degree, variable, numeratorInformation.arguments));
        }

        return variable != null;
    }

    public static NumeratorInformation matchesNumerator( PrintablePomTaggedExpression pte ) {
        List<PrintablePomTaggedExpression> components = new LinkedList<>();

        if ( PomTaggedExpressionUtility.isSequence(pte) ) components.addAll( pte.getPrintableComponents() );
        else components.add( pte );

        if ( !checkValidity(components, "d") ) return null;

        if ( components.size() == 1 ) {
            // still valid but its just simply d in the numerator
            return new NumeratorInformation(null, null);
        }

        // we know the components are not empty (checkValidity) and bigger than 1. so lets try to get the power
        PrintablePomTaggedExpression power = getPower( components.get(1) );

        // alright, now the arguments.
        List<PrintablePomTaggedExpression> arguments = new LinkedList<>();
        // if there was no power, the argument starts on the 1, otherwise on 2
        for ( int i = power == null ? 1 : 2; i < components.size(); i++ ) {
            arguments.add( components.get(i) );
        }
        return new NumeratorInformation(power, arguments);
    }

    private static PrintablePomTaggedExpression getPower(PrintablePomTaggedExpression secondElement) {
        if ( MathTermTags.caret.equals( MathTermTags.getTagByExpression(secondElement) ) ) {
            return secondElement.getPrintableComponents().get(0);
        } else return null;
    }

    public static PrintablePomTaggedExpression matchesDenominator( NumeratorInformation numeratorInformation, PrintablePomTaggedExpression denominator ) {
        if ( !PomTaggedExpressionUtility.isSequence(denominator) ) {
            if ( MathTermTags.alphanumeric.equals( MathTermTags.getTagByExpression(denominator) ) ) {
                MathTerm mt = denominator.getRoot();
                mt.setTermText( mt.getTermText().substring(1) );
                denominator.setRoot(mt);
                return numeratorInformation.degree == null ? denominator : null;
            } else return null;
        }

        List<PrintablePomTaggedExpression> components = denominator.getPrintableComponents();
        if ( components.isEmpty() ) return null;

        PrintablePomTaggedExpression first = components.get(0);
        // first is either d or alphanumeric with d in beginning
        if ( !first.getRoot().getTermText().startsWith("d") ) return null;

        return getArgument(numeratorInformation.degree, components);
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

        return components.size() >= 1;
    }
}
