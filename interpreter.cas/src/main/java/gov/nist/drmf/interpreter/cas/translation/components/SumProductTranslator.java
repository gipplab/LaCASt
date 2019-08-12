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

        //put the arguments to the sum in the list of sum args
        int numArgs = addToArgs(next, list);

        //add factors to summand
        addFactorsToSummand(list, tempNum, numArgs);

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
    private int onlyLower(PomTaggedExpression next, List<PomTaggedExpression> list){
        List<PomTaggedExpression> components = next.getComponents();
        List<PomTaggedExpression> components2 = components.get(0).getComponents();
        String storeIndex = searchForIndex(components);
        indices.add(storeIndex);
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
        String storeIndex;
        PomTaggedExpression index2;
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
                index2 = components3.get(0);
            } catch(IndexOutOfBoundsException e){
                storeIndex = components2.get(0).getRoot().getTermText();
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
                storeIndex = components3.get(0).getRoot().getTermText();
                index2 = components3.get(0);
            } catch(IndexOutOfBoundsException e){
                storeIndex = components2.get(0).getRoot().getTermText();
                index2 = components2.get(0);
            }
            lowerLim = components2.get(0);
            List<PomTaggedExpression> upperComponents = components.get(0).getComponents();
            upperLim = upperComponents.get(0);
        }
        boolean needNewIndex = !Character.isLetter(storeIndex.charAt(0)) && !MathTermTags.getTagByKey(index2.getRoot().getTag()).equals(MathTermTags.special_math_letter);
        if(needNewIndex) {
            storeIndex = searchForIndex(list);
        }
        if(needNewIndex){
            storeIndex = "i";
        }
        indices.add(storeIndex);
        //lower limit of summation
        //remove the index from the lower limit
        int size = lowerLim.getComponents().size();
        if(lowerLim.getComponents().size() > 1 && GlobalConstants.CAS_KEY.equals("Mathematica")){
            String newLowerLim = "";
            String lastTemp = "";
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
                }
            }
            args.get(num).add(newLowerLim);
            remove(size-index);
        } else{
            args.get(num).add(parseGeneralExpression(lowerLim, list).toString());
            //remove the expression that parseGeneralExpression added to global_exp, because that expression is being used as a sum arg.
            remove(size);
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
    private int addToArgs(PomTaggedExpression next, List<PomTaggedExpression> list){
        ExpressionTags tag = ExpressionTags.getTagByKey(next.getTag());
        //only upper limit, only lower limit, or no limits with a summand of length 1.
        if(tag == null && MathTermTags.getTagByKey(next.getRoot().getTag()).equals(MathTermTags.underscore)){
            return onlyLower(next, list);
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
    private void addFactorsToSummand(List<PomTaggedExpression> list, int tempNum, int numArgs){
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
            //if there is a sum with the same index of summation, stop.
            if(list.get(i).getComponents().size() != 0 && isIndexPresent(list.get(i).getComponents(), tempNum) == -2) {
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
                        if(isIndexPresent(list, tempNum) == 1){
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
                        List<PomTaggedExpression> components = list.get(i+1).getComponents();
                        List<PomTaggedExpression> components2 = components.get(0).getComponents();
                        List<PomTaggedExpression> components3 = components2.get(0).getComponents();
                        String text = components3.get(0).getRoot().getTermText();
                        if(text.equals(indices.get(tempNum))) {
                            endSummand = true;
                            break;
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
    private int isIndexPresent(List<PomTaggedExpression> list, int tempNum){
        int numParen = 0;
        MathTermTags lastTag = null;
        for(PomTaggedExpression ex : list){
            String text = ex.getRoot().getTermText();
            MathTermTags tag = MathTermTags.getTagByKey(ex.getRoot().getTag());
            //if the index is found, return true
            if(text.equals(indices.get(tempNum))){
                return 1;
            }
            //If theres a sum with the same index as this sum's index of summation, stop searching and return -1.
            boolean conds = lastTag != null && ex.getComponents().size() != 0;
            if(conds && lastTag.equals(MathTermTags.operator) && isIndexPresent(ex.getComponents().get(0).getComponents(), tempNum) == 1)
                return -2;

            if(ex.getComponents().size() != 0) {
                int val = isIndexPresent(ex.getComponents(), tempNum);
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