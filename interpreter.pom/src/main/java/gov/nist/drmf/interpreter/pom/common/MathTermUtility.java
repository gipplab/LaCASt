package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.common.grammar.FeatureValues;
import gov.nist.drmf.interpreter.pom.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import mlp.FeatureSet;
import mlp.MathTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

import static gov.nist.drmf.interpreter.pom.common.FeatureSetUtility.getSetByFeatureValue;

/**
 * @author Andre Greiner-Petter
 */
public final class MathTermUtility {
    private static final Logger LOG = LogManager.getLogger(MathTermUtility.class.getName());

    private static GreekLetters greekLettersMappings;

    private MathTermUtility(){}

    private static void initGreekLetters() {
        if ( greekLettersMappings == null ) {
            greekLettersMappings = new GreekLetters(Keys.KEY_LATEX, Keys.KEY_MAPLE);
            try {
                greekLettersMappings.init();
            } catch (IOException e) {
                greekLettersMappings = null;
            }
        }
    }

    /**
     * @param term math term
     * @return true if the given term is a greek letter
     */
    public static boolean isGreekLetter( MathTerm term ){
        String expr = term.getTermText();
        if ( expr == null || expr.isBlank() ) return false;

        initGreekLetters();
        String translation = greekLettersMappings.translate(expr);
        if ( translation == null ) translation = greekLettersMappings.translate("\\"+expr);
        return translation != null;
    }

    /**
     * Returns true if the math term is tagged with a feature set that describes the token as a greek letter.
     * Note, if you want to find out if a math term is actually a greek letter,
     * better use {@link #isGreekLetter(MathTerm)}.
     * @param term the math term
     * @return true if it has a greek letter interpretation
     */
    public static boolean hasGreekLetterMeaning( MathTerm term ) {
        List<FeatureSet> list = term.getAlternativeFeatureSets();
        for ( FeatureSet fset : list ){
            SortedSet<String> set = fset.getFeature(Keys.FEATURE_ALPHABET);
            if ( set == null ) continue;
            if ( set.contains(Keys.FEATURE_VALUE_GREEK) ) return true;
        }
        return false;
    }

    /**
     * @param term math term
     * @return true if the given term is a function
     */
    public static boolean isFunction( MathTerm term ){
        MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
        if (tag == null) {
            FeatureSet set = getSetByFeatureValue(term, Keys.FEATURE_ROLE, MathTermTags.function.tag());
            return set != null;
        } else return tag.equals(MathTermTags.function);
    }

    /**
     * Check if the provided math term is of the type tag.
     * @param term math term
     * @param tag the math term tag that should equal the given math term
     * @return true if the math term has the given tag
     */
    public static boolean equals( MathTerm term, MathTermTags tag ) {
        if ( term == null ) return false;
        MathTermTags t = MathTermTags.getTagByKey(term.getTag());
        return tag.equals(t);
    }

    /**
     * Returns true if the given term is a relation symbol
     * @param term a math term
     * @return true if the term is a relation symbol
     */
    public static boolean isRelationSymbol( MathTerm term ) {
        if ( term == null ) return false;
        MathTermTags tag = MathTermTags.getTagByMathTerm(term);
        switch (tag) {
            case equals: case relation: case greater_than: case less_than:
                return true;
            default: return false;
        }
    }

    public static boolean isAt(MathTerm term) {
        if ( term == null || term.getTermText().isBlank() ) return false;
        return "@".equals(term.getTermText());
    }

    public static boolean isSumOrProductOrLimit(MathTerm term) {
        return LimitedExpressions.isLimitedExpression(term);
    }

    public static String getAppropriateFontTex(MathTerm term) {
        String token = term.getTermText();
        if ( term.wasFontActionApplied() ) {
            token = term.firstFontAction() + "{" + token + "}";
        }

        List<String> accents = FeatureValues.ACCENT.getFeatureValues(term);
        for ( int i = accents.size()-1; i >= 0; i-- ) {
            token = "\\" + accents.get(i) + "{" + token + "}";
        }
        return token;
    }

    public static MathTerm secureClone(MathTerm term) {
        MathTerm mt = new MathTerm(term.getTermText(), term.getTag());
        mt.setFontAction(term.firstFontAction());
        mt.addSecondaryTags(term.getSecondaryTags());

        Map<String, String> features = term.getNamedFeatures();
        for ( Map.Entry<String, String> featureEntry : features.entrySet() )
            mt.addNamedFeature(featureEntry.getKey(), featureEntry.getValue());

        List<FeatureSet> featureSets = term.getAlternativeFeatureSets();
        List<FeatureSet> copiedFeatures = new LinkedList<>();
        for ( FeatureSet fset : featureSets ) {
            FeatureSet copyFSet = FeatureSetUtility.secureClone(fset);
            copiedFeatures.add(copyFSet);
        }

        mt.setAlternativeFeatureSets(copiedFeatures);
        return mt;
    }
}
