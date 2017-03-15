package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.MString;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.MapleTranslationException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleFunction;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleLexicon;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;

import java.util.Arrays;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class FunctionAndVariableTranslator extends ListTranslator {
    FunctionAndVariableTranslator( MapleInternal internal, int length ){
        super( internal, length );
    }

    @Override
    public boolean translate( List list ) throws TranslationException, MapleException {
        boolean b;
        switch ( root ){
            case string:
                wrapString( list );
                return true;
            case name:
                b = translateName( list );
                LOG.trace( "Translated " + root + ". " + translatedList.getLastExpression() );
                return b;
            case ass_name:
                Algebraic a = list.select(2);
                String msg = "AssignedNames are not allowed in this program. " +
                        "To find this here, means you previously defined the " +
                        "name of the object. " + a.toString() + ". But this is not allowed!";
                LOG.warn(msg);
                failures.addFailure( msg, this.getClass(), list.toString() );
                return false;
            case function:
                translateFunction( list );
                return true;
            case power:
                translatePower( list );
                return true;
            case divide:
                translateFraction( list );
                return true;
            default:
                failures.addFailure( "Wrong Parser for given element.", this.getClass(), list.toString() );
                LOG.debug("Cannot translate " + root + " in FunctionAndVariableTranslator.");
                return false;
        }
    }

    private void wrapString( List list ) throws MapleException {
        Algebraic a = list.select(2);
        MString ms = (MString)a;
        String out = ms.stringValue();
        out = "\\text{" + out + "}";
        translatedList.addTranslatedExpression(out);
    }

    private boolean translateName( List list ) throws MapleException {
        Algebraic a = list.select(2);
        if ( !(a instanceof MString) ){
            failures.addFailure( "Expecting an MString!", this.getClass(), a.toString() );
            return false;
        }

        // get string value
        MString ms = (MString)a;
        String str = ms.stringValue();

        // this string could be a greek letter or a constant.
        TranslatedExpression t;
        MapleInterface mi = MapleInterface.getUniqueMapleInterface();
        GreekLetters greek = mi.getGreekTranslator();
        Constants constants = mi.getConstantsTranslator();

        // first looking for constants
        String constant = constants.translate( str );
        if ( constant != null ) {
            infos.addGeneralInfo(str, "Translated "+ str +" constant in string to " + constant);
            t = new TranslatedExpression(constant);
        } else { // second looking for greek letters
            String greekResult = greek.translate( str );
            if ( greekResult != null ){
                infos.addGeneralInfo( str, "Translated "+ str +" as greek latter to " + greekResult );
                t = new TranslatedExpression(greekResult);
            }
            else t = new TranslatedExpression(str);
        }

        // put the translation into the list
        translatedList.addTranslatedExpression( t );
        return true;
    }

    private void translateFunction( List list ) throws MapleTranslationException, MapleException {
        if ( list.length() != 3 )
            throw new MapleTranslationException(
                    "Illegal length of function list. Length " + list.length());

        List assigned_name_list = (List)list.select(2);
        List expression_seq_list = (List)list.select(3);

        MapleInternal in = getAbstractInternal( assigned_name_list.select(1).toString() );
        if ( !in.equals( MapleInternal.ass_name ) )
            throw new MapleTranslationException("Function doesn't contain assigned name!");
        in = getAbstractInternal( expression_seq_list.select(1).toString() );
        if ( !in.equals( MapleInternal.exp ) )
            throw new MapleTranslationException("Functions arguments are not in an EXPSEQ!");

        MString func_name_string = (MString)assigned_name_list.select(2);
        String function = func_name_string.stringValue();

        LOG.info("Found function: " + function);

        // translate arguments first.
        String[] arguments = translateExpressionSequence( expression_seq_list );
        LOG.info("Extracted arguments of function " + function + ": " + Arrays.toString(arguments));

        // translate function
        MapleLexicon lexicon = MapleLexicon.getLexicon();
        MapleFunction mapleFunction = lexicon.getFunction( function, arguments.length );
        String translation = mapleFunction.replacePlaceHolders( arguments );
        translatedList.addTranslatedExpression( translation );
    }

    private void translatePower( List list ) throws TranslationException, MapleException {
        List base, exponent;

        try {
            base = (List)list.select(2);
            exponent = (List)list.select(3);
        } catch ( MapleException me ){
            throw new TranslationException(
                    Keys.KEY_MAPLE,
                    Keys.KEY_LATEX,
                    "Cannot translate power. Fail to extract base and exponent.",
                    me
            );
        }

        MapleInternal in = getAbstractInternal(base.select(1).toString());
        TranslatedList trans_base = translateGeneralExpression( base );
        if ( trans_base.getLength() > 1 )
            trans_base.embrace();
        else if ( in.equals( MapleInternal.divide ) )
            trans_base.embrace( Brackets.left_latex_parenthesis );

        TranslatedList trans_exponent = translateGeneralExpression( exponent );
        trans_exponent.embrace( Brackets.left_braces );

        translatedList.addTranslatedExpression( trans_base );
        translatedList.addTranslatedExpression( GlobalConstants.CARET_CHAR );
        translatedList.addTranslatedExpression( trans_exponent );
        LOG.trace("Translated POWER. " + trans_base + GlobalConstants.CARET_CHAR + trans_exponent);
    }

    private void translateFraction( List list ) throws TranslationException, MapleException {
        List numerator, denominator;
        boolean sign = MapleConstants.POSITIVE;

        try {
            numerator = (List)list.select(2);
            denominator = (List)list.select(3);
        } catch ( MapleException me ){
            throw new TranslationException(
                    Keys.KEY_MAPLE,
                    Keys.KEY_LATEX,
                    "Cannot translate fraction. Fail to extract numerator and denominator.",
                    me
            );
        }

        LOG.trace("Extract numerator and denominator.");
        TranslatedList numerator_trans = translateGeneralExpression( numerator );
        LOG.info("Numerator Sign: " + numerator_trans.getSign() );
        LOG.info("Numerator: " + numerator_trans.toString());
        LOG.trace("Translated numerator!");
        TranslatedList denominator_trans = translateGeneralExpression( denominator );
        LOG.trace("Translated denominator!");

        if ( numerator_trans.isNegative() ){
            numerator_trans.setSign( MapleConstants.POSITIVE );
            sign = MapleConstants.NEGATIVE;
            LOG.trace("Found negative numerator and switch it!");
        }

        String[] args = new String[]{
                numerator_trans.getAccurateString(),
                denominator_trans.getAccurateString()
        };

        // get the pattern for fraction from the function translator
        // and replace the place holders by numerator and denominator.
        MapleInterface mi = MapleInterface.getUniqueMapleInterface();
        BasicFunctionsTranslator funcTrans = mi.getBasicFunctionsTranslator();
        String pattern = funcTrans.translate( args, Keys.MLP_KEY_FRACTION );
        LOG.debug("Translated fraction: " + ((sign == MapleConstants.NEGATIVE) ? "-":"") + pattern);

        TranslatedExpression trans = new TranslatedExpression( pattern, sign );
        translatedList.addTranslatedExpression( trans );
    }
}
