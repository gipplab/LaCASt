package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.text.IndexRange;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import mlp.PomTaggedExpression;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class PrintablePomTaggedExpressionRangeCalculator {
    @Language("RegExp")
    private static final String SPACE_PATTERN = "[\\s{}\\[\\]&]*";

    public PrintablePomTaggedExpressionRangeCalculator() {}

    public IndexRange getRange(PomTaggedExpression component, String expr) {
        String thisMatch = getStartingString(component);
        String nextMatch = getEndingString(component);

        Pattern thisPattern = Pattern.compile(generatePattern(thisMatch));
        Pattern nextPattern = Pattern.compile(nextMatch);

        Matcher thisM = thisPattern.matcher(expr);
        Matcher nextM = nextPattern.matcher(expr);

        int idxStart = 0;
        int idxEnd = expr.length();

        if (thisM.find()) {
            idxStart = thisM.start();
        }

        if (nextM.find()) {
            idxEnd = nextM.end();
        }

        if (idxStart == idxEnd) {
            // essentially means, getEnding and getStart provide the same string
            idxEnd += thisMatch.length();
        }

        // check before the wrapping { ... } if the brackets are correct now, or if we missed something
        idxEnd = checkIndexForClosingBrackets(idxStart, idxEnd, expr);

        if (isStartingIndexOpenBracket(idxStart, expr) && isEndingIndexCloseBracket(idxEnd, expr)){
            idxStart--;
            idxEnd++;
        }

        return new IndexRange(idxStart, idxEnd);
    }

    public boolean isStartingIndexOpenBracket(int idxStart, String expr) {
        return idxStart > 0 && (expr.charAt(idxStart-1) == '[' || expr.charAt(idxStart-1) == '{' );
    }

    public boolean isEndingIndexCloseBracket(int idxEnd, String expr) {
        return idxEnd < expr.length() && (expr.charAt(idxEnd) == ']' || expr.charAt(idxEnd) == '}');
    }

    private String generatePattern(String input) {
        if ( input.matches("[A-Za-z]+") ) {
            return "(?<![A-Za-z])"+input+"(?![A-Za-z])";
        } else return Pattern.quote(input);
    }

    private String getStartingString(PomTaggedExpression pte) {
        String token = PomTaggedExpressionUtility.getAppropriateFontTex(pte);
        return checkSubExpressionToken(token, pte);
    }

    private String checkSubExpressionToken(String token, PomTaggedExpression pte) {
        if (token.isBlank()) {
            if (pte.getComponents().isEmpty()) {
                // well, a blank token with no components is only possible by "{}". So we shall
                // return this, I guess.
                return "{";
            } else return getStartingString(pte.getComponents().get(0));
        } else return token;
    }

    private String getEndingString(PomTaggedExpression pte) {
        String envEndString = getEndEnvironmentPattern(pte);
        if ( envEndString != null ) return envEndString;

        List<PomTaggedExpression> components = pte.getComponents();
        if (components.isEmpty()) {
            String p = generatePattern(getStartingString(pte));
            // this only happens for empty expression. Hence, we want the } symbol here.
            if ( p.equals("\\Q{\\E") ) return Pattern.quote("}");
            return p;
        } else {
            StringBuilder entireListOfComponents = new StringBuilder();
            String potentialRoot = PrintablePomTaggedExpressionUtility.getInternalNodeCommand(pte);
            entireListOfComponents.append(Pattern.quote(potentialRoot));
            if ( !potentialRoot.isBlank() ) entireListOfComponents.append(SPACE_PATTERN);

            for ( int i = 0; i < components.size(); i++ ) {
                PomTaggedExpression last = components.get(i);
                if ( PomTaggedExpressionUtility.isSequence(last) ) {
                    for ( PomTaggedExpression child : last.getComponents() ) {
                        entireListOfComponents.append(SPACE_PATTERN)
                                .append(getEndingString(child));
                    }
                } else if ( !last.getComponents().isEmpty() ) {
                    String root = PrintablePomTaggedExpressionUtility.getInternalNodeCommand(last);
                    entireListOfComponents.append(Pattern.quote(root));
                    for ( PomTaggedExpression child : last.getComponents() ) {
                        entireListOfComponents.append(SPACE_PATTERN)
                                .append(getEndingString(child));
                    }
                } else {
                    entireListOfComponents.append(getEndingString(components.get(i)));
                }
                if ( i < components.size()-1 )
                    entireListOfComponents.append(SPACE_PATTERN);
            }

            return entireListOfComponents.toString();
        }
    }

    private String getEndEnvironmentPattern(PomTaggedExpression pte) {
        if ( PomTaggedExpressionUtility.isTeXEnvironment(pte) ) {
            String feature = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
            String endString = feature.split("\\.{3}")[1];
            return Pattern.quote(endString);
        } else return null;
    }

    private int checkIndexForClosingBrackets(int start, int end, String expression) {
        if (expression.length() == 0) return 0;

        String sub = expression.substring(start, end);
        int opened = countOpenBrackets(sub);

        return getEndIndex(opened, end, expression);
    }

    private int countOpenBrackets(String sub) {
        int opened = 0;
        for (int i = 0; i < sub.length(); i++) {
            if (isBracket(sub, i, '{')) opened++;
            else if (isBracket(sub, i, '}')) opened--;
        }
        return opened;
    }

    private boolean isBracket(String sub, int i, char bracketSymb) {
        if ( i > 0 && sub.charAt(i-1) == '\\' ) return false;
        return sub.charAt(i) == bracketSymb;
    }

    private int getEndIndex(int opened, int end, String expression) {
        while (opened > 0 && end < expression.length()) {
            if (expression.charAt(end) == '}') {
                end++;
                opened--;
            } else end++;
        }

        return end;
    }

}
