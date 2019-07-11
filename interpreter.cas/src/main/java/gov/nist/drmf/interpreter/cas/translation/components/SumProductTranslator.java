package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;

import mlp.PomTaggedExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * SumProductTranslator needs its own class because PomTagger does not recognize \sum.
 * Normally something like \sum would be translated fine in Empty Expression Translator,
 * but because the root term for \sum is something other than what it is for other
 * empty expressions, it cannot be translated there.
 *
 * SumProductTranslator manually added "Sum[]" to local and global exp.
 * It makes addToArgs true, signaling to other classes that there has been a sum
 * and it needs arguments.
 * These arguments are stored in an ArrayList, sumArgs.
 * At the very end of the translation in SemanticLatexTranslator,
 * the final translated expression is cut up and the arguments to sum
 * are inserted in the right places.
 *
 * @author Rajen Dey
 *
 * July 2019
 */
public class SumProductTranslator extends AbstractListTranslator{

    public static ArrayList<String> sumArgs = new ArrayList<>();

    public static boolean addToArgs = false;

    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        //If the CAS is Mathematica do this
        if(GlobalConstants.CAS_KEY.equals("Mathematica")) {
            //If its a sum add "Sum[]"
            if(exp.getRoot().getTermText().equals("\\sum")) {
                local_inner_exp.addTranslatedExpression("Sum[]");
                global_exp.addTranslatedExpression("Sum[]");
                //Otherwise its a product so add "Prod[]"
            } else {
                local_inner_exp.addTranslatedExpression("Prod[]");
                global_exp.addTranslatedExpression("Prod[]");
            }
        }

        //If its Maple do this
        if(GlobalConstants.CAS_KEY.equals("Maple")){
            //if its a sum add "sum()"
            if(exp.getRoot().getTermText().equals("\\sum")) {
                local_inner_exp.addTranslatedExpression("sum()");
                global_exp.addTranslatedExpression("sum()");
                //Otherwise it is a product so add "product()"
            } else {
                local_inner_exp.addTranslatedExpression("product()");
                global_exp.addTranslatedExpression("product()");
            }
        }
        //let other classes know to start adding arguments to sumArgs.
        addToArgs = true;
        return true;
    }
}