package gov.nist.drmf.core.tests;

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
}
