package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.cas.blueprints.Limits;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * SumProductTranslator uses parseGeneralExpression to get the arguments to the sum/product.
 * Then it uses BasicFunctionParser to put the arguments where they need to go.
 *
 * Method call order: translate -> addToArgs -> onlyLower/lowerAndUpper -> addFactorsToSummand
 *
 * @author Andre Greiner-Petter
 * @author Rajen Dey
 *
 * July 2019
 */
public class SumProductTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(SumProductTranslator.class.getName());

    private static ArrayList<ArrayList<String>> args = new ArrayList<>();

    private String index;

    private static int num = -1;

    // perform translation and put everything into global_exp
    private BasicFunctionsTranslator bft;

    private TranslatedExpression localTranslations;

    public SumProductTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.bft = super.getConfig().getBasicFunctionsTranslator();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    //    @Override
//    public boolean translateOLD(PomTaggedExpression exp, List<PomTaggedExpression> list){
//        num++;
//        //need to store tempNum in case there is a nested sum/product
//        int tempNum = num;
//        //create a new arraylist to store the arguments to this sum/product
//        args.add(new ArrayList<String>());
//        PomTaggedExpression next = list.remove(0);
//
//        //put the arguments to the sum in the list of sum args
//        int numArgs = addToArgs(next, list);
//
//        //put the args from the arraylist into an array so that it can be passed as an argument to parseBasicFunction
//        String[] argsArray = new String[args.get(tempNum).size()];
//        for(int i = 0; i < args.get(tempNum).size(); i++){
//            argsArray[i] = args.get(tempNum).get(i);
//        }
//        String name = exp.getRoot().getTermText().substring(1) + numArgs;
//        BasicFunctionsTranslator bft = SemanticLatexTranslator.getBasicFunctionParser();
//        String expr = bft.translate(argsArray, name);
//        local_inner_exp.addTranslatedExpression(expr);
//        global_exp.addTranslatedExpression(expr);
//        return true;
//    }
//
//    /**
//     * Method to remove expressions that are already being used as an argument to the sum/prod from global_exp
//     * so that they don't get reused.
//     *
//     * When size is 0, it is actually 1.
//     * @param size
//     */
//    private void remove(int size){
//        if(size != 0)
//            global_exp.removeLastNExps(size);
//        else
//            global_exp.removeLastNExps(1);
//    }
//
//    /**
//     * This method is called when the sum/prod has only a lower limit defined.
//     *
//     * Translates the index, lower limit, summand, and adds them into the list of arguments.
//     * Ex: \sum_{x \in X}x+5
//     * @param next
//     * @param list
//     * @return
//     */
//    private int onlyLower(PomTaggedExpression next, List<PomTaggedExpression> list, int tempNum){
//        List<PomTaggedExpression> components = next.getComponents();
//        List<PomTaggedExpression> components2 = components.get(0).getComponents();
//        PomTaggedExpression lowerLim = components.get(0);
//        int size = components2.size();
//
//        //find index
//        index = searchForIndex(components);
//
//        //case with sum with both lower and upper limits in the underscore.
//        String[] lims = doubleLTs(lowerLim);
//        //if this boolean is true, then this case is true
//        boolean goToThree = !lims[2].isEmpty();
//        int addPlace = 0;
//        boolean isMaple = GlobalConstants.CAS_KEY.equals("Maple");
//        //do this if sum has 2 LTs
//        if(goToThree) {
//            //put in all the extracted lims and put them in their places in args
//            index = lims[1];
//            if(isMaple){
//                args.get(num).add(lims[1]+ "=" + lims[0]);
//                args.get(num).add(lims[2]);
//            } else {
//                args.get(num).add(lims[0]);
//                args.get(num).add(lims[2]);
//                args.get(num).add(lims[1]);
//            }
//            addPlace = 2;
//
//            //otherwise add lower limit to args normally
//        } else {
//            //this is the index and lower limit of summation
//            int size2 = lowerLim.getComponents().size();
//            if (lowerLim.getComponents().size() > 1 && !isMaple) {
//                removeIndex(lowerLim, size2);
//            } else {
//                args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
//                remove(size);
//            }
//        }
//        size = list.get(0).getComponents().size();
//        //this is the function being summed
//        if(!isMaple)
//            args.get(num).add(addPlace, parseGeneralExpression(list.remove(0), list).toString());
//        else
//            args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
//        remove(size);
//
//        if(addPlace == 0)
//            args.get(tempNum).add(index);
//
//        //add factors to summand
//        int num = 3;
//        if(isMaple)
//            //need this for addFactorsToSummand to work as intended
//            num = -1;
//
//        if(goToThree) {
//            addFactorsToSummand(list, tempNum, num, null);
//            //now this sum/prod as all both a lower and upper limit, so need to return 3.
//            return 3;
//        } else {
//            addFactorsToSummand(list, tempNum, 2, null);
//            //this sum/prod still has just a lower limit, so return 2.
//            return 2;
//        }
//
//    }
//
//    /**
//     * For sums with two less thans ex: \sum_{0<i<10}i
//     * Extracts the lower limit, index, and upper limit and returns them in an array.
//     *
//     * @param lowerLim
//     * @return an array containing the lower limit, index, and upper limit
//     */
//    private String[] doubleLTs(PomTaggedExpression lowerLim){
//        String[] lims = new String[3];
//        String beforeLTs = "";
//        String betweenLTs = "";
//        String afterLTs = "";
//        List<PomTaggedExpression> components = lowerLim.getComponents();
//        int numRels = 0;
//        int numLTs = 0;
//
//        for(int i = 0; i < lowerLim.getComponents().size(); i++){
//            MathTermTags tag = MathTermTags.getTagByKey(components.get(i).getRoot().getTag());
//            if(tag.equals(MathTermTags.relation)) {
//                numRels++;
//                continue;
//            }
//            if(tag.equals(MathTermTags.less_than)) {
//                numRels++;
//                numLTs ++;
//                continue;
//            }
//            switch(numRels) {
//                //before the first lt
//                case 0:
//                    beforeLTs += parseGeneralExpression(components.get(i), null);
//                    global_exp.removeLastExpression();
//                    break;
//                //between the 2 lts
//                case 1:
//                    betweenLTs += parseGeneralExpression(components.get(i), null);
//                    global_exp.removeLastExpression();
//                    break;
//                //after both lts
//                case 2:
//                    afterLTs += parseGeneralExpression(components.get(i), null);
//                    global_exp.removeLastExpression();
//                    break;
//            }
//
//        }
//        //add +1s or -1s if there were "less thans" instead of "less than or equals tos"
//        if(numLTs == 1 && numRels == 1)
//            beforeLTs += "+1";
//        else if(numLTs == 1 && numRels == 2)
//            afterLTs += "-1";
//        else if (numLTs == 2) {
//            beforeLTs += "+1";
//            afterLTs += "-1";
//        }
//
//        lims[0] = beforeLTs;
//        lims[1] = betweenLTs;
//        lims[2] = afterLTs;
//
//        return lims;
//    }
//
//    /**
//     * This method is called when the sum/prod has both an upper and a lower limit.
//     *
//     * Translates the index, lower limit, upper limit, summand, and adds them into the arguments list.
//     *
//     * Ex: \sum_{x=0}^{10}x+5
//     * Ex: \sum^{10}_{x=0}x+5
//     *
//     * @param next
//     * @param list
//     * @param tempNum
//     * @return
//     */
//    private int lowerAndUpper(PomTaggedExpression next, List<PomTaggedExpression> list, int tempNum){
//        List<PomTaggedExpression> components = next.getComponents();
//        PomTaggedExpression[] lims = findLims(components);
//        PomTaggedExpression lowerLim = lims[0];
//        PomTaggedExpression upperLim = lims[1];
//        PomTaggedExpression index2 = lims[2];
//        String storeIndex = index2.getRoot().getTermText();
//
//        boolean wasIndex = true;
//        //if theres no index, find the first letter and assume it is the index
//        boolean needNewIndex = !Character.isLetter(storeIndex.charAt(0)) && !MathTermTags.getTagByKey(index2.getRoot().getTag()).equals(MathTermTags.special_math_letter);
//        if(needNewIndex) {
//            storeIndex = searchForIndex(list);
//            //if the index wasn't defined, make this false
//            wasIndex = false;
//        }
//        needNewIndex = !Character.isLetter(storeIndex.charAt(0)) && !MathTermTags.getTagByKey(index2.getRoot().getTag()).equals(MathTermTags.special_math_letter);
//        //if theres still no index, put in i
//        if(needNewIndex){
//            storeIndex = "i";
//        }
//        index = storeIndex;
//
//        //check if there is a nested sum defined with a comma
//        String innerIndex = isAnotherSum(lowerLim);
//        boolean isInnerSum = !"".equals(innerIndex);
//
//        //lower limit of summation
//        int size = lowerLim.getComponents().size();
//        if(lowerLim.getComponents().size() > 1 && GlobalConstants.CAS_KEY.equals("Mathematica") && wasIndex) {
//            removeIndex(lowerLim, size);
//        } else{
//            if(isInnerSum) {
//                String operator = removeIndex(lowerLim, size);
//                String currentLim = args.get(tempNum).get(0);
//                args.get(tempNum).set(0, innerIndex + operator + currentLim);
//                //translate lower limit normally
//            } else if(wasIndex || GlobalConstants.CAS_KEY.equals("Mathematica")){
//                args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
//                global_exp.removeLastNExps(size);
//                //there was no index, so add the index in.
//            } else{
//                args.get(num).add(index + "=" + parseGeneralExpression(lowerLim, list).toString());
//                global_exp.removeLastNExps(size);
//            }
//        }
//
//        //upper limit of summation
//        size = upperLim.getComponents().size();
//        args.get(num).add(parseGeneralExpression(upperLim, list).toString());
//        remove(size);
//
//        //summand
//        size = list.get(0).getComponents().size();
//        args.get(num).add(parseGeneralExpression(list.remove(0), list).toString());
//        remove(size);
//
//        //add index of summation
//        if(MathTermTags.getTagByKey(index2.getRoot().getTag()).equals(MathTermTags.special_math_letter)){
//            String translatedIndex = parseGeneralExpression(index2, null).toString();
//            global_exp.removeLastExpression();
//            args.get(tempNum).add(translatedIndex);
//        } else {
//            args.get(tempNum).add(storeIndex);
//        }
//
//        //add factors to summand
//        if(isInnerSum)
//            addFactorsToSummand(list, tempNum, 3, innerIndex);
//        else
//            addFactorsToSummand(list, tempNum, 3, null);
//
//        //if theres a nested sum defined with a comma, ex: \sum_{n, k \hiderel{=} 0}^{\infty}\frac{x^n}{n!}y^k
//        //translate that here
//        if(isInnerSum) {
//            addNextSum(innerIndex, tempNum);
//        }
//        return  3;
//    }
//
//    /**
//     * Finds the lower limit, uppper limit, and index of summation.
//     * Determines which limit (lower/upper) is defined first (^ or _) to extract the correct limits.
//     *
//     * Used in lowerAndUpper
//     *
//     * @param components
//     * @return A list of these things
//     */
//    private PomTaggedExpression[] findLims(List<PomTaggedExpression> components){
//        PomTaggedExpression[] lims = new PomTaggedExpression[3];
//        PomTaggedExpression lowerLim;
//        PomTaggedExpression upperLim;
//        PomTaggedExpression index2;
//        //lower limit defined before upper limit
//        if(MathTermTags.getTagByKey(components.get(0).getRoot().getTag()).equals(MathTermTags.underscore)){
//            List<PomTaggedExpression> components2 = components.get(0).getComponents();
//            //Store the index of summation.
//            try{
//                List<PomTaggedExpression> components3 = components2.get(0).getComponents();
//                index2 = components3.get(0);
//            } catch(IndexOutOfBoundsException e){
//                index2 = components2.get(0);
//            }
//            lowerLim = components2.get(0);
//            List<PomTaggedExpression> upperComponents = components.get(1).getComponents();
//            upperLim = upperComponents.get(0);
//        } //upper limit defined before lower limit
//        else {
//            List<PomTaggedExpression> components2 = components.get(1).getComponents();
//            //Store the index of summation.
//            try {
//                List<PomTaggedExpression> components3 = components2.get(0).getComponents();
//                index2 = components3.get(0);
//            } catch(IndexOutOfBoundsException e){
//                index2 = components2.get(0);
//            }
//            lowerLim = components2.get(0);
//            List<PomTaggedExpression> upperComponents = components.get(0).getComponents();
//            upperLim = upperComponents.get(0);
//        }
//        lims[0] = lowerLim;
//        lims[1] = upperLim;
//        lims[2] = index2;
//        return lims;
//    }
//
//    /**
//     * Uses the current args as args to the nested sum.
//     * Translates the sum using BasicFunctionParser.
//     * Uses the nested sum as the argument to this sum.
//     * Plugs in the correct indexes of summation.
//     *
//     * @param nextIndex
//     * @param tempNum
//     */
//    private void addNextSum(String nextIndex, int tempNum){
//        args.get(tempNum).set(args.get(tempNum).size()-1, nextIndex);
//        String[] argsarray = new String[args.get(tempNum).size()];
//        for(int i = 0; i < args.get(tempNum).size(); i++){
//            argsarray[i] = args.get(tempNum).get(i);
//        }
//        //translate the inner sum
//        BasicFunctionsTranslator bft = SemanticLatexTranslator.getBasicFunctionParser();
//        String nextSum = bft.translate(argsarray, "sum3");
//
//        //put in the sum as the summand to this sum
//        args.get(tempNum).set(args.get(tempNum).size()-2, nextSum);
//
//        //put the old index back in
//        args.get(tempNum).set(args.get(tempNum).size()-1, index);
//        if(GlobalConstants.CAS_KEY.equals("Maple")){
//            String restOfLim = args.get(tempNum).get(0).substring(args.get(tempNum).get(0).indexOf(nextIndex)+1);
//            args.get(tempNum).set(0, index + restOfLim);
//        }
//    }
//
//    /**
//     * Removes the index of summation from the lower limit.
//     *
//     * @param lowerLim
//     * @param size
//     * @return the relational operator, which Maple needs for nested sums defined with a comma.
//     */
//    private String removeIndex(PomTaggedExpression lowerLim, int size){
//        String newLowerLim = "";
//        String lastTemp = "";
//        String relationalOperator = "";
//        boolean add = false;
//        int index = -1;
//        for(int i = 0; i < lowerLim.getComponents().size(); i++){
//            MathTermTags tag = MathTermTags.getTagByKey(lowerLim.getComponents().get(i).getRoot().getTag());
//            if(add){
//                String temp = parseGeneralExpression(lowerLim.getComponents().remove(i), lowerLim.getComponents()).toString();
//                if(temp.isEmpty()) {
//                    newLowerLim = newLowerLim.substring(0, newLowerLim.indexOf(lastTemp));
//                    newLowerLim += global_exp.removeLastExpression();
//                } else{
//                    newLowerLim += temp;
//                    lastTemp = temp;
//                }
//                i--;
//            } else if(tag.equals(MathTermTags.relation) || tag.equals(MathTermTags.equals) || tag.equals(MathTermTags.greater_than) || tag.equals(MathTermTags.less_than)){
//                index = i;
//                add = true;
//                relationalOperator = lowerLim.getComponents().get(i).getRoot().getTermText();
//            }
//        }
//        args.get(num).add(newLowerLim);
//        remove(size-index);
//        return relationalOperator;
//    }
//
//    /**
//     * If there is a comma followed by a letter inside the lower limit of summation, then this is a nested sum.
//     * That letter is the index of summation for the nested sum, return it.
//     *
//     * @param remainingExpression
//     * @return the inner index
//     */
//    private String isAnotherSum(PomTaggedExpression remainingExpression){
//        List<PomTaggedExpression> components = remainingExpression.getComponents();
//        int size = components.size();
//        if(components.size() == 0)
//            return "";
//        else {
//            for(int i = 0; i < size; i++){
//                if(MathTermTags.getTagByKey(components.get(i).getRoot().getTag()).equals(MathTermTags.comma) && i + 1 < size){
//                    PomTaggedExpression next = components.get(i+1);
//                    MathTermTags tag = MathTermTags.getTagByKey(next.getRoot().getTag());
//                    if(tag.equals(MathTermTags.alphanumeric) || tag.equals(MathTermTags.letter) || tag.equals(MathTermTags.special_math_letter)){
//                        String index = parseGeneralExpression(next, null).toString();
//                        global_exp.removeLastExpression();
//                        return index;
//                    }
//                }
//
//            }
//            return "";
//        }
//    }
//
//    /**
//     * Depending on what the tag is, this method calls different methods to add the right arguments to the arguments list.
//     * If the tag is null, the sum has either only a lower limit or only an upper limit.
//     * If the tag is sub_super_script, it has both.
//     * If the tag is sequence, it has none.
//     * Then some subcases are used to determine which exact case it is.
//     *
//     * @param next
//     * @param list
//     * @return
//     */
//    private int addToArgs(PomTaggedExpression next, List<PomTaggedExpression> list){
//        ExpressionTags tag = ExpressionTags.getTagByKey(next.getTag());
//        //only upper limit, only lower limit, or no limits with a summand of length 1.
//        if(tag == null && MathTermTags.getTagByKey(next.getRoot().getTag()).equals(MathTermTags.underscore)){
//            return onlyLower(next, list, num);
//        } else if(tag != null && tag.equals(ExpressionTags.sub_super_script)) {
//            return lowerAndUpper(next, list, num);
//        }
//        throw new TranslationException("This sum format is not allowed.");
//    }
//
//    protected String getTranslation(){
//        String exp = global_exp.toString();
//        global_exp.clear();
//        return exp;
//    }
//
//    /**
//     * If terms are being multiplied to the original summand, add them to the summand. (default case)
//     * Add carets, factorials, and underscores to the summand.
//     * If there is a relational operator or a right delimiter without a corresponding left delimiter, that is the end of the summand.
//     * If there is a sum/prod with the same index of summation/multiplication, stop.
//     * If there is a + or -, if the index is still present in the list of following terms, continue adding to summand.
//     * If the index is not present, stop.
//     *
//     * Otherwise, the term is part of the summand.
//     * @param list
//     * @param tempNum
//     * @param numArgs
//     */
//    private void addFactorsToSummand(List<PomTaggedExpression> list, int tempNum, int numArgs, String nextIndex){
//        //determine where to add the new summand to
//        //Mathematica is 2, and Maple is 1, except for the case with 3 args, then Maple is 2 also.
//        int numFromEnd;
//        if(GlobalConstants.CAS_KEY.equals("Mathematica") && numArgs == 3)
//            numFromEnd = 2;
//        else if (GlobalConstants.CAS_KEY.equals("Mathematica"))
//            numFromEnd = 3;
//        else if(numArgs == -1)
//            numFromEnd = 1;
//        else
//            numFromEnd = 2;
//
//        List<String> sum = new ArrayList<>();
//        //this is the current summand
//        String summand = args.get(tempNum).get(args.get(tempNum).size() - numFromEnd);
//        sum.add(summand);
//        String lastExp = summand;
//        boolean endSummand = false;
//        //for each term in the expressions list following the summand, if its tag is something other than
//        //addition, subtraction, equals, etc. then add it to the summand.
//        for(int i = 0; i < list.size(); i++){
//            if(endSummand)
//                break;
//
//            MathTermTags tag = MathTermTags.getTagByKey(list.get(i).getRoot().getTag());
//            //if there is a sum with the same index of summation, stop.
//            if(list.get(i).getComponents().size() != 0 && isIndexPresent(list.get(i).getComponents(), tempNum, index, nextIndex) == -2) {
//                break;
//            } else if(tag == null){
//                sum.add(parseGeneralExpression(list.remove(i), list).toString());
//                lastExp = global_exp.removeLastExpression();
//                i--;
//            } else{
//                switch (tag) {
//                    //if + or -, see if the index is present after.
//                    //if it is, continue adding to summand
//                    //if its not, stop
//                    case plus:
//                    case minus:
//                        if(isIndexPresent(list, tempNum, index, nextIndex) == 1){
//                            sum.add(parseGeneralExpression(list.remove(i), list).toString());
//                            lastExp = global_exp.removeLastExpression();
//                            i--;
//                        } else
//                            endSummand = true;
//                        break;
//                    //if one of these encountered, stop
//                    case equals:
//                    case less_than:
//                    case greater_than:
//                    case relation:
//                    case right_parenthesis:
//                    case right_delimiter:
//                        endSummand = true;
//                        break;
//                    //cases that use the last term
//                    //add the last translated term back into the global_exp so that MathTermTranslator can work
//                    case factorial:
//                    case caret:
//                    case underscore:
//                        global_exp.addTranslatedExpression(lastExp);
//                        parseGeneralExpression(list.remove(i), list);
//                        lastExp = global_exp.removeLastExpression();
//                        sum.set(sum.size()-1, lastExp);
//                        i--;
//                        break;
//                    //if the sum/prod has the same index of summation, stop
//                    case operator:
//                        try {
//                            List<PomTaggedExpression> components = list.get(i + 1).getComponents();
//                            List<PomTaggedExpression> components2 = components.get(0).getComponents();
//                            List<PomTaggedExpression> components3 = components2.get(0).getComponents();
//                            String text = components3.get(0).getRoot().getTermText();
//                            if (text.equals(index)) {
//                                endSummand = true;
//                                break;
//                            }
//                        } catch(IndexOutOfBoundsException e){
//                        }
//                        //no break here
//                    default:
//                        sum.add(parseGeneralExpression(list.remove(i), list).toString());
//                        lastExp = global_exp.removeLastExpression();
//                        i--;
//                        break;
//                }
//            }
//        }
//        //turn list into string
//        String combine = "";
//        for(String str : sum){
//            combine += str;
//        }
//        //put the new summand in
//        args.get(tempNum).set(args.get(tempNum).size() - numFromEnd, combine);
//    }
//
//    /**
//     * Recursively searches for the index variable in the parse tree.
//     * If the index is present anywhere in the list, returns 1.
//     * If the index is not present, returns 0.
//     * If there is a stopping point where summand is sure to stop, stops searching and returns -1.
//     * If there is a sum with the same index of summation, returns -2.
//     *
//     * @param list, the list of following expressions after the sum.
//     * @param tempNum, the current number of sums, used to access the right index.
//     * @return integer representing what is found.
//     */
//    private int isIndexPresent(List<PomTaggedExpression> list, int tempNum, String index, String nextIndex){
//        int numParen = 0;
//        MathTermTags lastTag = null;
//        for(PomTaggedExpression ex : list){
//            String text = ex.getRoot().getTermText();
//            MathTermTags tag = MathTermTags.getTagByKey(ex.getRoot().getTag());
//            //if the index is found, return true
//            if(text.equals(index)){
//                return 1;
//            }
//            //If theres a sum with the same index as this sum's index of summation, stop searching and return -1.
//            boolean conds = lastTag != null && ex.getComponents().size() != 0;
//            if(conds && lastTag.equals(MathTermTags.operator) && isIndexPresent(ex.getComponents().get(0).getComponents(), tempNum, index, null) == 1)
//                return -2;
//
//            if(ex.getComponents().size() != 0) {
//                int val = isIndexPresent(ex.getComponents(), tempNum, index, null);
//                if (val != 0)
//                    return val;
//            }
//
//            //if the tag is null, don't do anything
//            if(tag != null){
//                //stop if there are any of these things
//                switch (tag) {
//                    case equals:
//                    case less_than:
//                    case greater_than:
//                    case relation:
//                        if(ex.getParent() == null)
//                            return -1;
//                        break;
//                    //count the number of parenthesis
//                    //if there are more right parens than left parens, stop.
//                    case left_parenthesis:
//                        numParen++;
//                        break;
//                    case right_parenthesis:
//                        numParen--;
//                        if (numParen < 0)
//                            return -1;
//                        break;
//                }
//            }
//            lastTag = tag;
//        }
//
//        //If its a double sum, account for the inner sum's index too
//        if(nextIndex != null){
//            return isIndexPresent(list, tempNum, nextIndex, null);
//        }
//        return 0;
//    }
//
//    /**
//     * Recursively searches for the first thing that is a letter. That is assumed to be the index.
//     * @param list, the list of expressions to be searched
//     * @return the index
//     */
//    private String searchForIndex(List<PomTaggedExpression> list){
//        for(PomTaggedExpression ex : list) {
//            if (!ex.getRoot().getTermText().isEmpty()) {
//                char nextChar = ex.getRoot().getTermText().charAt(0);
//                if (Character.isLetter(nextChar))
//                    return Character.toString(nextChar);
//            }
//            if (ex.getComponents().size() != 0 && !searchForIndex(ex.getComponents()).isEmpty()) {
//                return searchForIndex(ex.getComponents());
//            }
//        }
//        return "";
//    }

    @Override
    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        // exp is sum/prod/lim

        if (list.isEmpty()) {
            throw new TranslationException("Limited expression in the end are illegal!");
        }

        PomTaggedExpression limitExpression = list.remove(0);
        Limits limit = extractLimits(limitExpression);

        List<PomTaggedExpression> potentialArguments = getPotentialArgumentsUntilEndOfScope(list);
        // the potential arguments is a theoretical sequence, so handle it as a sequence!
        PomTaggedExpression topPTE = new PomTaggedExpression(new MathTerm("",""), "sequence");
        for ( PomTaggedExpression pte : potentialArguments ) topPTE.addComponent(pte);

        SequenceTranslator p = new SequenceTranslator(getSuperTranslator());
        boolean successful = p.translate( topPTE );

        if ( !successful ) { // well, there were an error... stop here
            return false;
        }

        // next, we translate the expressions to search for the variables
        TranslatedExpression translatedPotentialArguments = p.getTranslatedExpressionObject();

        // first, clear global expression
        getGlobalTranslationList().removeLastNExps(translatedPotentialArguments.getLength());

        // find elements that are part of the argument:
        // next, split into argument parts and the rest
        TranslatedExpression transArgs = removeUntilLastAppearence(
                translatedPotentialArguments,
                limit.getVars()
        );

        int lastIdx = limit.getVars().size()-1;

        // start with inner -> last elements in limit
        String finalTranslation = translatePattern(
                limit,
                lastIdx,
                transArgs.getTranslatedExpression(),
                "sum"
        );

        if ( lastIdx > 0 ) {
            for ( int i = lastIdx-1; i >= 0; i-- ) {
                finalTranslation = translatePattern(
                        limit,
                        i,
                        finalTranslation,
                        "sum"
                );
            }
        }

        // add translation and the rest of the translation
        localTranslations.addTranslatedExpression(finalTranslation);
        localTranslations.addTranslatedExpression(translatedPotentialArguments);

        getGlobalTranslationList().addTranslatedExpression(finalTranslation);
        getGlobalTranslationList().addTranslatedExpression(translatedPotentialArguments);

        return true;
    }

    private String translatePattern(Limits limit, int idx, String arg, String key) {
        if ( !limit.isLimitOverSet() ) {
            String[] args = new String[]{
                    limit.getVars().get(idx),
                    limit.getLower().get(idx),
                    limit.getUpper().get(idx),
                    arg,
            };
            return bft.translate(args, key);
        } else {
            String[] args = new String[]{
                    limit.getVars().get(idx),
                    limit.getLower().get(idx),
                    arg,
            };
            return bft.translate(args, key+"Set");
        }
    }

    private Limits extractLimits(PomTaggedExpression limitSuperExpr) {
        MathTerm term = limitSuperExpr.getRoot();

        PomTaggedExpression limitExpression = null;
        List<PomTaggedExpression> upperBound = null;

        // in case it is a MathTerm, it MUST be a lower bound!
        if ( term != null && !term.isEmpty() ) {
            MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
            if ( !tag.equals(MathTermTags.underscore) ) {
                throw new TranslationException("Illegal expression followed a limited expression: " + term.getTermText());
            }
            // underscore always has only one child!
            limitExpression = limitSuperExpr.getComponents().get(0);
        } else {
            String tagS = limitSuperExpr.getTag();
            ExpressionTags tag = ExpressionTags.getTagByKey(tagS);
            if ( tag.equals(ExpressionTags.sub_super_script) ) {
                List<PomTaggedExpression> els = limitSuperExpr.getComponents();
                for ( PomTaggedExpression pte : els ) {
                    MathTermTags t = MathTermTags.getTagByKey(pte.getRoot().getTag());
                    if ( t.equals(MathTermTags.underscore) ) {
                        limitExpression = pte.getComponents().get(0);
                    } else if ( t.equals(MathTermTags.caret) ) {
                        upperBound = pte.getComponents();
                    }
                }
            } else {
                throw new TranslationException("A limited expression without limits is not allowed: " + term.getTermText());
            }
        }

        // now we have limitExpression and an optional upperBound. Parse it:
        BlueprintMaster btm = getConfig().getLimitParser();
        Limits limit = btm.findMatchingLimit(limitExpression);

        // if an upper bound was explicitly given, overwrite the parsed upper bound
        if ( upperBound != null ) {
            TranslatedExpression te = parseGeneralExpression(upperBound.remove(0), upperBound);
            getGlobalTranslationList().removeLastNExps(te.getLength());
            limit.overwriteUpperLimit(te.getTranslatedExpression());
        }

        return limit;
    }

    private List<PomTaggedExpression> getPotentialArgumentsUntilEndOfScope(List<PomTaggedExpression> list) {
        LinkedList<PomTaggedExpression> cache = new LinkedList<>();

        // the very next element is always(!) part of the argument
        if ( list.isEmpty() ) {
            throw new TranslationException(
                    "A limited expression ends with no argument left."
            );
        }
        cache.add(list.remove(0));

        // now add all until there is a stop expression
        while ( !list.isEmpty() ) {
            PomTaggedExpression curr = list.get(0); // do not remove yet!
            MathTerm mt = curr.getRoot();
            if ( mt != null && mt.getTag() != null ) {
                MathTermTags tag = MathTermTags.getTagByKey(mt.getTag());
                // stop only in case of a harsh stop symbol appears on the same level of the sum
                // stoppers are relations (left-hand side and right-hand side).
                switch (tag) {
                    case relation:
                    case equals:
                    case less_than:
                    case greater_than:
                        // found stopper -> return the cache
                        return cache;
                }
            } // if no stopper is found, just add it to the potential list
            cache.addLast(list.remove(0));
        }

        // well, it might be the entire expression until the end, of course
        return cache;
    }

    private TranslatedExpression removeUntilLastAppearence(TranslatedExpression te, List<String> vars) {
        return te.removeUntilLastAppearanceOfVar(vars, getConfig().getMULTIPLY());
    }
}