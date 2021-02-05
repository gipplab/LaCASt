package gov.nist.drmf.interpreter.common.latex;

import gov.nist.drmf.interpreter.common.replacements.ReplacementConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class TeXPreProcessor {
    private static final Logger LOG = LogManager.getLogger(TeXPreProcessor.class.getName());

    private static final Pattern IGNORE_FONTS = Pattern.compile(
                    "\\\\displaystyle|" +
                    "\\\\hbox|" +
                    "\\\\[Bb]ig[lrg]?|" +
                    "\\\\[sb]f|" +
                    "[.;<>^/\\t\\s]+$|" +
                    "(\\\\hiderel\\s*\\{\\s*([=<>/+\\-])\\s*})|" +
                    "(\\d)(?:\\s+|\\\\[;,! ])+(\\d)|" +
                    "(\\\\\\*)"
    );

    private static final Pattern EOF_PATTERN = Pattern.compile("^\\s*(.*)\\s*(?:\\\\?[,;.\\\\])+\\s*$");

    private static final Pattern GEN_FRAC_PATTERN = Pattern.compile(
            "\\\\genfrac(\\{.}|.)(\\{.}|.)(?:\\\\z@\\{}|\\{0pt}\\{})\\{(.*?)}\\{(.*?)}"
    );

    private static final Pattern BEGIN_ENV_PATTERN = Pattern.compile("\\\\begin\\{(.*?)}");

    private final static ReplacementConfig replacementConfig = ReplacementConfig.getInstance();

    private TeXPreProcessor() {}

    public static String preProcessingTeX( String tex ) {
        return preProcessingTeX(tex, null);
    }

    public static String preProcessingTeX( String tex, String label ) {
        tex = TeXPreProcessor.trimIfWrappedInCurlyBrackets(tex);
        if ( replacementConfig == null ) {
            LOG.warn("No replacement rules loaded, fallback to standard replacements.");
            tex = fallbackReplacements(tex);
        } else tex = replacementConfig.replace(tex, label);
        return clearEndOfFormulaPunctuation(tex);
    }

    private static String fallbackReplacements( String tex ){
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = IGNORE_FONTS.matcher(tex);

        while ( matcher.find() ){
            if ( matcher.group(1) != null ){
                matcher.appendReplacement( buffer, matcher.group(2) );
            } else if ( matcher.group(3) != null ) {
                matcher.appendReplacement( buffer, matcher.group(3) + matcher.group(4));
            } else if ( matcher.group(5) != null ) {
                matcher.appendReplacement( buffer, "*");
            } else {
                matcher.appendReplacement( buffer, "" );
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    public static String clearEndOfFormulaPunctuation(String in) {
        if ( in == null || in.isBlank() ) return in;
        Matcher m = EOF_PATTERN.matcher(in);
        if ( m.matches() ) return m.group(1).trim();
        else return in;
    }

    public static String trimCurlyBrackets(String in) {
        in = in.trim();
        if ( in.startsWith("{") && in.endsWith("}") ) return in.substring(1, in.length()-1).trim();
        else return in;
    }

    public static String trimIfWrappedInCurlyBrackets(String in) {
        if ( wrappedInCurlyBrackets(in) ) return trimCurlyBrackets(in);
        else return in;
    }

    public static boolean wrappedInCurlyBrackets(String in) {
        if ( in == null || in.isBlank() ) return false;
        if ( !in.trim().startsWith("{") && !in.trim().endsWith("}") ) return false;
        int openCounter = 1;
        for ( int i = 1; i < in.length(); i++ ) {
            if ( openCounter <= 0 ) return false;
            openCounter = updateCounter(in, i, openCounter);
        }
        return openCounter == 0;
    }

    private static int updateCounter(String in, int i, int openCounter) {
        Character c = in.charAt(i);
        if ( c.equals('{') ) openCounter++;
        else if ( c.equals('}') ) openCounter--;
        return openCounter;
    }

    public static String resetNumberOfAtsToOne(String in) {
        // if there are multiple @s, replace it my one @
        return in.replaceAll("@{2,}", "@");
    }

    public static String removeTeXEnvironment(String in) {
        if ( in == null || !in.trim().startsWith("\\begin") ) {
            return in;
        }

        Matcher envTitle = BEGIN_ENV_PATTERN.matcher(in);
        if ( !envTitle.find() ) return in;

        String envTitleString = Pattern.quote(envTitle.group(1));
        Pattern envContentPattern = Pattern.compile(
                "\\\\begin\\{" + envTitleString + "}(?:\\{.*?})*" +
                        "(.*?)" +
                "\\\\end\\{" + envTitleString + "}"
        );
        Matcher envContentMatcher = envContentPattern.matcher(in);
        if ( !envContentMatcher.find() ) return in;
        return envContentMatcher.group(1).trim();
    }

    public static String normalizeGenFrac(String in) {
        Matcher m = GEN_FRAC_PATTERN.matcher(in);
        StringBuilder sb = new StringBuilder();
        while ( m.find() ) {
            String newFrac = "\\\\left" + trimCurlyBrackets(m.group(1));
            newFrac += "{" + m.group(3) + " \\\\atop " + m.group(4) + "}";
            newFrac += "\\\\right" + trimCurlyBrackets(m.group(2));
            m.appendReplacement(sb, newFrac);
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }
}
