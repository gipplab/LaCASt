package gov.nist.drmf.interpreter.maple;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

/**
 * Created by AndreG-P on 27.02.2017.
 */
public class TranslatedExpressionTest {

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
        System.out.println(transList.toString());

        String result = transList.getAccurateString().replaceAll("\\s+","");
        assertTrue(
                result.matches("-2"),
                "Expected -2! But get: " + result );
    }

    @Test
    public void doubleNegativeExpression(){
        transList.addTranslatedExpression("-2");
        transList.setSign( NEGATIVE );
        System.out.println(transList.toString());

        String result = transList.getAccurateString().replaceAll("\\s+","");
        assertTrue(
                result.matches("2"),
                "Expected 2! But get: " + result );
    }

    /**
     * Expeted the output of ---2 -> -2
     */
    @Test
    public void tripleNegativeExpression(){
        TranslatedExpression neg = new TranslatedExpression("2", NEGATIVE);
        TranslatedList tmp = new TranslatedList();

        tmp.addTranslatedExpression(neg);
        tmp.setSign(NEGATIVE);
        System.out.println(tmp);

        transList.setSign( NEGATIVE );
        transList.addTranslatedExpression(tmp);

        System.out.println(transList.toString());

        String result = transList.getAccurateString().replaceAll("\\s+","");
        assertTrue(
                result.matches("-2"),
                "Expected 2! But get: " + result );
    }

    /**
     * Expeted the output of -(-2) -> -(-2)
     */
    @Test
    public void negativeWithBracketsExpression(){
        transList.addTranslatedExpression("-2");
        transList.embrace( Brackets.left_parenthesis );
        transList.setSign( NEGATIVE );
        System.out.println(transList.toString());

        String result = transList.getAccurateString().replaceAll("\\s+","");
        assertTrue(
                result.matches("-\\(-2\\)"),
                "Expected -(-2)! But get: " + result );
    }

    /**
     * Test: -\left( 2+x \right)
     * Attention: We are using \left( \right) as default parenthesis
     * @see MapleTranslator#DEFAULT_LATEX_BRACKET
     */
    @Test
    public void singleEmbracedNegativeExpression(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");
        transList.embrace();
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
        transList.addTranslatedExpression("\\cdot");

        TranslatedList tl = new TranslatedList();
        tl.addTranslatedExpression( new TranslatedExpression("2", NEGATIVE) );
        tl.embrace( Brackets.left_parenthesis );
        transList.addTranslatedExpression( tl );

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("2\\\\cdot\\s*\\(-2\\)"),
                "Expected 2*(-2)! But get: " + result );
    }

    /**
     * Test: -(2-2+x)
     */
    @Test
    public void summation(){
        transList.addTranslatedExpression("2");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression( new TranslatedExpression("2", NEGATIVE) );
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");
        transList.embrace( Brackets.left_parenthesis );
        transList.setSign( NEGATIVE );

        String result = transList.getAccurateString().replaceAll("\\s+","");

        assertTrue(
                result.matches("-\\(2-2\\+x\\)"),
                "Expected -(2-2+x)! But get: " + result );
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
        String left = Brackets.left_parenthesis.symbol;
        String right = Brackets.left_parenthesis.counterpart;
        String regex = "-" + left + "\\cpi+x" + right;

        Pattern p = Pattern.compile(regex, Pattern.LITERAL);
        Matcher m = p.matcher(result);

        assertTrue(
                m.matches(),
                "Expected -(\\cpi+x)! But get: " + result );
    }

    /**
     * Test: 2-\left( \cpi+x \right)
     * Attention: We are using \left( \right) as default parenthesis
     * @see MapleTranslator#DEFAULT_LATEX_BRACKET
     */
    @Test
    public void mergesWithoutEmbrace(){
        transList.addTranslatedExpression("\\cpi");
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression("x");
        transList.embrace();
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
     * Test: -x-3+\iunit+5
     */
    @Test
    public void complexNumberTest(){
        TranslatedExpression one = new TranslatedExpression("x", NEGATIVE);
        TranslatedExpression two = new TranslatedExpression("3", NEGATIVE);
        TranslatedExpression three = new TranslatedExpression("\\iunit", NEGATIVE);

        TranslatedList last_list = new TranslatedList();
        last_list.addTranslatedExpression( new TranslatedExpression("5", NEGATIVE) );

        transList.addTranslatedExpression(one);
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression(two);
        transList.addTranslatedExpression("-");
        transList.addTranslatedExpression(three);
        transList.addTranslatedExpression("+");
        transList.addTranslatedExpression(last_list);

        System.out.println(transList.toString());

        String result = transList.getAccurateString().replaceAll("\\s+", "");
        assertTrue(
                result.matches("-x-3\\+\\\\iunit-5"),
                "Expected -x-3-\\iunit-5! But get: " + result );
    }

    /**
     * Test: (-3-x)^{-\iunit}
     */
    @Test
    public void powerTest(){
        TranslatedExpression exponent1 = new TranslatedExpression("\\iunit", NEGATIVE);
        TranslatedExpression base1 = new TranslatedExpression("3", NEGATIVE);
        TranslatedExpression base2 = new TranslatedExpression("x", NEGATIVE);

        // build (-3+-x) -> (-3-x)
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
                "Expected (-3-x)^{-\\iunit}! But get: " + result );
    }

    /**
     *
     */
    @Test
    public void negativeTest(){
        TranslatedExpression exponent1 = new TranslatedExpression("\\iunit");

        TranslatedList trans1 = new TranslatedList();
        trans1.addTranslatedExpression( exponent1 );
        trans1.setSign(MapleConstants.NEGATIVE);

        TranslatedList trans0 = new TranslatedList();
        trans0.addTranslatedExpression( "x" );
        trans0.addTranslatedExpression( "+" );

        transList.addTranslatedExpression( trans0 );
        transList.addTranslatedExpression( trans1 );

        String result = transList.getAccurateString().replaceAll("\\s+", "");
        assertTrue(
                result.matches("x-\\\\iunit"),
                "Expected x-\\iunit! But get: " + result );
    }

    @Test
    public void obligatorySpacesTest(){
        transList.addTranslatedExpression(new TranslatedExpression("\\infty", NEGATIVE));
        transList.addTranslatedExpression(new TranslatedExpression("\\cdot "));
        transList.addTranslatedExpression(new TranslatedExpression("x"));
        String result = transList.getAccurateString().trim();
        assertTrue( result.matches("-\\\\infty\\s?\\\\cdot\\s+x"),
                "Expected whitespaces \"-\\infty\\cdot x\"! But get: " + result );
    }
}
