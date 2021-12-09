package gov.nist.drmf.interpreter.maple.translation.components;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.maple.wrapper.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.MString;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;
import gov.nist.drmf.interpreter.maple.wrapper.MapleList;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.exceptions.MapleTranslationException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleFunction;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleLexicon;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;

import java.util.Arrays;

/**
 *
 * Created by AndreG-P on 28.02.2017.
 */
public class FunctionAndVariableTranslator extends ListTranslator {
    private static final String MOD_NAME = "modulo";

    FunctionAndVariableTranslator( MapleInternal internal, int length ){
        super( internal, length );
    }

    @Override
    public Boolean translate( MapleList list ) throws TranslationException {
        try {
            return innerTranslate( list );
        } catch (MapleException me) {
            throw createException("Maple error in list translator.", me);
        }
    }

    private boolean innerTranslate( MapleList list ) throws TranslationException, MapleException {
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

    private void wrapString( MapleList list ) throws MapleException {
        Algebraic a = list.select(2);
        MString ms = MString.cast(a);
        String out = ms.stringValue();
        out = "\\text{" + out + "}";
        translatedList.addTranslatedExpression(out);
    }

    private boolean translateName( MapleList list ) throws MapleException {
        Algebraic a = list.select(2);
        if ( !(MString.isInstance(a)) ){
            failures.addFailure( "Expecting an MString!", this.getClass(), a.toString() );
            return false;
        }

        // get string value
        MString ms = (MString)a;
        String str = ms.stringValue();

        // this string could be a greek letter or a constant.
        TranslatedExpression t;
        MapleTranslator mi = MapleTranslator.getDefaultInstance();
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

    private void translateFunction( MapleList list ) throws MapleTranslationException, MapleException {
        if ( list.length() != 3 )
            throw new MapleTranslationException(
                    "Illegal length of function list. Length " + list.length());

        MapleList assigned_name_list = MapleList.cast(list.select(2));
        MapleList expression_seq_list = MapleList.cast(list.select(3));

        MapleInternal in = getAbstractInternal( assigned_name_list.select(1).toString() );
        if ( !(in.equals( MapleInternal.ass_name ) || in.equals( MapleInternal.name )) )
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

        /**
         * Special case for modulo
         */
        if ( function.matches("mod[sp]?") ){
            BasicFunctionsTranslator bft =
                    MapleTranslator.getDefaultInstance().getBasicFunctionsTranslator();
            String translation = bft.translate( arguments, MOD_NAME );
            LOG.info("Translated modulo: " + translation);
            translatedList.addTranslatedExpression( translation );
            infos.addMacroInfo( function, " Translated as modulo." );
            return;
        }

        // translate function
        MapleLexicon lexicon = MapleLexicon.getLexicon();
        MapleFunction mapleFunction = lexicon.getFunction( function, arguments.length );
        if ( mapleFunction == null ){
            LOG.warn("Not able to translate function " + function +
                    " with " + arguments.length + " number of arguments.");
        } else {
            String translation = mapleFunction.replacePlaceHolders( arguments );
            LOG.info("Function translated: " + translation);
            translatedList.addTranslatedExpression( translation );
            infos.addMacroInfo( function, mapleFunction.toString() );
        }

    }

    private void translatePower( MapleList list ) throws TranslationException, MapleException {
        MapleList base, exponent;

        try {
            base = MapleList.cast(list.select(2));
            exponent = MapleList.cast(list.select(3));
        } catch ( MapleException me ){
            throw createException(
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
        trans_exponent.embrace( Brackets.left_braces_tex_sequence );

        translatedList.addTranslatedExpression( trans_base );
        translatedList.addTranslatedExpression( GlobalConstants.CARET_CHAR );
        translatedList.addTranslatedExpression( trans_exponent );
        LOG.trace("Translated POWER. " + trans_base + GlobalConstants.CARET_CHAR + trans_exponent);
    }

    private void translateFraction( MapleList list ) throws TranslationException, MapleException {
        MapleList numerator, denominator;
        boolean sign = MapleConstants.POSITIVE;

        try {
            numerator = MapleList.cast(list.select(2));
            denominator = MapleList.cast(list.select(3));
        } catch ( MapleException me ){
            throw createException(
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
        MapleTranslator mi = MapleTranslator.getDefaultInstance();
        BasicFunctionsTranslator funcTrans = mi.getBasicFunctionsTranslator();
        String pattern = funcTrans.translate( args, Keys.MLP_KEY_FRACTION );
        LOG.debug("Translated fraction: " + ((sign == MapleConstants.NEGATIVE) ? "-":"") + pattern);

        TranslatedExpression trans = new TranslatedExpression( pattern, sign );
        translatedList.addTranslatedExpression( trans );
    }
}
