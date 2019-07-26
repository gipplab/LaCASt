package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
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
        //need to store tempNum in case there is a nested sum/product
        int tempNum = num;
        //create a new arraylist to store the arguments to this sum/product
        args.add(new ArrayList<String>());
        PomTaggedExpression next = list.remove(0);

        int numArgs = addToArgs(next, list);

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
     *
     * When size is 0, it is actually 1.
     * @param size
     */
    private void remove(int size){
        if(size != 0)
            global_exp.removeLastNExps(size);
        else
            global_exp.removeLastNExps(1);
    }

    /**
     * This method is called when the sum/prod has only a lower limit defined.
     * Ex: \sum_{x=0}{x+5}
     * @param next
     * @param list
     * @return
     */
    protected int onlyLower(PomTaggedExpression next, List<PomTaggedExpression> list){
        List<PomTaggedExpression> components = next.getComponents();
        int size = components.get(0).getComponents().size();
        //this is the index and lower limit of summation
        PomTaggedExpression lowerLim = components.get(0);
        args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
        remove(size);

        size = list.get(0).getComponents().size();
        //this is the function being summed
        args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
        remove(size);

        return 2;
    }

    /**
     * This method is called when the sum/prod has only an upper limit defined.
     * Ex: \sum^{10}{x+5}
     * @param next
     * @param list
     * @return
     */
    protected int onlyUpper(PomTaggedExpression next, List<PomTaggedExpression> list){
        if(GlobalConstants.CAS_KEY.equals("Maple")){
            throw new TranslationException("Invalid sum/product!");
        }
        List<PomTaggedExpression> components = next.getComponents();
        int size = components.get(0).getComponents().size();

        //upper limit of summation
        PomTaggedExpression upperLim = components.get(0);
        args.get(num).add(parseGeneralExpression(upperLim, list).toString());
        remove(size);

        //find index of summation
        String storeIndex = "";
        List<PomTaggedExpression> listComponents = list.get(0).getComponents();
        for(int i = 0; i < list.get(0).getComponents().size(); i++) {
            char nextChar = listComponents.get(i).getRoot().getTermText().charAt(0);
            if(java.lang.Character.isLetter(nextChar)){
                storeIndex = Character.toString(nextChar);
                break;
            }
        }
        if(!storeIndex.isEmpty())
            args.get(num).add(storeIndex);
        else
            args.get(num).add("i");

        size = list.get(0).getComponents().size();
        //summand
        args.get(num).add(1, parseGeneralExpression(list.remove(0), list).toString());
        remove(size);

        return 1;
    }

    /**
     * This method is called when the sum/prod has no lower or upper limit defined.
     * Ex: \sum{x+5}
     * @param next
     * @param list
     * @param tempNum
     * @return
     */
    protected int none(PomTaggedExpression next, List<PomTaggedExpression> list, int tempNum){
        String storeIndex = "";
        List<PomTaggedExpression> components = next.getComponents();

        //find index of summation
        for(int i = 0; i < components.size(); i++) {
            char nextChar = components.get(i).getRoot().getTermText().charAt(0);
            if(java.lang.Character.isLetter(nextChar)){
                storeIndex = Character.toString(nextChar);
                break;
            }
        }
        int size = components.size();
        //summand
        args.get(num).add(parseGeneralExpression(next, list).toString());
        remove(size);

        //add index of summation
        if(!storeIndex.isEmpty())
            args.get(tempNum).add(storeIndex);
        else
            args.get(tempNum).add("i");

        return 0;
    }

    /**
     * This method is called when the sum/prod has both an upper and a lower limit.
     * Determines which limit (lower/upper) is defined first, and adds them to the sum args appropriately.
     * Ex: \sum_{x=0}^{10}{x+5}
     * Ex: \sum^{10}_{x=0}{x+5}
     *
     * @param next
     * @param list
     * @param tempNum
     * @return
     */
    protected int lowerAndUpper(PomTaggedExpression next, List<PomTaggedExpression> list, int tempNum){
        String storeIndex;
        PomTaggedExpression lowerLim;
        PomTaggedExpression upperLim;
        List<PomTaggedExpression> components = next.getComponents();
        //lower limit defined before upper limit
        if(MathTermTags.getTagByKey(components.get(0).getRoot().getTag()).equals(MathTermTags.underscore)){
            List<PomTaggedExpression> components2 = components.get(0).getComponents();
            //Store the index of summation.
            try{
                List<PomTaggedExpression> components3 = components2.get(0).getComponents();
                storeIndex = components3.get(0).getRoot().getTermText();
            } catch(Exception e){
                storeIndex = components2.get(0).getRoot().getTermText();
            }
            lowerLim = components2.get(0);
            List<PomTaggedExpression> upperComponents = components.get(1).getComponents();
            upperLim = upperComponents.get(0);
        } //upper limit defined before lower limit
        else {
            List<PomTaggedExpression> components2 = components.get(1).getComponents();
            //Store the index of summation.
            try {
                List<PomTaggedExpression> components3 = components2.get(0).getComponents();
                storeIndex = components3.get(0).getRoot().getTermText();
            } catch(Exception e){
                storeIndex = components2.get(0).getRoot().getTermText();
            }
            lowerLim = components2.get(0);
            List<PomTaggedExpression> upperComponents = components.get(0).getComponents();
            upperLim = upperComponents.get(0);
        }

        //lower limit of summation
        int size = lowerLim.getComponents().size();
        args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
        //remove the expression that parseGeneralExpression added to global_exp, because that expression is being used as a sum arg.
        remove(size);

        //upper limit of summation
        size = upperLim.getComponents().size();
        args.get(num).add(parseGeneralExpression(upperLim, list).toString());
        remove(size);

        //summand
        size = list.get(0).getComponents().size();
        args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
        remove(size);

        //add index of summation
        args.get(tempNum).add(storeIndex);
        return  3;
    }

    /**
     * Depending on what the tag is, this method calls different methods to add the right arguments to the arguments list.
     * If the tag is null, the sum has either only a lower limit or only an upper limit.
     * If the tag is sub_super_script, it has both.
     * If the tag is sequence, it has none.
     * Then some subcases are used to determine which exact case it is.
     *
     * @param next
     * @param list
     * @return
     */
    protected int addToArgs(PomTaggedExpression next, List<PomTaggedExpression> list){
        ExpressionTags tag = ExpressionTags.getTagByKey(next.getTag());
        //only upper limit, only lower limit, or no limits with a summand of length 1.
        if(tag == null){
            MathTermTags termTag = MathTermTags.getTagByKey(next.getRoot().getTag());
            //lower limit
            if(termTag.equals(MathTermTags.underscore))
                return onlyLower(next, list);
            //upper limit
            else if(termTag.equals(MathTermTags.caret))
                return onlyUpper(next, list);
            //none
            else
                return none(next, list, num);
        } else {
            switch (tag) {
                //both lower and upper limit
                case sub_super_script:
                    return lowerAndUpper(next, list, num);
                //no limits defined
                case sequence:
                    return none(next, list, num);
                default:
                    throw new TranslationException("");

            }
        }
    }

    protected String getTranslation(){
        String exp = global_exp.toString();
        global_exp.clear();
        return exp;
    }
}