package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class GenericNormalizeOperatorNameCarets {
    private final PrintablePomTaggedExpression refPte;

    public GenericNormalizeOperatorNameCarets(PrintablePomTaggedExpression pte) {
        this.refPte = pte;
    }

    public PrintablePomTaggedExpression normalize() {
        normalizeInternally(List.of(refPte));
        return refPte;
    }

    private void normalizeInternally(List<PrintablePomTaggedExpression> components) {
        if ( components == null ) return;
        for ( int i = 0; i < components.size(); i++ ) {
            PrintablePomTaggedExpression pte = components.get(i);
            if (PomTaggedExpressionUtility.isOperatorname(pte)) {
                handleOperatorname(components, i);
            }

            normalizeInternally(pte.getPrintableComponents());
        }
    }

    private void handleOperatorname(List<PrintablePomTaggedExpression> components, int operatorIndex) {
        if ( operatorIndex + 2 > components.size() ) return;
        List<PrintablePomTaggedExpression> newComponents = new LinkedList<>(components.subList(0, operatorIndex+2));

        PrintablePomTaggedExpression potCaret = components.get(operatorIndex+2);
        if ( potCaret == null ) return;

        if (!MathTermUtility.equals(potCaret.getRoot(), MathTermTags.caret)) return;

        // ok we found it... move it to the end if necessary
        int endIndex = getEndArgIndex(components, operatorIndex+3);
        newComponents.addAll(components.subList(operatorIndex+3, endIndex+1));
        newComponents.add(potCaret);
        newComponents.addAll(components.subList(endIndex+1, components.size()));

        PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression) potCaret.getParent();
        parent.setPrintableComponents(newComponents);
    }

    private int getEndArgIndex(List<PrintablePomTaggedExpression> components, int startIndex) {
        LinkedList<Brackets> bracketStack = new LinkedList<>();
        for ( int i = startIndex; i < components.size(); i++ ) {
            PrintablePomTaggedExpression comp = components.get(i);
            Brackets bracket = Brackets.getBracket(comp);
            if ( bracket != null && bracket.opened ) {
                bracketStack.addLast(bracket);
            } else if ( bracket != null ) {
                bracketStack.removeLast();
            } else if ( bracketStack.isEmpty() ) {
                if ( i == startIndex ) return i;
                else return i-1;
            }
        }
        return components.size()-1;
    }
}
