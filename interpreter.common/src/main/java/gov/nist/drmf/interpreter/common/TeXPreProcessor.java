package gov.nist.drmf.interpreter.common;

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

    private static ReplacementConfig replacementConfig = ReplacementConfig.getInstance();

    private TeXPreProcessor() {}

    public static String preProcessingTeX( String tex ) {
        return preProcessingTeX(tex, null);
    }

    public static String preProcessingTeX( String tex, String label ) {
        if ( replacementConfig == null ) {
            LOG.warn("No replacement rules loaded, fallback to standard replacements.");
            return fallbackReplacements(tex);
        } else return replacementConfig.replace(tex, label);
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
        return buffer.toString();
    }

    public static String trimCurlyBrackets(String in) {
        in = in.trim();
        if ( in.startsWith("{") && in.endsWith("}") ) return in.substring(1, in.length()-1).trim();
        else return in;
    }

    public static boolean wrappedInCurlyBrackets(String in) {
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
}
