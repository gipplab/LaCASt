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
            } catch(IndexOutOfBoundsException e){
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
            } catch(IndexOutOfBoundsException e){
                storeIndex = components2.get(0).getRoot().getTermText();
            }
            lowerLim = components2.get(0);
            List<PomTaggedExpression> upperComponents = components.get(0).getComponents();
            upperLim = upperComponents.get(0);
        }

        if(!Character.isLetter(storeIndex.charAt(0))) {
            storeIndex = searchForIndex(list);
        }
        if(!Character.isLetter(storeIndex.charAt(0))){
            storeIndex = "i";
        }
        //lower limit of summation
        int size = lowerLim.getComponents().size();
        PomTaggedExpression toAdd = lowerLim;
        PomTaggedExpression firstToAdd = null;
        if(toAdd.getComponents().size() != 0 && GlobalConstants.CAS_KEY.equals("Mathematica")){
            toAdd = lowerLim.getComponents().get(lowerLim.getComponents().size()-1);
            if(MathTermTags.getTagByKey(lowerLim.getComponents().get(lowerLim.getComponents().size()-2).getRoot().getTag()).equals(MathTermTags.minus))
                firstToAdd = lowerLim.getComponents().get(lowerLim.getComponents().size()-2);
        }
        String summand = "";
        if(firstToAdd != null)
            summand += parseGeneralExpression(firstToAdd, list).toString();
        summand += parseGeneralExpression(toAdd, list).toString();
        args.get(num).add(summand);
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
            if(tag == null){
                sum.add(parseGeneralExpression(list.remove(i), list).toString());
                lastExp = global_exp.removeLastExpression();
                i--;
            } else {
                switch (tag) {
                    case plus:
                    case minus:
                        if(isIndexPresent(list, tempNum)){
                            sum.add(parseGeneralExpression(list.remove(i), list).toString());
                            lastExp = global_exp.removeLastExpression();
                            i--;
                        } else
                            endSummand = true;
                        break;
                    case equals:
                    case less_than:
                    case greater_than:
                    case relation:
                    case right_parenthesis:
                    case right_delimiter:
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
     * If the index is present anywhere in the list, returns true.
     * If there is a relational operator or a right paren without a corresponding left paren,
     * stops searching and returns false.
     *
     * @param list, the list of following expressions after the sum.
     * @param tempNum, the current number of sums, used to access the right index.
     * @return
     */
    private boolean isIndexPresent(List<PomTaggedExpression> list, int tempNum){
        int numParen = 0;
        for(PomTaggedExpression ex : list){
            String text = ex.getRoot().getTermText();
            MathTermTags tag = MathTermTags.getTagByKey(ex.getRoot().getTag());
            //if the index is found, return true
            if(text.equals(indices.get(tempNum))){
                return true;
            }
            //if the expression has subcomponents, call isIndexPresent on the list of components
            if(ex.getComponents().size() != 0 && isIndexPresent(ex.getComponents(), tempNum)){
                return true;
                //if the tag is null, don't do anything
            } else if(tag != null){
                //stop if there are any of these things
                switch (tag) {
                    case equals:
                    case less_than:
                    case greater_than:
                    case relation:
                        return false;
                    //count the number of parenthesis
                    //if there are more right parens than left parens, stop.
                    case left_parenthesis:
                        numParen++;
                        break;
                    case right_parenthesis:
                        numParen--;
                        if (numParen < 0)
                            return false;
                        break;
                }
            }
        }
        return false;
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