package gov.nist.drmf.interpreter.maple;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

/**
 * Created by AndreG-P on 27.02.2017.
 */
public class TranslatedExpressionTests {

    private TranslatedList transList;

    @BeforeEach
    public void setup(){
        transList = new TranslatedList();
    }

    @Test
    public void emptyTest(){
        String empty = transList.getAccurateString();
        assertEquals( "", empty, "The default translated list is not empty." );
    }

    /**
     * Test: -2
     */
    @Test
    public void singleNegativeExpression(){
        transList.addTranslatedExpression("-2");
        String result = transList.getAccurateString().replaceAll("\\s+","");
        assertTrue(
                result.matches("-2"),
                "Expected -2! But get: " + result );
    }

    /**
     * Test: -\left( 2+x \right)
     * Attention: We are using \left( \right) as default parenthesis
     * @see gov.nist.drmf.interpreter.maple.parser.MapleInterface#DEFAULT_LATEX_BRACKET
     */
    @Test
    public void singleEmbracedNegativeExpression(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");
        transList.setSign( NEGATIVE );

        String result = transList.getAccurateString().replaceAll("\\s+","");
        String regex = "-\\\\left\\(2\\+x\\\\right\\)";

        assertTrue(
                result.matches(regex),
                "Expected -\\left( 2+x \\right)! But get: " + result );
    }

    /**
     * Test: 2+2
     */
    @Test
    public void simpleSummation(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("2");

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("2\\+2"),
                "Expected 2+2! But get: " + result );
    }

    /**
     * Test: 2-2
     */
    @Test
    public void simpleSubtraction(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression( new TranslatedExpression( "2", NEGATIVE ));

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("2-2"),
                "Expected 2-2! But get: " + result );
    }

    /**
     * Test: 2*2
     */
    @Test
    public void simpleProduct(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("*");
        transList.addTranslatedExpression("2");

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("2\\*2"),
                "Expected 2*2! But get: " + result );
    }

    /**
     * Test: 2*(-2)
     */
    @Test
    public void simpleNegativeProduction(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("*");
        transList.addTranslatedExpression( new TranslatedExpression("2", NEGATIVE) );

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("2\\*\\(-2\\)"),
                "Expected 2*(-2)! But get: " + result );
    }

    /**
     * Test: 2-2+x
     */
    @Test
    public void summation(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression( new TranslatedExpression("2", NEGATIVE) );
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("2-2\\+x"),
                "Expected 2-2+x! But get: " + result );
    }

    /**
     * Test: \sin@{x}-\cpi+x
     */
    @Test
    public void intermediateLaTeX(){
        transList.addTranslatedExpression("\\sin@{x}");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("-\\cpi");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("\\\\sin@\\{x}-\\\\cpi\\+x"),
                "Expected \\sin@{x}-\\cpi+x! But get: " + result );
    }

    /**
     * Test: -( \cpi+x )
     */
    @Test
    public void brackets(){
        transList.addTranslatedExpression("\\cpi");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");
        transList.embrace( Brackets.left_parenthesis );
        transList.setSign( NEGATIVE );

        String result = transList.getAccurateString().replaceAll("\\s+", "");
        String left = '\\' + Brackets.left_parenthesis.symbol;
        String right = '\\' + Brackets.left_parenthesis.counterpart;
        String regex = "-" + left + "\\\\cpi\\+x" + right;

        assertTrue(
                result.matches(regex),
                "Expected -( \\cpi+x )! But get: " + result );
    }

    /**
     * Test: 2-\left( \cpi+x \right)
     * Attention: We are using \left( \right) as default parenthesis
     * @see gov.nist.drmf.interpreter.maple.parser.MapleInterface#DEFAULT_LATEX_BRACKET
     */
    @Test
    public void mergesWithoutEmbrace(){
        transList.addTranslatedExpression("\\cpi");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");
        transList.setSign( NEGATIVE );

        TranslatedList tl = new TranslatedList();
        tl.addTranslatedExpression( "2" );
        tl.addTranslatedExpression( "+" );
        tl.addTranslatedExpression( transList );

        String result = tl.getAccurateString().replaceAll("\\s+", "");
        String regex = "2-\\\\left\\(\\\\cpi\\+x\\\\right\\)";

        assertTrue(
                result.matches(regex),
                "Expected 2-\\left( \\cpi+x \\right)! But get: " + result );
    }

    /**
     * Test: -x-3+\iunit
     */
    @Test
    public void complexNumberTest(){
        TranslatedExpression three = new TranslatedExpression("\\iunit", NEGATIVE);
        TranslatedExpression two = new TranslatedExpression("3", NEGATIVE);
        TranslatedExpression one = new TranslatedExpression("x", NEGATIVE);

        transList.addTranslatedExpression(one);
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression(two);
        transList.addTranslatedExpression("-");
        transList.addTranslatedExpression(three);

        String result = transList.getAccurateString().replaceAll("\\s+", "");
        assertTrue(
                result.matches("-x-3\\+\\\\iunit"),
                "Expected -x-3+\\iunit! But get: " + result );
    }

    /**
     * Test: (-3-x)^{-\iunit}
     */
    @Test
    public void powerTest(){
        TranslatedExpression exponent1 = new TranslatedExpression("\\iunit", NEGATIVE);
        TranslatedExpression base1 = new TranslatedExpression("3", NEGATIVE);
        TranslatedExpression base2 = new TranslatedExpression("x", NEGATIVE);

        TranslatedList base = new TranslatedList();
        base.addTranslatedExpression( base1 );
        base.addTranslatedExpression( " + ");
        base.addTranslatedExpression( base2 );
        base.embrace( Brackets.left_parenthesis );

        TranslatedList exponent = new TranslatedList();
        exponent.addTranslatedExpression( exponent1 );
        exponent.embrace( Brackets.left_braces );

        transList.addTranslatedExpression( base );
        transList.addTranslatedExpression( "^" );
        transList.addTranslatedExpression( exponent );

        String result = transList.getAccurateString().replaceAll("\\s+", "");
        assertTrue(
                result.matches("\\(-3-x\\)\\^\\{-\\\\iunit}"),
                "Expected (3+x)^\\left(-\\iunit\\right)! But get: " + result );
    }
}
