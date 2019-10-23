package gov.nist.drmf.interpreter.maple.translation.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.maple.grammar.MapleInternal;
import gov.nist.drmf.interpreter.maple.grammar.TranslatedList;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;

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
    public boolean translate( List list ) throws MapleException, IllegalArgumentException {
        SymbolTranslator symbolTranslator =
                MapleInterface.getUniqueMapleInterface().getSymbolTranslator();
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

    private boolean translateRelation( List list, String translated_relation )
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
