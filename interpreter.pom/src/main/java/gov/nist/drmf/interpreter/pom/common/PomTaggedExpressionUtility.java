package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.FeatureValues;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.*;
import java.util.function.Function;

import static gov.nist.drmf.interpreter.common.text.TextUtility.splitAndNormalizeCommands;

/**
 * @author Andre Greiner-Petter
 */
public final class PomTaggedExpressionUtility {

    private PomTaggedExpressionUtility() {
    }

    public static String getNormalizedCaption(PomTaggedExpression pte, String expr) {
        if ( pte.getParent() == null && TeXPreProcessor.wrappedInCurlyBrackets(expr) )
            expr = TeXPreProcessor.trimCurlyBrackets(expr);
        else if ( pte.getParent() != null && PomTaggedExpressionUtility.isSequence(pte) && !TeXPreProcessor.wrappedInCurlyBrackets(expr) )
            expr = "{" + expr.trim() + "}";
        return expr.trim();
    }

    public static boolean beginsWithRelation(PomTaggedExpression pte) {
        if ( pte == null ) return false;
        if ( pte.isEmpty() ) return beginsWithRelation(pte.getNextSibling());
        if ( MathTermUtility.isRelationSymbol(pte.getRoot()) ) return true;
        if ( pte.getComponents().size() <= 1 ) return false;

        List<PomTaggedExpression> children = pte.getComponents();
        return beginsWithRelation(children.get(0));
    }

    public static boolean isListSetSeparationIndicator(PomTaggedExpression pte) {
        MathTermTags tag = MathTermTags.getTagByExpression(pte);
        if ( tag == null ) return false;
        switch (tag) {
            case comma: case semicolon: return true;
            default: return false;
        }
    }

    public static boolean isSequence(PomTaggedExpression pte) {
        if (pte == null || pte.isEmpty()) return false;
        ExpressionTags tag = ExpressionTags.getTagByKey(pte.getTag());
        return ExpressionTags.sequence.equals(tag);
    }

    /**
     * True if the given expression is accented (e.g. é).
     *
     * @param pte the expression component
     * @return true if it has an accent
     */
    public static boolean isAccented(PomTaggedExpression pte) {
        List<String> tags = pte.getSecondaryTags();
        for (String t : tags) {
            if (t.matches(ExpressionTags.accented.tag())) {
                return true;
            }
        }

        MathTerm mt = pte.getRoot();
        List<String> mtags = mt.getSecondaryTags();
        for (String t : mtags) {
            if (t.matches(ExpressionTags.accented.tag())) {
                return true;
            }
        }

        return false;
    }

    public static List<String> getFontManipulations(PomTaggedExpression pte) {
        List<String> manipulations = new LinkedList<>();
        String fontAction = pte.getRoot().firstFontAction();
        if ( fontAction != null && !fontAction.isBlank() ) manipulations.add(fontAction);
        if ( isAccented(pte) ) {
            String latex = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
            if ( latex != null )
                manipulations.add( latex ); // latex is always with \ and never contains multi elements

            String accent = pte.getFeatureValue(Keys.FEATURE_ACCENT);
            List<String> accents = splitAndNormalizeCommands(accent);

            if ( latex == null && !accents.isEmpty() ) {
                latex = "\\" + accents.get(0);
                accents.add(0, latex);
            }

            manipulations.addAll( accents );

            if ( accents.isEmpty() ) {
                accent = pte.getRoot().getFeatureValue(Keys.FEATURE_ACCENT);
                manipulations.addAll( splitAndNormalizeCommands(accent) );
            }
        }
        return manipulations;
    }

    public static void removeFontManipulations(PomTaggedExpression pte, List<String> deletions) {
        if ( isAccented(pte) ) {
            String latex = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
            List<String> accents = FeatureValues.ACCENT.getFeatureValues(pte);
            List<String> rootAccents = FeatureValues.ACCENT.getFeatureValues(pte.getRoot());

            for (String del : deletions) {
                if (del.equals(latex)) {
                    pte.removeNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY);
                    latex = null;
                }
                if (!accents.isEmpty() && accents.get(0).equals(del)) accents.remove(0);
                if (!rootAccents.isEmpty() && rootAccents.get(0).equals(del)) rootAccents.remove(0);
            }

            boolean addedToLatex = false;
            if ( latex == null ) {
                if ( accents.size() == 1 ) {
                    latex = "\\" + accents.remove(0);
                } else if ( rootAccents.size() == 1 ) {
                    latex = "\\" + rootAccents.remove(0);
                }

                if ( latex != null ) {
                    pte.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, latex);
                    pte.setSecondaryTags(ExpressionTags.accented.tag());
                    addedToLatex = true;
                }
            }

