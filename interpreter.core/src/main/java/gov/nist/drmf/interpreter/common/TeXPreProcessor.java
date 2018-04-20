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
            "\\\\displaystyle"
    );

    private TeXPreProcessor() {}

    public static String preProcessingTeX( String tex ){
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = IGNORE_FONTS.matcher(tex);

        while ( matcher.find() ){
            matcher.appendReplacement( buffer, "" );
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
