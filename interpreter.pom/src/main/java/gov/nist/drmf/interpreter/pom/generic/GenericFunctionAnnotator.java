package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.common.interfaces.TranslationFeature;
import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class GenericFunctionAnnotator implements TranslationFeature<PrintablePomTaggedExpression> {

    private PrintablePomTaggedExpression pte;

    public GenericFunctionAnnotator() {}

    private GenericFunctionAnnotator(PrintablePomTaggedExpression pte) {
        this.pte = pte;
    }

    @Override
    public PrintablePomTaggedExpression preProcess(PrintablePomTaggedExpression pte) {
        GenericFunctionAnnotator genericFunctionAnnotator = new GenericFunctionAnnotator(pte);
        return genericFunctionAnnotator.annotateFunctions();
    }

    private PrintablePomTaggedExpression annotateFunctions() {
        Set<String> mem = new HashSet<>();
        Set<String> antiMem = new HashSet<>();
        findFunctionsRecursive(pte, mem, antiMem);
        mem.removeAll(antiMem);
        tagMemoryElementsAsFunction(pte, mem);
        return pte;
    }

    private void tagMemoryElementsAsFunction(PomTaggedExpression pte, Set<String> funcMemory) {
        if ( funcMemory.isEmpty() ) return;

        if ( pte == null || pte.isEmpty() ) return;
        if ( !pte.getComponents().isEmpty() ) {
            for ( PomTaggedExpression child : pte.getComponents() ) tagMemoryElementsAsFunction(child, funcMemory);
            return;
        }

        MathTerm term = pte.getRoot();
        if (funcMemory.contains(term.getTermText())) {
            PomTaggedExpressionUtility.tagAsFunction(pte);
        }
    }

    private void findFunctionsRecursive(PomTaggedExpression pte, Set<String> funcMemory, Set<String> antiFuncMemory) {
        if ( pte == null || pte.isEmpty() ) return;
        if ( !pte.getComponents().isEmpty() ) {
            for ( PomTaggedExpression child : pte.getComponents() ) findFunctionsRecursive(child, funcMemory, antiFuncMemory);
            return;
        }

        MathTerm term = pte.getRoot();
        if ( nextIsEqual(pte) ) antiFuncMemory.add(term.getTermText());
        else if (isFunctionLetter(term) && isFunction(pte) && isNotClosedExpression(pte)) {
            funcMemory.add(term.getTermText());
        }
    }

    private boolean nextIsEqual(PomTaggedExpression pte) {
        return pte != null && pte.getNextSibling() != null && MathTermUtility.isRelationSymbol( pte.getNextSibling().getRoot() );
    }

    private boolean isFunctionLetter(MathTerm term) {
        return MathTermUtility.equalsOr(term, MathTermTags.alphanumeric, MathTermTags.letter) ||
                MathTermUtility.isGreekLetter(term);
    }

    private boolean isFunction(PomTaggedExpression pte) {
        PomTaggedExpression next = pte.getNextSibling();
        return isFunctionFollowing(next);
    }

    private boolean isNotClosedExpression(PomTaggedExpression pte) {
        if ( pte instanceof PrintablePomTaggedExpression ) {
            PrintablePomTaggedExpression ppte = (PrintablePomTaggedExpression) pte;
            String expr = ppte.getTexString();
            return !TeXPreProcessor.wrappedInCurlyBrackets(expr);
        } else return true; // we probably should be more precise...
    }

    private boolean isFunctionFollowing(PomTaggedExpression next) {
        if ( next == null || next.isEmpty() ) return false;
        MathTerm term = next.getRoot();
        Brackets bracket = Brackets.getBracket(term);
        LinkedList<Brackets> bracketStack = new LinkedList<>();
        if ( bracket != null && bracket.opened ) {
            if ( Brackets.fence_open.equals(bracket) || Brackets.abs_val_open.equals(bracket) ) {
                // we do not recognize function arguments if they are given in |...| or \|...\|
                return false;
            }
            bracketStack.add(bracket);
            return !isArithmeticInside(next.getNextSibling(), bracketStack);
        } else if (MathTermUtility.equalsOr(term,
                MathTermTags.prime, MathTermTags.primes,
                MathTermTags.caret, MathTermTags.underscore) ||
                PomTaggedExpressionUtility.equals(next, ExpressionTags.sub_super_script)) {
            next = next.getNextSibling();
            return isFunctionFollowing(next);
        } else return false;
    }

    private boolean isArithmeticInside(PomTaggedExpression next, LinkedList<Brackets> bracketStack) {
        boolean arithmetic = false;
        boolean firstElement = true;
        while ( next != null ) {
            Brackets bracket = Brackets.getBracket(next);
            if ( bracket != null && bracket.opened ) bracketStack.addLast(bracket);
            else if ( bracket != null && !bracketStack.isEmpty() ) bracketStack.removeLast();
            else if ( isArgumentDelimiter(next) ) return false;
            else if ( PomTaggedExpressionUtility.isTeXEnvironment(next) ) return false;
            else if ( !firstElement ){
                arithmetic |= isArithmetic(next);
            }

            if ( bracketStack.isEmpty() ) return arithmetic;

            firstElement = false;
            next = next.getNextSibling();
        }
        return arithmetic;
    }

    private boolean isArgumentDelimiter(PomTaggedExpression next) {
        return next.getRoot().getTermText().matches("[,;]");
    }

    private boolean isArithmetic(PomTaggedExpression pte) {
        MathTerm term = pte.getRoot();
        return MathTermUtility.equalsOr(term, MathTermTags.plus, MathTermTags.minus,
                MathTermTags.divide, MathTermTags.multiply,
                MathTermTags.operator,
                MathTermTags.alphanumeric // alphanumeric also count because we interpret them as a sequence of multiplications!
        );
    }

}
