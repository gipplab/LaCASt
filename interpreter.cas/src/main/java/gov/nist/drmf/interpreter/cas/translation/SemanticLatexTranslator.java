package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.SumProductTranslator;
import gov.nist.drmf.interpreter.common.*;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import gov.nist.drmf.interpreter.cas.SemanticToCASInterpreter;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This translation translate semantic LaTeX formula using
 * the math processor language by Abdou Youssef.
 * It based on BNF grammar programmed with JavaCC.
 *
 * It is the top level translation objects. That means
 * you can use {@link #translate(String)} to translate an
 * expression in general. To do so, you have to
 * invoke {@link #init(Path)} before you use this
 * translate method. On the other hand this translation can
 * handle also general PomTaggedExpression to translate.
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class SemanticLatexTranslator extends AbstractTranslator {

    public static String TAB = "";

    private static GreekLetters greekLetters;
    private static Constants constants;
    private static BasicFunctionsTranslator functions;
    private static SymbolTranslator symbols;

    private PomParser parser;

    public SemanticLatexTranslator(String from_language, String to_language ){
        TranslationException.FROM_LANGUAGE_DEFAULT = from_language;
        TranslationException.TO_LANGUAGE_DEFAULT = to_language;

        greekLetters = new GreekLetters(from_language, to_language);
        constants = new Constants(Keys.KEY_DLMF, to_language);
        functions = new BasicFunctionsTranslator(to_language);
        symbols = new SymbolTranslator(from_language, to_language);

        INFO_LOG = new InformationLogger();

        global_exp = new TranslatedExpression();
        int length = GlobalConstants.CAS_KEY.length()+1 > "DLMF: ".length() ?
                (GlobalConstants.CAS_KEY.length()+2) : "DLMF: ".length();
        for ( int i = 0; i <= length; i++ )
            TAB += " ";
    }

    /**
     * Initializes the back end for the translation from semantic LaTeX to
     * a computer algebra system. It loads all translation information
     * from the files in the given path and instantiate the PomParser from
     * Prof. Abdou Youssef.
     * @param reference_dir_path the path to the ReferenceData directory.
     *                           You can find the path in
     *                           {@link GlobalPaths#PATH_REFERENCE_DATA}.
     * @throws IOException if it is not possible to read the information
     *                      from the files.
     */
    public void init( Path reference_dir_path ) throws IOException {
        greekLetters.init();
        constants.init();
        functions.init();
        symbols.init();

        MacrosLexicon.init();

        MULTIPLY = symbols.translateFromMLPKey( Keys.MLP_KEY_MULTIPLICATION );

        parser = new PomParser(reference_dir_path.toString());
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );
    }

    /**
     *
     * @param expression
     * @return
     */
    public boolean translate( String expression ) throws TranslationException {
        if ( expression == null || expression.isEmpty() ) return false;
        expression = TeXPreProcessor.preProcessingTeX(expression);
        try {
            PomTaggedExpression exp = parser.parse(expression);
            return translate( exp );
        } catch ( ParseException pe ){
            return handleNull( null, pe.getMessage(), TranslationException.Reason.MLP_ERROR, expression, pe );
        }
    }

    @Override
    public boolean translate( PomTaggedExpression expression ) throws TranslationException {
        reset();
        local_inner_exp.addTranslatedExpression(
                parseGeneralExpression(expression, null).getTranslatedExpression()
        );

        if ( isInnerError() ){
            handleNull( null,
                "Wasn't able to translate the given expression.",
                TranslationException.Reason.NULL,
                expression.toString(),
                null);
        }

        addSumArgs();
        return true;
    }

    public static GreekLetters getGreekLettersParser(){
        return greekLetters;
    }

    public static Constants getConstantsParser(){
        return constants;
    }

    public static BasicFunctionsTranslator getBasicFunctionParser(){ return functions; }

    public static SymbolTranslator getSymbolsTranslator(){
        return symbols;
    }

    public InformationLogger getInfoLog(){
        return INFO_LOG;
    }

    /**
     * Now we need to add the arguments to the sum or product
     * This method finds where sum or product was added to the translated expression,
     * Splits it at that point, adds in the arguments that sum or product needs,
     * (adding commas and curly braces and dots and stuff where necessary)
     * and finally places the finished expression into local and global exp.
     */
    private void addSumArgs(){
        //only do this if there was a sum or product
        if(SumProductTranslator.sumArgs.size() == SemanticToCASInterpreter.numArgs){
            String newTrans = "";
            //if the CAS is Mathematica do this
            if(GlobalConstants.CAS_KEY.equals("Mathematica")) {
                int index = local_inner_exp.toString().indexOf("Sum[") + 4;
                //if there is no sum, then there must be a product
                if(index == 3)
                    index = local_inner_exp.toString().indexOf("Prod[") + 5;
                //if the sum/prod needs 3 args do this
                if(SemanticToCASInterpreter.numArgs == 3) {
                    newTrans += local_inner_exp.toString().substring(0, index) + SumProductTranslator.sumArgs.get(2) +
                            ", {";
                    if (!SemanticToCASInterpreter.reverse) {
                        newTrans += SumProductTranslator.sumArgs.get(0) + "," + SumProductTranslator.sumArgs.get(1) + "}";
                    } else {
                        newTrans += SumProductTranslator.sumArgs.get(1) + "," + SumProductTranslator.sumArgs.get(0) + "}";
                    }
                    newTrans += local_inner_exp.toString().substring(index);

                    //if it only needs 2 args do this. for example, only a lower limit defined.
                } else if(SemanticToCASInterpreter.numArgs == 2){
                    newTrans += local_inner_exp.toString().substring(0, index) + SumProductTranslator.sumArgs.get(1)
                            + ", " + SumProductTranslator.sumArgs.get(0) + local_inner_exp.toString().substring(index);
                    //cant have only 1 or 2 args.
                } else
                    throw new TranslationException("Mathematica needs at least 2 arguments to a sum or product");
            }
            //if the CAS is Maple do this
            if(GlobalConstants.CAS_KEY.equals("Maple")){
                int index = local_inner_exp.toString().indexOf("sum(") + 4;
                //if there is no sum then there must be a product
                if(index == 3)
                    index = local_inner_exp.toString().indexOf("product(") + 8;
                //if the sum/prod needs 3 args do this
                if(SemanticToCASInterpreter.numArgs == 3) {
                    newTrans += local_inner_exp.toString().substring(0, index) + SumProductTranslator.sumArgs.get(2) +
                            ", ";
                    if (!SemanticToCASInterpreter.reverse) {
                        newTrans += SumProductTranslator.sumArgs.get(0) + ".." + SumProductTranslator.sumArgs.get(1);
                    } else {
                        newTrans += SumProductTranslator.sumArgs.get(1) + ".." + SumProductTranslator.sumArgs.get(0);
                    }
                    newTrans += local_inner_exp.toString().substring(index);
                }
                //if it only needs 2 args do this
                else if(SemanticToCASInterpreter.numArgs == 2){
                    newTrans += local_inner_exp.toString().substring(0, index) + SumProductTranslator.sumArgs.get(1)
                            + ", " + SumProductTranslator.sumArgs.get(0) + local_inner_exp.toString().substring(index);
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
            //clear local and global exp and add the new expression
            local_inner_exp.clear();
            local_inner_exp.addTranslatedExpression(newTrans);
            global_exp.clear();
            global_exp.addTranslatedExpression(newTrans);
        }
    }
}
