package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.SemanticToCASInterpreter;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;

import gov.nist.drmf.interpreter.common.TranslationException;
import mlp.PomTaggedExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * SumProductTranslator manually adds "Sum[]" (or "sum()" for Maple) or "Prod[]" to local and global exp.
 * It stores the arguments to the sum in sumArgs
 * and then inserts them into the right places inside the Sum[]
 *
 * @author Rajen Dey
 *
 * July 2019
 */
public class SumProductTranslator extends AbstractListTranslator{

    public static ArrayList<String> sumArgs = new ArrayList<>();

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
        for(int i = 0; i < SemanticToCASInterpreter.numArgs; i++) {
            sumArgs.add(parseGeneralExpression(list.remove(0), list).toString());
        }

        return addSumArgs();
    }

    /**
     * Now we need to add the arguments to the sum or product
     * This method finds where sum or product was added to the translated expression,
     * Splits it at that point, adds in the arguments that sum or product needs,
     * (adding commas and curly braces and dots and stuff where necessary)
     * and finally places the finished expression into local and global exp.
     */
    private boolean addSumArgs(){
        if(sumArgs.size() == SemanticToCASInterpreter.numArgs){
            String newTrans = "";
            //if the CAS is Mathematica do this
            if(GlobalConstants.CAS_KEY.equals("Mathematica")) {
                int index = local_inner_exp.toString().indexOf("Sum[") + 4;
                //if there is no sum, then there must be a product
                if(index == 3)
                    index = local_inner_exp.toString().indexOf("Prod[") + 5;
                //if the sum/prod needs 3 args do this
                if(SemanticToCASInterpreter.numArgs == 3) {
                    newTrans += local_inner_exp.toString().substring(0, index) + sumArgs.get(2) +
                            ", {";
                    if (!SemanticToCASInterpreter.reverse) {
                        newTrans += sumArgs.get(0) + "," + sumArgs.get(1) + "}";
                    } else {
                        newTrans += sumArgs.get(1) + "," + sumArgs.get(0) + "}";
                    }
                    newTrans += local_inner_exp.toString().substring(index);

                    //if it only needs 2 args do this. for example, only a lower limit defined.
                } else if(SemanticToCASInterpreter.numArgs == 2){
                    newTrans += local_inner_exp.toString().substring(0, index) + sumArgs.get(1)
                            + ", " + sumArgs.get(0) + local_inner_exp.toString().substring(index);
                    //cant have only 1 or 2 args.
                } else {
                    throw new TranslationException("Mathematica needs at least 2 arguments to a sum or product");
                }
            }
            //if the CAS is Maple do this
            if(GlobalConstants.CAS_KEY.equals("Maple")){
                int index = local_inner_exp.toString().indexOf("sum(") + 4;
                //if there is no sum then there must be a product
                if(index == 3)
                    index = local_inner_exp.toString().indexOf("product(") + 8;
                //if the sum/prod needs 3 args do this
                if(SemanticToCASInterpreter.numArgs == 3) {
                    newTrans += local_inner_exp.toString().substring(0, index) + sumArgs.get(2) +
                            ", ";
                    if (!SemanticToCASInterpreter.reverse) {
                        newTrans += sumArgs.get(0) + ".." + sumArgs.get(1);
                    } else {
                        newTrans += sumArgs.get(1) + ".." + sumArgs.get(0);
                    }
                    newTrans += local_inner_exp.toString().substring(index);
                }
                //if it only needs 2 args do this
                else if(SemanticToCASInterpreter.numArgs == 2){
                    newTrans += local_inner_exp.toString().substring(0, index) + sumArgs.get(1)
                            + ", " + sumArgs.get(0) + local_inner_exp.toString().substring(index);
                    //if it only has 1 or 2 args then do this.
                } else
                    throw new TranslationException("Maple needs at least 2 arguments for a sum or product");

                int count = 0;
                int endIndex = 0;

                //Translation to Maple generates some extra *'s at the end of the sum/product that we need to delete
                //Find where the end of the sum/product is
                for(int i = index - 1; i < newTrans.length(); i++){
                    if(newTrans.charAt(i) == '(')
                        count++;
                    if(newTrans.charAt(i) == ')')
                        count--;
                    if(count == 0){
                        endIndex = i;
                        break;
                    }
                }
                //delete the all the extra *'s
                while(endIndex+1 < newTrans.length() && newTrans.charAt(endIndex+1) == '*'){
                    newTrans = newTrans.substring(0, endIndex + 1) + newTrans.substring(endIndex + 2);
                }
            }
            sumArgs.clear();
            //clear local and global exp and add the new expression
            local_inner_exp.clear();
            local_inner_exp.addTranslatedExpression(newTrans);
            global_exp.clear();
            global_exp.addTranslatedExpression(newTrans);
            return true;
        } return false;
    }
}