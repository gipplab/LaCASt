package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
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

    private static ArrayList<ArrayList<String>> args = new ArrayList<>();

    private static int num = -1;

    @Override
    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        num++;
        int tempNum = num;
        int numArgs;
        //create a new arraylist to store the arguments to this sum/product
        args.add(new ArrayList<String>());
        PomTaggedExpression next = list.remove(0);

        boolean possiblyTwoTerm = false;
        try {
            //if this statement throws an error, then the sum/product maybe has only an upper limit
            next.getComponents().get(1).getRoot().getTag();
        } catch(Exception e){
            possiblyTwoTerm = true;
        }

        //case with only lower limit
        if(next.getRoot().getTermText().equals("_")){
            numArgs = 2;

            int size = next.getComponents().get(0).getComponents().size();
            //this is the index and lower limit of summation
            args.get(num).add(parseGeneralExpression(next.getComponents().get(0), list).toString());
            remove(size);

            size = list.get(0).getComponents().size();
            //this is the function being summed
            args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
            remove(size);

            //case with only upper limit
            //Maple does not handle these
        } else if(next.getComponents().size() == 1 && next.getRoot().getTag().equals("caret") && possiblyTwoTerm){
            if(GlobalConstants.CAS_KEY.equals("Maple")){
                throw new TranslationException("Invalid sum/product!");
            }
            numArgs = 1;
            int size = next.getComponents().get(0).getComponents().size();
            args.get(num).add(parseGeneralExpression(next.getComponents().get(0), list).toString());
            remove(size);

            String storeIndex = "";
            for(int i = 0; i < list.get(0).getComponents().size(); i++) {
                char nextChar = list.get(0).getComponents().get(i).getRoot().getTermText().toCharArray()[0];
                if(java.lang.Character.isLetter(nextChar)){
                    storeIndex = Character.toString(nextChar);
                    break;
                }
            }
            if(!storeIndex.equals(""))
                args.get(num).add(storeIndex);
            else
                args.get(num).add("i");

            size = list.get(0).getComponents().size();
            args.get(num).add(1, parseGeneralExpression(list.remove(0), list).toString());
            remove(size);
        }
        //case with no upper or lower limit
        else if(!"subsuperscript".equals(next.getTag())){
            numArgs = 0;
            String storeIndex = "";
            //index of summation not specified, so assume that it is the first letter encountered.
            for(int i = 0; i < next.getComponents().size(); i++) {
                char nextChar = next.getComponents().get(i).getRoot().getTermText().toCharArray()[0];
                if(java.lang.Character.isLetter(nextChar)){
                    storeIndex = Character.toString(nextChar);
                    break;
                }
            }
            int size = next.getComponents().size();
            args.get(num).add(parseGeneralExpression(next, list).toString());
            remove(size);
            if(!storeIndex.equals(""))
                args.get(tempNum).add(storeIndex);
            else
                args.get(tempNum).add("i");

            //case with lower limit before upper limit
        } else if(next.getTag().equals("subsuperscript") && next.getComponents().get(1).getRoot().getTag().equals("caret")) {
            numArgs = 3;
            //Store the index of summation. Mathematica needs this.
            String storeIndex = "";
            try{
                storeIndex = next.getComponents().get(0).getComponents().get(0).getComponents().get(0).getRoot().getTermText();
            } catch(Exception e){
                storeIndex = next.getComponents().get(0).getComponents().get(0).getRoot().getTermText();
            }
            //this is the index and lower limit of summation
            int size = next.getComponents().get(0).getComponents().get(0).getComponents().size();

            args.get(num).add(parseGeneralExpression(next.getComponents().get(0).getComponents().get(0), list).toString());
            //remove the expression that parseGeneralExpression added to global_exp, because that expression is being used as a sum arg.
            remove(size);
            //this is the upper limit of summation
            size = next.getComponents().get(1).getComponents().get(0).getComponents().size();
            args.get(num).add(parseGeneralExpression(next.getComponents().get(1).getComponents().get(0), list).toString());

            remove(size);

            size = list.get(0).getComponents().size();
            args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());

            remove(size);

            //Add the index of summation to the args.
            args.get(tempNum).add(storeIndex);

            //case with upper limit before lower limit
        } else if (next.getTag().equals("subsuperscript") && !next.getComponents().get(1).getRoot().getTag().equals("caret")) {
            numArgs = 3;
            //Store the index of summation. Mathematica needs this.
            String storeIndex = "";
            try {
                storeIndex = next.getComponents().get(1).getComponents().get(0).getComponents().get(0).getRoot().getTermText();
            } catch(Exception e){
                storeIndex = next.getComponents().get(1).getComponents().get(0).getRoot().getTermText();
            }

            int size = next.getComponents().get(0).getComponents().get(0).getComponents().size();
            args.get(num).add(parseGeneralExpression(next.getComponents().get(0).getComponents().get(0), list).toString());
            remove(size);

            size = next.getComponents().get(1).getComponents().get(0).getComponents().size();
            //this is the upper limit of summation
            args.get(num).add(parseGeneralExpression(next.getComponents().get(1).getComponents().get(0), list).toString());
            remove(size);

            args.get(num).add(args.get(num).remove(0));

            size = list.get(0).getComponents().size();
            args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
            remove(size);
            //Add the index of summation to the args.
            args.get(tempNum).add(storeIndex);
        } else {
            throw new TranslationException("");
        }

        //put the args from the arraylist into an array so that it can be passed as an argument to parseBasicFunction
        String[] argsArray = new String[args.get(tempNum).size()];
        for(int i = 0; i < args.get(tempNum).size(); i++){
            argsArray[i] = args.get(tempNum).get(i);
        }
        local_inner_exp.addTranslatedExpression(SemanticLatexTranslator.getBasicFunctionParser().translate(argsArray, exp.getRoot().getTermText().substring(1) + numArgs));
        global_exp.addTranslatedExpression(SemanticLatexTranslator.getBasicFunctionParser().translate(argsArray, exp.getRoot().getTermText().substring(1) + numArgs));
        return true;
    }

    /**
     * Method to remove expressions that are already being used as an argument to the sum/prod from global_exp
     * so that they don't get reused.
     * When size is 0, it is actually 1.
     * @param size
     */
    public void remove(int size){
        if(size != 0)
            global_exp.removeLastNExps(size);
        else
            global_exp.removeLastNExps(1);
    }

}