package gov.nist.drmf.interpreter.maple.parser.components;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.common.grammar.IParser;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public class AlgebraicListParser implements IParser<List> {

    private final String SYNTAX_REGEX = "_Inert_([A-Z]+)";

    private String translatedExpression;

    public AlgebraicListParser (){
        translatedExpression = "";
    }

    @Override
    public boolean parse(List expression)  {
        try{
            int length = expression.length();

            Algebraic root_alg = expression.select(1);
            // the root object is never a list, it is a maple internal representation
            String root = root_alg.toString();


            if ( root_alg instanceof List ){
                translatedExpression = root_alg.toString();
                // TODO recursive!
            } else {
                String root_str = root_alg.toString();
                translatedExpression = root_str;
                // TODO depending on the root, delegate the task to other parsers.
            }
        } catch (MapleException me){
            MapleInterface.LOG.severe("Cannot parse length of List object. " + me.toString());
            return false;
        }
        return true;
    }

    @Override
    public String getTranslatedExpression() {
        return translatedExpression;
    }

    public String extractSyntax( String inert ){
        return null;
    }
}
