package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class TeXPreProcessorTest {

    @Test
    public void displayStyleTest(){
        String input = "{\\displaystyle{\\displaystyle{\\displaystyle\\ctsHahn{n}@{x}{a}{b}{c}{d}{}={%&#10;\\mathrm{i}^{n}}\\frac{\\pochhammer{a+c}{n}\\pochhammer{a+d}{n}}{n!}\\,\\HyperpFq{3}%&#10;{2}@@{-n,n+a+b+c+d-1,a+\\mathrm{i}x}{a+c,a+d}{1}}}}";
        String expect = "{{\\ctsHahn{n}@{x}{a}{b}{c}{d}{}={%&#10;\\mathrm{i}^{n}}\\frac{\\pochhammer{a+c}{n}\\pochhammer{a+d}{n}}{n!}\\HyperpFq{3}%&#10;{2}@@{-n,n+a+b+c+d-1,a+\\mathrm{i}x}{a+c,a+d}{1}}}";

        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output, "Clear displaystyle didn't work." );
    }

    @Test
    public void displayStyleCommaTest(){
        String input = "{\\displaystyle \\zeta(s) =\\sum_{n=1}^\\infty\\frac{1}{n^s} ,}";

        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( "\\zeta(s) =\\sum_{n=1}^\\infty\\frac{1}{n^s}", output, "Clear displaystyle didn't work." );
    }

    @Test
    public void paranthesisTest(){
        String input = "\\bigl( x \\bigr) \\bigg/ \\Big( y \\Big)";
        String expect = "( x ) / ( y )";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output );
    }

    @Test
    public void lineBreakSpacingTest(){
        String input = "a \\\\[5pt] b";
        String expect = "a \\\\ b";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output );
    }

    @Test
    public void imagePartTest(){
        String input = "\\Im (z+1)";
        String expect = "\\imagpart (z+1)";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output );
    }

    @Test
    public void nonImagePartTest(){
        String input = "\\Image z";
        String expect = "\\Image z";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output );
    }

    @Test
    public void realPartTest(){
        String input = "\\Re z";
        String expect = "\\realpart z";
        String output = TeXPreProcessor.preProcessingTeX( input );
        assertEquals( expect, output );
    }

    @Test
    public void stylesTest(){
        String input = "{\\sf\\bf a}";
        String expect = "a";
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

    @Test
    public void bracketTest() {
        String in = "{{test}}";
        assertTrue(TeXPreProcessor.wrappedInCurlyBrackets(in), in);

        in = "{{test} + {k}}";
        assertTrue(TeXPreProcessor.wrappedInCurlyBrackets(in), in);

        in = "{a + {test}}";
        assertTrue(TeXPreProcessor.wrappedInCurlyBrackets(in), in);

        in = "{ a + { test }} + {x}";
        assertFalse(TeXPreProcessor.wrappedInCurlyBrackets(in), in);
    }

    @Test
    public void bracketLnTest() {
        String in = "{\\sqrt{1-k^2}}^{-1}\\ln{\\Jacobielldck{x}{k}+\\sqrt{1-k^2}\\Jacobiellsck{x}{k}}";
        assertFalse(TeXPreProcessor.wrappedInCurlyBrackets(in));
    }

    @Test
    public void reduceAtsTest() {
        String in = "\\Jacobiellsnk@{\\NVar{z}}{\\NVar{k}}";
        assertEquals(in, TeXPreProcessor.resetNumberOfAtsToOne(in));

        String twoAts = "\\Jacobiellsnk@@{\\NVar{z}}{\\NVar{k}}";
        String threeAts = "\\Jacobiellsnk@@@{\\NVar{z}}{\\NVar{k}}";
        assertEquals(in, TeXPreProcessor.resetNumberOfAtsToOne(twoAts));
        assertEquals(in, TeXPreProcessor.resetNumberOfAtsToOne(threeAts));
    }

    @Test
    public void reduceMultiAtsTest() {
        String in = "\\Jacobiellsnk@{\\macro@{z}}";
        assertEquals(in, TeXPreProcessor.resetNumberOfAtsToOne(in));

        String twoAts = "\\Jacobiellsnk@@@{\\macro@@{z}}";
        String threeAts = "\\Jacobiellsnk@{\\macro@@@{z}}";
        assertEquals(in, TeXPreProcessor.resetNumberOfAtsToOne(twoAts));
        assertEquals(in, TeXPreProcessor.resetNumberOfAtsToOne(threeAts));
    }

    @Test
    public void normalizeGenFracTest() {
        String in = "\\theta\\genfrac{[}{]}{0pt}{}{#1}{#2}";
        String out = TeXPreProcessor.normalizeGenFrac(in);
        assertEquals("\\theta\\left[{#1 \\atop #2}\\right]", out);
    }

    @Test
    public void normalizeGenFracAngleBracketTest() {
        String in = "\\genfrac{<}{>}{0pt}{}{#1}{#2}";
        String out = TeXPreProcessor.normalizeGenFrac(in);
        assertEquals("\\left<{#1 \\atop #2}\\right>", out);
    }

    @Test
    public void normalizeGenFracParenthesisTest() {
        String in = "\\genfrac(){0pt}{}{#1}{#2}";
        String out = TeXPreProcessor.normalizeGenFrac(in);
        assertEquals("\\left({#1 \\atop #2}\\right)", out);
    }
}
