package gov.nist.drmf.interpreter.pom.generic;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        replaceInternal(List.of(referencePTE));
        fixImReOperatorInternal(List.of(referencePTE));
        fixNotEquals(List.of(referencePTE));
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

    private void fixNotEquals(List<PrintablePomTaggedExpression> expr) {
        if ( expr == null || expr.isEmpty() ) return;
        for (PrintablePomTaggedExpression pte : expr) {
            if (MathTermUtility.equals(pte.getRoot(), MathTermTags.negated_equals)) {
                MathTerm term = pte.getRoot();
                term.setTermText("\\neq");
                term.setTag(MathTermTags.relation.tag());
                mlp.loadFeatures(term);
                pte.setRoot(term);
            }
            fixNotEquals(pte.getPrintableComponents());
        }
    }

    private void replaceInternal(List<PrintablePomTaggedExpression> comps) {
        if ( comps == null || comps.isEmpty() ) return;

        LinkedList<PrintablePomTaggedExpression> newElements = new LinkedList<>();
        boolean listChange = false;
        PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression) comps.get(0).getParent();
        for ( PrintablePomTaggedExpression pte : comps ) {
            if ( !pte.hasNoChildren() ) {
                replaceInternal(pte.getPrintableComponents());
                newElements.add( pte );
                continue;
            }

            MathTerm term = pte.getRoot();
            MathTermTags tag = MathTermTags.getTagByMathTerm(term);
            String tex = term.getTermText();
            boolean updated = false;
            boolean addedElements = false;
            if ( "i".equals(tex) && replaceI ) {
                term.setTermText("\\iunit");
                updated = true;
            } else if ( "e".equals(tex) && replaceE ) {
                term.setTermText("\\expe");
                updated = true;
            } else if ( "\\pi".equals(tex) && replacePi ) {
                term.setTermText("\\cpi");
                updated = true;
            } else if ( replaceString(pte, tag, tex, "i") && replaceI ) {
                listChange = true;
                addedElements = true;
                newElements.addAll(replaceInPlaceI(pte, "i", "\\iunit"));
            } else if ( replaceString(pte, tag, tex, "e") && replaceE ) {
                listChange = true;
                addedElements = true;
                newElements.addAll(replaceInPlaceI(pte, "e", "\\expe"));
            } else if ( replaceString(pte, tag, tex, "pi") && replacePi ) {
                listChange = true;
                addedElements = true;
                newElements.addAll(replaceInPlaceI(pte, "pi", "\\cpi"));
            }

            if ( updated ) {
                term.setTag(MathTermTags.constant.tag());
                term.getNamedFeatures().clear();
                mlp.loadFeatures(term);
                pte.setRoot(term);
            }

            if ( !addedElements ) newElements.add(pte);
        }

        if ( listChange && parent != null ) {
            parent.setPrintableComponents(newElements);
        }
    }

    public boolean replaceString(PomTaggedExpression pte, MathTermTags tag, String tex, String sym) {
        if ( pte.getPreviousSibling() != null && PomTaggedExpressionUtility.isOperatorname(pte.getPreviousSibling()) )
            return false;
        return MathTermTags.alphanumeric.equals(tag) && tex.contains(sym);
    }

    private List<PrintablePomTaggedExpression> replaceInPlaceI(PrintablePomTaggedExpression pte, String tex, String replacement) {
        String txt = pte.getRoot().getTermText();
        StringBuilder sb = new StringBuilder();
        List<PrintablePomTaggedExpression> elements = new LinkedList<>();
        Matcher m = Pattern.compile(tex).matcher(txt);
        while ( m.find() ) {
            m.appendReplacement(sb, "");
            String s = sb.toString();
            if ( s.length() >= 1 ) elements.add(createAlphanumeric(s));

            MathTerm term = new MathTerm(replacement, MathTermTags.constant.tag());
            mlp.loadFeatures(term);
            elements.add(new PrintablePomTaggedExpression(term));
            sb = new StringBuilder();
        }

        m.appendTail(sb);
        String s = sb.toString();
        if ( s.length() >= 1 ) elements.add(createAlphanumeric(s));

        return elements;
    }

    private PrintablePomTaggedExpression createAlphanumeric(String s) {
        if ( s.length() == 1 ) {
            MathTerm term = new MathTerm(s, MathTermTags.letter.tag());
            mlp.loadFeatures(term);
            return new PrintablePomTaggedExpression(term);
        } else if ( s.length() > 1 ) {
            MathTerm term = new MathTerm(s, MathTermTags.alphanumeric.tag());
            mlp.loadFeatures(term);
            return new PrintablePomTaggedExpression(term);
        }
        return null;
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
