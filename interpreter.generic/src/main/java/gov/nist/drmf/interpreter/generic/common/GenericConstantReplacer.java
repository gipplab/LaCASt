package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class GenericConstantReplacer {

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    private final PrintablePomTaggedExpression referencePTE;

    private boolean replacePi, replaceE, replaceI;

    public GenericConstantReplacer(PrintablePomTaggedExpression pte) {
        this.referencePTE = pte;
        this.replacePi = true;
        this.replaceE = analyzeE(pte);
        this.replaceI = analyzeI(pte);
    }

    public PrintablePomTaggedExpression fixConstants() {
        replaceInternal(referencePTE);
        fixImReOperatorInternal(List.of(referencePTE));
        return referencePTE;
    }

    private void fixImReOperatorInternal(List<PomTaggedExpression> elements) {
        if ( elements == null || elements.isEmpty() ) return;

        LinkedList<PomTaggedExpression> newElements = new LinkedList<>();
        boolean replaced = false;
        for ( int i = 0; i < elements.size(); i++ ) {
            PomTaggedExpression pte = elements.get(i);
            if ( PomTaggedExpressionUtility.isOperatorname(pte) ) {
                PomTaggedExpression next = pte.getNextSibling();
                if ( next != null && next.getRoot().getTermText().toLowerCase().matches("im|re") ) {
                    fixImReOperator(next);
                    newElements.add(pte);
                    i++;
                    replaced = true;
                    continue;
                }
            }
            newElements.add(pte);
        }

        if ( replaced ) {
            PomTaggedExpression parent = newElements.get(0).getParent();
            if ( parent != null ) parent.setComponents(newElements);
        }

        for ( PomTaggedExpression p : newElements ) {
            if ( p != null && !p.getComponents().isEmpty() ) fixImReOperatorInternal(p.getComponents());
        }
    }

    private void fixImReOperator(PomTaggedExpression imre) {
        MathTerm term = imre.getRoot();
        String macro;
        if ( term.getTermText().toLowerCase().matches("im") ) macro = "\\imagpart";
        else macro = "\\realpart";

        PomTaggedExpression operator = imre.getPreviousSibling();
        operator.getRoot().setTermText(macro);
        mlp.loadFeatures(operator.getRoot());
        operator.setRoot(operator.getRoot());
    }

    private void replaceInternal(PrintablePomTaggedExpression pte) {
        if ( pte == null ) return;

        if ( !pte.hasNoChildren() ) {
            for ( PrintablePomTaggedExpression p : pte.getPrintableComponents() ) {
                replaceInternal(p);
            }
            return;
        }

        MathTerm term = pte.getRoot();
        String tex = term.getTermText();
        boolean updated = false;
        if ( "i".equals(tex) && replaceI ) {
            term.setTermText("\\iunit");
            updated = true;
        } else if ( "e".equals(tex) && replaceE ) {
            term.setTermText("\\expe");
            updated = true;
        } else if ( "\\pi".equals(tex) && replacePi ) {
            term.setTermText("\\cpi");
            updated = true;
        }

        if ( updated ) {
            term.setTag(MathTermTags.constant.tag());
            term.getNamedFeatures().clear();
            mlp.loadFeatures(term);
            pte.setRoot(term);
        }
    }

    private boolean analyzeE(PrintablePomTaggedExpression pte) {
        if ( pte.hasNoChildren() && "e".equals(pte.getRoot().getTermText()) ) {
            boolean evidence = false;
            if ( pte.getNextSibling() != null )
                evidence = MathTermUtility.equals( pte.getNextSibling().getRoot(), MathTermTags.caret );
            return evidence;
        } else {
            List<PrintablePomTaggedExpression> comps = pte.getPrintableComponents();
            for ( PrintablePomTaggedExpression comp : comps ) {
                if ( analyzeE(comp) ) return true;
            }
        }

        return false;
    }

    private boolean analyzeI(PrintablePomTaggedExpression pte) {
        // find substring with i (like sum) otherwise its always constant...
        if ( "\\sum".equals(pte.getRoot().getTermText()) ) {
            return !containsI(pte.getNextSibling());
        } else {
            boolean subSuper = PomTaggedExpressionUtility.equals(pte, ExpressionTags.sub_super_script);
            subSuper |= MathTermUtility.equals( pte.getRoot(), MathTermTags.underscore );
            if ( subSuper ) {
                return !containsI(pte);
            }
        }

        for ( PrintablePomTaggedExpression p : pte.getPrintableComponents() ) {
            if ( !analyzeI(p) ) return false;
        }
        return true;
    }

    private boolean containsI(PomTaggedExpression pte) {
        if ( pte == null ) return false;
        if ( "i".equals(pte.getRoot().getTermText()) ) return true;
        for ( PomTaggedExpression p : pte.getComponents() ) {
            if ( containsI(p) ) return true;
        }
        return false;
    }

    public static void replaceGammaAsEulerMascheroniConstant(PrintablePomTaggedExpression pte) {
        if ( pte == null ) return;

        if ( !pte.hasNoChildren() ) {
            for ( PrintablePomTaggedExpression p : pte.getPrintableComponents() ) {
                replaceGammaAsEulerMascheroniConstant(p);
            }
            return;
        }

        MathTerm term = pte.getRoot();
        String tex = term.getTermText();
        if ( "\\gamma".equals(tex) ) {
            term.setTermText("\\EulerConstant");
            term.setTag(MathTermTags.constant.tag());
            term.getNamedFeatures().clear();
            mlp.loadFeatures(term);
            pte.setRoot(term);
        }
    }
}
