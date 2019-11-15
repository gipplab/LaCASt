package gov.nist.drmf.interpreter.tests;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by Andre Greiner-Petter on 10.11.2016.
 */
public class LittlePatternTest {

    @Test
    void patternTests(){
        Pattern p = Pattern.compile("(Capital)?(.+)");
        Matcher m = p.matcher("CapitalAlpha");
        System.out.println(m.matches());
        if ( !m.matches() ) fail("No matches! o.O");
        System.out.println(m.groupCount());
        for (int i = 0; i < m.groupCount(); i++)
            System.out.println(m.toMatchResult().group(i));

    }
}