            if ( accents.isEmpty() ) {
                pte.removeNamedFeature(Keys.FEATURE_ACCENT);
            } else if (!addedToLatex) {
                pte.setNamedFeature(Keys.FEATURE_ACCENT, String.join(", ", accents));
            }

            if ( rootAccents.isEmpty() ) {
                pte.getRoot().removeNamedFeature(Keys.FEATURE_ACCENT);
            } else {
                pte.getRoot().setNamedFeature(Keys.FEATURE_ACCENT, String.join(", ", rootAccents));
            }
        }

        for ( String manipulation : deletions ) {
            String fontAction = pte.getRoot().firstFontAction();
            if ( fontAction != null && fontAction.equals(manipulation) ) {
                pte.getRoot().setFontAction(null);
            }
        }

        if ( !deletions.isEmpty() && (pte instanceof PrintablePomTaggedExpression) ) {
            ((PrintablePomTaggedExpression) pte).refreshTexComponents();
        }
    }

    public static String getAppropriateFontTex(PomTaggedExpression pte) {
        String appropriateTex = MathTermUtility.getAppropriateFontTex(pte.getRoot());

        List<String> rootAccents = FeatureValues.ACCENT.getFeatureValues(pte.getRoot());
        List<String> exprAccents = FeatureValues.ACCENT.getFeatureValues(pte);
        for ( int i = exprAccents.size()-1; i >= 0; i-- ) {
            appropriateTex = "\\" + exprAccents.get(i) + "{" + appropriateTex + "}";
        }

        String latexFeature = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
        if ( !shouldSkip(rootAccents, exprAccents, pte.getRoot(), latexFeature) ) {
            if ( !appropriateTex.isBlank() ) {
                appropriateTex = latexFeature + "{" + appropriateTex + "}";
            } else appropriateTex = latexFeature;
        }

        return appropriateTex;
    }

    private static boolean shouldSkip(List<String> rootAccents, List<String> exprAccents, MathTerm term, String latexFeature) {
        if ( latexFeature == null || latexFeature.isBlank() ) return true;

        String termText = term.getTermText();
        if ( termText.equals(latexFeature) ) return true;

        String fontAction = term.firstFontAction();
        if ( fontAction != null && fontAction.equals(latexFeature) ) return true;


        latexFeature = latexFeature.substring(1);

        if ( !exprAccents.isEmpty() ) {
            return exprAccents.get(0).equals(latexFeature);
        }
        if ( !rootAccents.isEmpty() ) {
            return rootAccents.get(0).equals(latexFeature);
        }

        return false;
    }

    public static boolean equals(PomTaggedExpression pte, ExpressionTags tag) {
        if (pte == null || tag == null) return false;
        ExpressionTags t = ExpressionTags.getTagByKey(pte.getTag());
        return tag.equals(t);
    }

    public static boolean isSingleVariable(PomTaggedExpression pte) {
        if (pte == null || !pte.getComponents().isEmpty()) return false;

        MathTermTags tag = MathTermTags.getTagByExpression(pte);
        if (tag == null) return false;
        switch (tag) {
            case symbol:
            case constant:
            case letter:
            case special_math_letter:
            case alphanumeric:
                return true;
            default:
                return MathTermUtility.isGreekLetter(pte.getRoot());
        }
    }

    public static boolean isAt(PomTaggedExpression exp) {
        if (exp == null) return false;
        return MathTermUtility.isAt(exp.getRoot());
    }

    public static <T extends PomTaggedExpression> List<T> findElements(
            T pte,
            Function<T, Boolean> checker
    ) {
        List<T> results = new LinkedList<>();
        findElements(pte, checker, results);
        return results;
    }

    private static <T extends PomTaggedExpression> void findElements(
            PomTaggedExpression pte,
            Function<T, Boolean> checker,
            List<T> results
    ) {
        if ( checker.apply((T)pte) ) {
            results.add((T)pte);
        }

        for ( PomTaggedExpression child : pte.getComponents() ) {
            findElements(child, checker, results);
        }
    }
}
