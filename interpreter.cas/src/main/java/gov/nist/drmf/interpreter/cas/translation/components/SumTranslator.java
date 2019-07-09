package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;

import mlp.PomTaggedExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * SumTranslator needs its own class because PomTagger does not recognize \sum.
 * Normally something like \sum would be translated fine in Empty Expression Translator,
 * but because the root term for \sum is something other than what it is for other
 * empty expressions, it cannot be translated there.
 *
 * SumTranslator manually added "Sum[]" to local and global exp.
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
public class SumTranslator extends AbstractListTranslator{

    public static ArrayList<String> sumArgs = new ArrayList<>();

    public static boolean addToArgs = false;

    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        local_inner_exp.addTranslatedExpression("Sum[]");
        global_exp.addTranslatedExpression("Sum[]");
        addToArgs = true;
        return true;
    }
}