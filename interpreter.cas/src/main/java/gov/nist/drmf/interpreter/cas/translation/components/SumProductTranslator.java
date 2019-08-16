package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
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

    private String index;

    private static int num = -1;

    @Override
    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        num++;
        //need to store tempNum in case there is a nested sum/product
        int tempNum = num;
        //create a new arraylist to store the arguments to this sum/product
        args.add(new ArrayList<String>());
        PomTaggedExpression next = list.remove(0);

        //put the arguments to the sum in the list of sum args
        int numArgs = addToArgs(next, list);

        //put the args from the arraylist into an array so that it can be passed as an argument to parseBasicFunction
        String[] argsArray = new String[args.get(tempNum).size()];
        for(int i = 0; i < args.get(tempNum).size(); i++){
            argsArray[i] = args.get(tempNum).get(i);
        }
        String name = exp.getRoot().getTermText().substring(1) + numArgs;
        BasicFunctionsTranslator bft = SemanticLatexTranslator.getBasicFunctionParser();
        String expr = bft.translate(argsArray, name);
        local_inner_exp.addTranslatedExpression(expr);
        global_exp.addTranslatedExpression(expr);
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
    private int onlyLower(PomTaggedExpression next, List<PomTaggedExpression> list, int tempNum){
        List<PomTaggedExpression> components = next.getComponents();
        List<PomTaggedExpression> components2 = components.get(0).getComponents();

        index = searchForIndex(components);
        int size = components2.size();

        //this is the index and lower limit of summation
        PomTaggedExpression lowerLim = components.get(0);
        int size2 = lowerLim.getComponents().size();
        if(lowerLim.getComponents().size() > 1 && GlobalConstants.CAS_KEY.equals("Mathematica")){
            removeIndex(lowerLim, size2);
        } else {
            args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
            remove(size);
        }
        size = list.get(0).getComponents().size();
        //this is the function being summed
        if(GlobalConstants.CAS_KEY.equals("Mathematica"))
            args.get(num).add(0, parseGeneralExpression(list.remove(0), list).toString());
        else
            args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
        remove(size);

        args.get(tempNum).add(index);
        //add factors to summand
        addFactorsToSummand(list, tempNum, 2, null);

        return 2;
    }

    /**
     * This method is called when the sum/prod has no lower or upper limit defined.
     * Ex: \sum{x+5}
     * @param next
     * @param list
     * @param tempNum
     * @return
     */

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
    private int lowerAndUpper(PomTaggedExpression next, List<PomTaggedExpression> list, int tempNum){
        List<PomTaggedExpression> components = next.getComponents();
        PomTaggedExpression[] lims = findLims(components);
        PomTaggedExpression lowerLim = lims[0];
        PomTaggedExpression upperLim = lims[1];
        PomTaggedExpression index2 = lims[2];
        String storeIndex = index2.getRoot().getTermText();

        //if theres no index, find the first letter and assume it is the index
        boolean needNewIndex = !Character.isLetter(storeIndex.charAt(0)) && !MathTermTags.getTagByKey(index2.getRoot().getTag()).equals(MathTermTags.special_math_letter);
        if(needNewIndex) {
            storeIndex = searchForIndex(list);
        }
        //if theres still no index, put in i
        if(needNewIndex){
            storeIndex = "i";
        }
        index = storeIndex;

        //check if there is a nested sum defined with a comma
        String innerIndex = isAnotherSum(lowerLim);
        boolean isInnerSum = !"".equals(innerIndex);

        //lower limit of summation
        int size = lowerLim.getComponents().size();
        if(lowerLim.getComponents().size() > 1 && GlobalConstants.CAS_KEY.equals("Mathematica")){
            removeIndex(lowerLim, size);
        } else{
            if(isInnerSum) {
                String operator = removeIndex(lowerLim, size);
                String currentLim = args.get(tempNum).get(0);
                args.get(tempNum).set(0, innerIndex + operator + currentLim);
            } else {
                args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
                global_exp.removeLastNExps(size);
            }
        }

        //upper limit of summation
        size = upperLim.getComponents().size();
        args.get(num).add(parseGeneralExpression(upperLim, list).toString());
        remove(size);

        //summand
        size = list.get(0).getComponents().size();
        args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
        remove(size);

        //add index of summation
        if(MathTermTags.getTagByKey(index2.getRoot().getTag()).equals(MathTermTags.special_math_letter)){
            String translatedIndex = parseGeneralExpression(index2, null).toString();
            global_exp.removeLastExpression();
            args.get(tempNum).add(translatedIndex);
        } else {
            args.get(tempNum).add(storeIndex);
        }

        //add factors to summand
        if(isInnerSum)
            addFactorsToSummand(list, tempNum, 3, innerIndex);
        else
            addFactorsToSummand(list, tempNum, 3, null);

        //if theres a nested sum defined with a comma, ex: \sum_{n, k \hiderel{=} 0}^{\infty}\frac{x^n}{n!}y^k
        //translate that here
        if(isInnerSum) {
            addNextSum(innerIndex, tempNum);
        }
        return  3;
    }

    /**
     * Finds the lower limit, uppper limit, and index of summation.
     *
     * @param components
     * @return A list of these things
     */
    private PomTaggedExpression[] findLims(List<PomTaggedExpression> components){
        PomTaggedExpression[] lims = new PomTaggedExpression[3];
        PomTaggedExpression lowerLim;
        PomTaggedExpression upperLim;
        PomTaggedExpression index2;
        //lower limit defined before upper limit
        if(MathTermTags.getTagByKey(components.get(0).getRoot().getTag()).equals(MathTermTags.underscore)){
            List<PomTaggedExpression> components2 = components.get(0).getComponents();
            //Store the index of summation.
            try{
                List<PomTaggedExpression> components3 = components2.get(0).getComponents();
                index2 = components3.get(0);
            } catch(IndexOutOfBoundsException e){
                index2 = components2.get(0);
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
                index2 = components3.get(0);
            } catch(IndexOutOfBoundsException e){
                index2 = components2.get(0);
            }
            lowerLim = components2.get(0);
            List<PomTaggedExpression> upperComponents = components.get(0).getComponents();
            upperLim = upperComponents.get(0);
        }
        lims[0] = lowerLim;
        lims[1] = upperLim;
        lims[2] = index2;
        return lims;
    }

    /**
     * Uses the current args as args to the nested sum.
     * Translates the sum using BasicFunctionParser.
     * Uses the nested sum as the argument to this sum.
     * Plugs in the correct indexes of summation.
     *
     * @param nextIndex
     * @param tempNum
     */
    private void addNextSum(String nextIndex, int tempNum){
        args.get(tempNum).set(args.get(tempNum).size()-1, nextIndex);
        String[] argsarray = new String[args.get(tempNum).size()];
        for(int i = 0; i < args.get(tempNum).size(); i++){
            argsarray[i] = args.get(tempNum).get(i);
        }
        //translate the inner sum
        BasicFunctionsTranslator bft = SemanticLatexTranslator.getBasicFunctionParser();
        String nextSum = bft.translate(argsarray, "sum3");

        //put in the sum as the summand to this sum
        args.get(tempNum).set(args.get(tempNum).size()-2, nextSum);

        //put the old index back in
        args.get(tempNum).set(args.get(tempNum).size()-1, index);
        if(GlobalConstants.CAS_KEY.equals("Maple")){
            String restOfLim = args.get(tempNum).get(0).substring(args.get(tempNum).get(0).indexOf(nextIndex)+1);
            args.get(tempNum).set(0, index + restOfLim);
        }
    }

    /**
     * Removes the index of summation from the lower limit.
     *
     * @param lowerLim
     * @param size
     * @return the relational operator, which Maple needs for nested sums defined with a comma.
     */
    private String removeIndex(PomTaggedExpression lowerLim, int size){
        String newLowerLim = "";
        String lastTemp = "";
        String relationalOperator = "";
        boolean add = false;
        int index = -1;
        for(int i = 0; i < lowerLim.getComponents().size(); i++){
            MathTermTags tag = MathTermTags.getTagByKey(lowerLim.getComponents().get(i).getRoot().getTag());
            if(add){
                String temp = parseGeneralExpression(lowerLim.getComponents().remove(i), lowerLim.getComponents()).toString();
                if(temp.isEmpty()) {
                    newLowerLim = newLowerLim.substring(0, newLowerLim.indexOf(lastTemp));
                    newLowerLim += global_exp.removeLastExpression();
                } else{
                    newLowerLim += temp;
                    lastTemp = temp;
                }
                i--;
            } else if(tag.equals(MathTermTags.relation) || tag.equals(MathTermTags.equals) || tag.equals(MathTermTags.greater_than) || tag.equals(MathTermTags.less_than)){
                index = i;
                add = true;
                relationalOperator = lowerLim.getComponents().get(i).getRoot().getTermText();
            }
        }
        args.get(num).add(newLowerLim);
        remove(size-index);
        return relationalOperator;
    }

    /**
     * If there is a comma followed by a letter inside the lower limit of summation, then this is a nested sum.
     * That letter is the index of summation for the nested sum, return it.
     *
     * @param remainingExpression
     * @return the letter
     */
    private String isAnotherSum(PomTaggedExpression remainingExpression){
        List<PomTaggedExpression> components = remainingExpression.getComponents();
        int size = components.size();
        if(components.size() == 0)
            return "";
        else {
            for(int i = 0; i < size; i++){
                if(MathTermTags.getTagByKey(components.get(i).getRoot().getTag()).equals(MathTermTags.comma) && i + 1 < size){
                    PomTaggedExpression next = components.get(i+1);
                    MathTermTags tag = MathTermTags.getTagByKey(next.getRoot().getTag());
                    if(tag.equals(MathTermTags.alphanumeric) || tag.equals(MathTermTags.letter) || tag.equals(MathTermTags.special_math_letter)){
                        String index = parseGeneralExpression(next, null).toString();
                        global_exp.removeLastExpression();
                        return index;
                    }
                }

            }
            return "";
        }
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
    private int addToArgs(PomTaggedExpression next, List<PomTaggedExpression> list){
        ExpressionTags tag = ExpressionTags.getTagByKey(next.getTag());
        //only upper limit, only lower limit, or no limits with a summand of length 1.
        if(tag == null && MathTermTags.getTagByKey(next.getRoot().getTag()).equals(MathTermTags.underscore)){
            return onlyLower(next, list, num);
        } else if(tag != null && tag.equals(ExpressionTags.sub_super_script)) {
            return lowerAndUpper(next, list, num);
        }
        throw new TranslationException("This sum format is not allowed.");
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
    private void addFactorsToSummand(List<PomTaggedExpression> list, int tempNum, int numArgs, String nextIndex){
        //determine where to add the new summand to
        //Mathematica is 2, and Maple is 1, except for the case with 3 args, then Maple is 2 also.
        int numFromEnd;
        if(GlobalConstants.CAS_KEY.equals("Mathematica") && numArgs == 3)
            numFromEnd = 2;
        else if (GlobalConstants.CAS_KEY.equals("Mathematica"))
            numFromEnd = 3;
        else
            numFromEnd = 2;

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
            //if there is a sum with the same index of summation, stop.
            if(list.get(i).getComponents().size() != 0 && isIndexPresent(list.get(i).getComponents(), tempNum, index, nextIndex) == -2) {
                break;
            } else if(tag == null){
                sum.add(parseGeneralExpression(list.remove(i), list).toString());
                lastExp = global_exp.removeLastExpression();
                i--;
            } else{
                switch (tag) {
                    //if + or -, see if the index is present after.
                    //if it is, continue adding to summand
                    //if its not, stop
                    case plus:
                    case minus:
                        if(isIndexPresent(list, tempNum, index, nextIndex) == 1){
                            sum.add(parseGeneralExpression(list.remove(i), list).toString());
                            lastExp = global_exp.removeLastExpression();
                            i--;
                        } else
                            endSummand = true;
                        break;
                    //if one of these encountered, stop
                    case equals:
                    case less_than:
                    case greater_than:
                    case relation:
                    case right_parenthesis:
                    case right_delimiter:
                        endSummand = true;
                        break;
                    //cases that use the last term
                    //add the last translated term back into the global_exp so that MathTermTranslator can work
                    case factorial:
                    case caret:
                    case underscore:
                        global_exp.addTranslatedExpression(lastExp);
                        parseGeneralExpression(list.remove(i), list);
                        lastExp = global_exp.removeLastExpression();
                        sum.set(sum.size()-1, lastExp);
                        i--;
                        break;
                    //if the sum/prod has the same index of summation, stop
                    case operator:
                        try {
                            List<PomTaggedExpression> components = list.get(i + 1).getComponents();
                            List<PomTaggedExpression> components2 = components.get(0).getComponents();
                            List<PomTaggedExpression> components3 = components2.get(0).getComponents();
                            String text = components3.get(0).getRoot().getTermText();
                            if (text.equals(index)) {
                                endSummand = true;
                                break;
                            }
                        } catch(IndexOutOfBoundsException e){
                        }
                        //no break here
                    default:
                        sum.add(parseGeneralExpression(list.remove(i), list).toString());
                        lastExp = global_exp.removeLastExpression();
                        i--;
                        break;
                }
            }
        }
        //turn list into string
        String combine = "";
        for(String str : sum){
            combine += str;
        }
        //put the new summand in
        args.get(tempNum).set(args.get(tempNum).size() - numFromEnd, combine);
    }

    /**
     * Recursively searches for the index variable in the parse tree.
     * If the index is present anywhere in the list, returns 1.
     * If the index is not present, returns 0.
     * If there is a stopping point where summand is sure to stop, stops searching and returns -1.
     * If there is a sum with the same index of summation, returns -2.
     *
     * @param list, the list of following expressions after the sum.
     * @param tempNum, the current number of sums, used to access the right index.
     * @return
     */
    private int isIndexPresent(List<PomTaggedExpression> list, int tempNum, String index, String nextIndex){
        int numParen = 0;
        MathTermTags lastTag = null;
        for(PomTaggedExpression ex : list){
            String text = ex.getRoot().getTermText();
            MathTermTags tag = MathTermTags.getTagByKey(ex.getRoot().getTag());
            //if the index is found, return true
            if(text.equals(index)){
                return 1;
            }
            //If theres a sum with the same index as this sum's index of summation, stop searching and return -1.
            boolean conds = lastTag != null && ex.getComponents().size() != 0;
            if(conds && lastTag.equals(MathTermTags.operator) && isIndexPresent(ex.getComponents().get(0).getComponents(), tempNum, index, null) == 1)
                return -2;

            if(ex.getComponents().size() != 0) {
                int val = isIndexPresent(ex.getComponents(), tempNum, index, null);
                if (val != 0)
                    return val;
            }

            //if the tag is null, don't do anything
            if(tag != null){
                //stop if there are any of these things
                switch (tag) {
                    case equals:
                    case less_than:
                    case greater_than:
                    case relation:
                        if(ex.getParent() == null)
                            return -1;
                        break;
                    //count the number of parenthesis
                    //if there are more right parens than left parens, stop.
                    case left_parenthesis:
                        numParen++;
                        break;
                    case right_parenthesis:
                        numParen--;
                        if (numParen < 0)
                            return -1;
                        break;
                }
            }
            lastTag = tag;
        }

        //If its a double sum, account for the inner sum's index too
        if(nextIndex != null){
            return isIndexPresent(list, tempNum, nextIndex, null);
        }
        return 0;
    }

    /**
     * Recursively searches for the first thing that is a letter. That is assumed to be the index.
     * @param list, the list of expressions to be searched
     * @return
     */
    private String searchForIndex(List<PomTaggedExpression> list){
        for(PomTaggedExpression ex : list) {
            if (!ex.getRoot().getTermText().isEmpty()) {
                char nextChar = ex.getRoot().getTermText().charAt(0);
                if (Character.isLetter(nextChar))
                    return Character.toString(nextChar);
            }
            if (ex.getComponents().size() != 0 && !searchForIndex(ex.getComponents()).isEmpty()) {
                return searchForIndex(ex.getComponents());
            }
        }
        return "";
    }
}