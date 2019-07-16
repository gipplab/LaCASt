package gov.nist.drmf.interpreter.cas.translation.components;


import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationException;
import mlp.PomTaggedExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * SumProductTranslator uses parseGeneralExpression to get the arguments to the sum/product.
 * Then it uses BasicFunctionParser to put the arguments where they need to go.
 *
 * @author Rajen Dey
 *
 * July 2019
 */
public class SumProductTranslator extends AbstractListTranslator{

    public static ArrayList<String> args = new ArrayList<>();

    @Override
    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        int numArgs;
        PomTaggedExpression next = list.remove(0);
        //case where there are only 2 terms
        if(next.getRoot().getTermText().equals("_")){
            numArgs = 2;
            //this is the index and lower limit of summation
            args.add(parseGeneralExpression(next.getComponents().get(0), list).toString());
            //this is the function being summed
            args.add(parseGeneralExpression(list.remove(0), list).toString());
            //case with 3 terms
        } else if(next.getTag().equals("subsuperscript")) {
            numArgs = 3;
            //this is the index and lower limit of summation
            args.add(parseGeneralExpression(next.getComponents().get(0).getComponents().get(0), list).toString());
            //this is the upper limit of summation
            args.add(parseGeneralExpression(next.getComponents().get(1).getComponents().get(0), list).toString());
            //if this is true, it means that the caret was first, so rearrange the arguments to reflect this.
            if (!next.getComponents().get(1).getRoot().getTag().equals("caret")) {
                args.add(args.remove(0));
            }
            args.add(parseGeneralExpression(list.remove(0), list).toString());
        } else {
            throw new TranslationException("");
        }

        String[] argsArray = new String[args.size()];
        for(int i = 0; i < args.size(); i++){
            argsArray[i] = args.get(i);
        }
        local_inner_exp.addTranslatedExpression(SemanticLatexTranslator.getBasicFunctionParser().translate(argsArray, exp.getRoot().getTermText().substring(1) + numArgs));
        global_exp.addTranslatedExpression(SemanticLatexTranslator.getBasicFunctionParser().translate(argsArray, exp.getRoot().getTermText().substring(1) + numArgs));
        return true;
    }

    public void test(){

    }
}