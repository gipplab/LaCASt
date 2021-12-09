package gov.nist.drmf.interpreter.maple.translation.components;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import gov.nist.drmf.interpreter.maple.wrapper.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;
import gov.nist.drmf.interpreter.maple.wrapper.MapleList;

/**
 * Created by AndreG-P on 28.04.2017.
 */
public class RelationTranslator extends ListTranslator {
    RelationTranslator(MapleInternal in, int length) throws IllegalArgumentException {
        super(in, length);
        if ( length != 3 ){
            LOG.error( "Illegal number of arguments in RelationTranslator. " +
                    "Only 2 arguments are allowed." );
            throw new IllegalArgumentException(
                    "Relation objects are only 2 elements long. This has "
                            + length);
        }
    }

    @Override
    public Boolean translate( MapleList list ) throws TranslationException {
        try {
            return innerTranslate( list );
        } catch (MapleException me) {
            throw createException("Maple error in numerical translator.", me);
        }
    }

    public boolean innerTranslate( MapleList list ) throws MapleException, IllegalArgumentException {
        SymbolTranslator symbolTranslator =
                MapleTranslator.getDefaultInstance().getSymbolTranslator();
        String translated_symb;
        switch (root) {
            case equation:
                translated_symb = symbolTranslator.translate(
                        SymbolTranslator.KEY_NAME,
                        Keys.KEY_LATEX,
                        Keys.MLP_KEY_EQ );
                break;
            case ineq:
                translated_symb = symbolTranslator.translate(
                    SymbolTranslator.KEY_NAME,
                    Keys.KEY_LATEX,
                    Keys.MLP_KEY_NEQ );
                break;
            case lesseq:
                translated_symb = symbolTranslator.translate(
                        SymbolTranslator.KEY_NAME,
                        Keys.KEY_LATEX,
                        Keys.MLP_KEY_LEQ );
                break;
            case lessthan:
                translated_symb = "<";
                break;
            default:
                String message = "Expected an relation object but get: " + root;
                LOG.debug( message );
                failures.addFailure( message, this.getClass(), root.toString() );
                return false;
        }
        return translateRelation( list, translated_symb );
    }

    private boolean translateRelation( MapleList list, String translated_relation )
            throws MapleException {
        Algebraic lhs = list.select(2);
        Algebraic rhs = list.select(3);
        LOG.debug("Translate Relation, LHS: "+lhs.toString() +"; RHS: " + rhs);

        TranslatedList translatedLHS = translateGeneralExpression( lhs );
        LOG.debug("LHS translated to: " + translatedLHS);
        TranslatedList translatedRHS = translateGeneralExpression( rhs );
        LOG.debug("RHS translated to: " + translatedRHS);

        translatedList.addTranslatedExpression( translatedLHS );
        translatedList.addTranslatedExpression( translated_relation );
        translatedList.addTranslatedExpression( translatedRHS );
        return true;
    }

}
