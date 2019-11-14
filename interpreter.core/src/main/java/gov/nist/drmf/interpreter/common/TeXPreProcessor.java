package gov.nist.drmf.interpreter.common;

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

    private TeXPreProcessor() {}

    public static String preProcessingTeX( String tex ){
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
}
