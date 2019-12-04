package gov.nist.drmf.interpreter.tests;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class TeXPreProcessorTest {

    @Test
    public void displayStyleTest(){
        String input = "{\\displaystyle{\\displaystyle{\\displaystyle\\ctsHahn{n}@{x}{a}{b}{c}{d}{}={%&#10;\\mathrm{i}^{n}}\\frac{\\pochhammer{a+c}{n}\\pochhammer{a+d}{n}}{n!}\\,\\HyperpFq{3}%&#10;{2}@@{-n,n+a+b+c+d-1,a+\\mathrm{i}x}{a+c,a+d}{1}}}}";
        String expect = "{{{\\ctsHahn{n}@{x}{a}{b}{c}{d}{}={%&#10;\\mathrm{i}^{n}}\\frac{\\pochhammer{a+c}{n}\\pochhammer{a+d}{n}}{n!}\\,\\HyperpFq{3}%&#10;{2}@@{-n,n+a+b+c+d-1,a+\\mathrm{i}x}{a+c,a+d}{1}}}}";

        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output, "Clear displaystyle didn't work." );
    }

    @Test
    public void paranthesisTest(){
        String input = "\\bigl( x \\bigr) \\bigg/ \\Big( y \\Big)";
        String expect = "( x ) / ( y )";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output );
    }

    @Test
    public void stylesTest(){
        String input = "{\\sf\\bf a}";
        String expect = "{ a}";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output);
    }

    @Test
    public void hiderelTest(){
        String input = "a \\hiderel{ - } b \\hiderel{=} c \\hiderel{ /} d";
        String expect = "a - b = c / d";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output);
    }

    @Test
    public void endingCommasTest(){
        String input = "a+b=x; .";
        String expect = "a+b=x";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output);
    }

    @Test
    public void numericalTest(){
        String input = "2.03\\;04\\,33 1\\* x";
        String i = "2.71828 \\ 18284 \\ 59045 \\  23536";
        String expect = "2.0304331* x";
        String iExp = "2.71828182845904523536";
        String output = TeXPreProcessor.preProcessingTeX( input );
        String iOut = TeXPreProcessor.preProcessingTeX( i );
        assertEquals( expect, output);
        assertEquals( iExp, iOut);
    }
}
