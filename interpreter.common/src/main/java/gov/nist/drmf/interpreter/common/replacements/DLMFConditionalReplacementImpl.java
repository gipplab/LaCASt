package gov.nist.drmf.interpreter.common.replacements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFConditionalReplacementImpl extends ConditionalReplacementRule implements IReplacementCondition {
    private static final String EXP_IDX = "[#.]Ex?(\\d+)(?:X[a-z]|_\\d+)?";

    private static final Pattern DLMF_EXP_PATTERN = Pattern.compile(EXP_IDX);

    private static final Pattern DLMF_CONDITION_PATTERN = Pattern.compile(
            "\\d+(?:\\.\\d+)*(?:" + EXP_IDX + ")?"
    );

    public static final Pattern DLMF_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?dlmf\\.nist\\.gov/(.+)$"
    );

    private int[] hierarchy;

    public DLMFConditionalReplacementImpl(){};

    /**
     * Generates a replacement condition for a given string.
     * @param link the string to build a condition
     * @throws IllegalArgumentException if the given link is not a valid conditional string
     */
    public DLMFConditionalReplacementImpl(String link) throws IllegalArgumentException {
        Matcher m = DLMF_CONDITION_PATTERN.matcher(link);
        if ( !m.matches() ) throw new IllegalArgumentException("Invalid DLMF link " + link);

        // in case the link has the form #E13, switch to periods
        StringBuffer sb = new StringBuffer();
        Matcher expM = DLMF_EXP_PATTERN.matcher(link);
        if ( expM.find() ) {
            expM.appendReplacement(sb, "."+expM.group(1));
            expM.appendTail(sb);
            link = sb.toString();
        }

        String[] levelParts = link.split("\\.");
        this.hierarchy = new int[levelParts.length];
        for (int i = 0; i < levelParts.length; i++) {
            int level = Integer.parseInt(levelParts[i]);
            hierarchy[i] = level;
        }
    }

    @Override
    IReplacementCondition generateReplacementConditionalObject(String link) {
        return new DLMFConditionalReplacementImpl(link);
    }

    @Override
    public boolean match(IReplacementCondition refCon) {
        if ( !(refCon instanceof DLMFConditionalReplacementImpl) )
            return false; // or exception?

        DLMFConditionalReplacementImpl ref = (DLMFConditionalReplacementImpl)refCon;
        for ( int i = 0; i < hierarchy.length; i++ ) {
            if ( i >= ref.hierarchy.length ) return false;
            if ( hierarchy[i] != ref.hierarchy[i] ) return false;
        }

        return true;
    }

    @Override
    public int compareTo(IReplacementCondition refCon) {
        if ( !(refCon instanceof DLMFConditionalReplacementImpl) )
            throw new IllegalArgumentException("DLMF conditions cannot compared to other conditions.");

        DLMFConditionalReplacementImpl ref = (DLMFConditionalReplacementImpl)refCon;
        for ( int i = 0; i < hierarchy.length; i++ ) {
            if ( i >= ref.hierarchy.length ) return 0;
            if ( hierarchy[i] != ref.hierarchy[i] ) {
                return hierarchy[i] - ref.hierarchy[i];
            }
        }

        return 0;
    }

    /**
     * Extracts the equation label from a full DLMF url. If the given URL is invalid,
     * the returned string is empty.
     * @param dlmfURL DLMF equation URL
     * @return equation label in url
     */
    public static final String extractEquationLabelFromURL(String dlmfURL) {
        Matcher matcher = DLMF_URL_PATTERN.matcher(dlmfURL);
        if ( matcher.matches() ) {
            return matcher.group(1);
        } else return "";
    }
}
