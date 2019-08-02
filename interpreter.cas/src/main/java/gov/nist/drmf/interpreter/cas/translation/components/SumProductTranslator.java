package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Find out where summand ends:
 * Multiplication
 * Next in list not
 *
 *
 *
 * \sum_{x=0}^{\infty}2x(x+4)^2+4^x+2(3)
 */


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

    private static ArrayList<String> indices = new ArrayList<>();

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

        addFactorsToSummand(list, tempNum, numArgs);
//        if(tempNum < indices.size() && !indices.get(tempNum).isEmpty())
//            addMoreToSummand(indices.get(tempNum), tempNum, list);

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
        List<PomTaggedExpression> components2 = components.get(0).getComponents();
        String storeIndex = "";
        try{
            storeIndex = components2.get(0).getRoot().getTermText();
        } catch(Exception e){
            storeIndex = components.get(0).getRoot().getTermText();
        }
        int size = components.get(0).getComponents().size();
        //this is the index and lower limit of summation
        PomTaggedExpression lowerLim = components.get(0);
        args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
        remove(size);
        size = list.get(0).getComponents().size();
        //this is the function being summed
        if(GlobalConstants.CAS_KEY.equals("Mathematica"))
            args.get(num).add(0, parseGeneralExpression(list.remove(0), list).toString());
        else
            args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
        remove(size);
        indices.add(storeIndex);
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
        if(list.get(0).getComponents().size() == 0){
            for(int i = 0; i < list.size(); i++){
                char nextChar = list.get(i).getRoot().getTermText().charAt(0);
                if(Character.isLetter(nextChar)){
                    storeIndex = Character.toString(nextChar);
                    break;
                }
            }
        } else {
            for (int i = 0; i < list.get(0).getComponents().size(); i++) {
                char nextChar = listComponents.get(i).getRoot().getTermText().charAt(0);
                if (Character.isLetter(nextChar)) {
                    storeIndex = Character.toString(nextChar);
                    break;
                }
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
        indices.add(storeIndex);
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
        if(!(components.size() == 0)){
            for (int i = 0; i < components.size(); i++) {
                char nextChar = components.get(i).getRoot().getTermText().charAt(0);
                if (Character.isLetter(nextChar)) {
                    storeIndex = Character.toString(nextChar);
                    break;
                }
            }
        } if(storeIndex.isEmpty() && Character.isLetter(next.getRoot().getTermText().charAt(0))){
            storeIndex = Character.toString(next.getRoot().getTermText().charAt(0));
        } if(storeIndex.isEmpty()) {
            for(int i = 0; i < list.size(); i++){
                char nextChar = list.get(i).getRoot().getTermText().charAt(0);
                if(Character.isLetter(nextChar)){
                    storeIndex = Character.toString(nextChar);
                    break;
                }
            }
        }
        int size = components.size();
        //summand
        args.get(num).add(parseGeneralExpression(next, list).toString());
        remove(size);

     //   if(storeIndex.isEmpty() && Character.isLetter(next.getRoot().getTermText().charAt(0)))
   //         storeIndex = next.getRoot().getTermText();
        //add index of summation
        if(!storeIndex.isEmpty())
            args.get(tempNum).add(storeIndex);
        else
            args.get(tempNum).add("i");
        if(GlobalConstants.CAS_KEY.equals("Maple"))
            args.get(tempNum).add(args.get(tempNum).remove(0));
        indices.add(storeIndex);
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

        if(!Character.isLetter(storeIndex.charAt(0))) {
            for(int i = 0; i < list.size(); i++){
                char nextChar = list.get(i).getRoot().getTermText().charAt(0);
                if(Character.isLetter(nextChar)){
                    storeIndex = Character.toString(nextChar);
                    break;
                }
            }
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
        indices.add(storeIndex);
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

    /**
     * If terms are being multiplied to the original summand, add them to the summand.
     *
     * If there is an addition, subtraction, or relational operator, that is the end of the summand.
     * Otherwise, the term is part of the summand.
     * @param list
     * @param tempNum
     * @param numArgs
     */
    protected void addFactorsToSummand(List<PomTaggedExpression> list, int tempNum, int numArgs){
        //determine where to add the new summand to
        //Mathematica is 2, and Maple is 1, except for the case with 3 args, then Maple is 2 also.
        int numFromEnd;
        if(GlobalConstants.CAS_KEY.equals("Mathematica") || numArgs == 3)
            numFromEnd = 2;
        else
            numFromEnd = 1;

        List<String> sum = new ArrayList<>();
        //this is the current summand
        String summand = args.get(tempNum).get(args.get(tempNum).size() - numFromEnd);
        sum.add(summand);
        String lastExp = summand;
        boolean endSummand = false;
        //for each term in the expressions list following the summand, if its tag is something other than
        //addition, subtraction, equals, etc. then add it to the summand.
        for(int i = 0; i < list.size(); i++){
            if(endSummand)
                break;

            MathTermTags tag = MathTermTags.getTagByKey(list.get(i).getRoot().getTag());
            if(tag == null){
                sum.add(parseGeneralExpression(list.remove(i), list).toString());
                lastExp = global_exp.removeLastExpression();
                i--;
            } else {
                switch (tag) {
                    case plus:
                    case minus:
                    case equals:
                    case less_than:
                    case greater_than:
                    case relation:
                    case right_parenthesis:
                        endSummand = true;
                        break;
                    //add the last translated term back into the global_exp
                    //so that MathTermTranslator.parseCaret/Underscores can work
                    case caret:
                        global_exp.addTranslatedExpression("(" + lastExp + ")");
                        sum.add(parseGeneralExpression(list.remove(i), list).toString());
                        lastExp = global_exp.removeLastExpression();
                        i--;
                        break;
                    case underscore:
                        global_exp.addTranslatedExpression(lastExp);
                        parseGeneralExpression(list.remove(i), list);
                        lastExp = global_exp.removeLastExpression();
                        sum.set(sum.size()-1, lastExp);
                        i--;
                        break;
                    default:
                        sum.add(parseGeneralExpression(list.remove(i), list).toString());
                        lastExp = global_exp.removeLastExpression();
                        i--;
                        break;
                }
            }
        }
        String combine = "";
        for(String str : sum){
            combine += str;
        }
        //put the new summand in
        args.get(tempNum).set(args.get(tempNum).size() - numFromEnd, combine);
    }



    /**
     * If the variable that is the index of summation is also somewhere outside of the sum,
     * then it should be included in the summand.
     *
     * @param index
     * @param tempNum
     * @param list
     */
/*    private void addMoreToSummand(String index, int tempNum, List<PomTaggedExpression> list){
        int numToSubtract;
        if(GlobalConstants.CAS_KEY.equals("Maple"))
            numToSubtract = 1;
        else
            numToSubtract = 2;
        //SHOULD BE ARGS.GET(TEMPNUM).GET(NUMTOADD); NEED TO GO INSIDE ARRAYLIST OF ARRAYLIST THATS THE PROBLEM
        int whereToAdd = args.get(tempNum).size()-numToSubtract;
        String newSummand = args.get(tempNum).get(whereToAdd);
        int lastIndexOfIndex = -1;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).getNumberOfNonemptyMathTerms() == 1 && list.get(i).getRoot().getTermText().equals(index)) {
                lastIndexOfIndex = i;
            }
            else{
                for(int k = 0; k < list.get(i).getComponents().size(); k++){
                    if(list.get(i).getComponents().get(k).getNumberOfNonemptyMathTerms() == 1 && list.get(i).getComponents().get(k).getRoot().getTermText().equals(index)){
                        lastIndexOfIndex = i;
                    } else {
                        for(int l = 0; l < list.get(i).getComponents().get(k).getComponents().size(); l++){
                            if(list.get(i).getComponents().get(k).getComponents().get(l).getRoot().getTermText().equals(index)){
                                lastIndexOfIndex = i;
                            }
                        }
                    }
                }
            }
        }
        while(lastIndexOfIndex >= 0 && !list.isEmpty()) {
            if(MathTermTags.getTagByKey(list.get(0).getRoot().getTag()).equals(MathTermTags.dlmf_macro))
                lastIndexOfIndex--;
            newSummand += parseGeneralExpression(list.remove(0), list);
            global_exp.removeLastExpression();
            lastIndexOfIndex--;
        }
        args.get(tempNum).set(whereToAdd, newSummand);
    }

 */
}