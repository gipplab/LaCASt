import mlp.*;
import gov.nist.drmf.interpreter.examples.MLP;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class MacroTests {
    private static final String LOCAL_RESOURCE_PATH =
            "interpreter.cas\\src\\main\\resources";

    private static final String MACRO_LEXICON =
            "Lexicons\\dlmf-macro-lexicon.txt";

    private static final String EXAMPLE_FUNCTION =
            "\\Mathieuce{n}@@{x}{y}";

    public static void main(String[] args){
        MacroTests local_class = new MacroTests();
        //local_class.storeCombinedLexicon();
        PomParser parser = new PomParser(LOCAL_RESOURCE_PATH);
        String equation = EXAMPLE_FUNCTION;
        try{
            //local_class.analyzeEquation(equation, parser);
            PomTaggedExpression exp = parser.parse(equation);
            System.out.println(exp.toString());
        } catch ( ParseException pe ){
            pe.printStackTrace();
        }
    }

    private void analyzeEquation(String equation, PomParser parser)
            throws ParseException{
        PomTaggedExpression exp = parser.parse(equation);
        List<PomTaggedExpression> expressions = exp.getComponents();

        for ( int i = 0; i < expressions.size(); i++ ){
            PomTaggedExpression curr = expressions.get(i);
            MathTerm math = curr.getRoot();
            System.out.println(math.getTag());

            Map<String,String> features = math.getNamedFeatures();
            System.out.println("Features and values:");
            for ( String feature : features.keySet() ){
                System.out.println(feature + " : " + features.get(feature));
            }
        }
    }

    private void storeCombinedLexicon(){
        try {
            Lexicon new_lex = generateNewGlobalLexicon();
            new_lex.outputLexicon(
                    "Symbol: ",
                    "Feature Set:",
                    Paths.get(LOCAL_RESOURCE_PATH, "Lexicons\\global-lexicon.txt").toString()
            );
        } catch (IOException ioe){
            System.err.println("IOError");
            ioe.printStackTrace();
        }
    }

    private Lexicon generateNewGlobalLexicon() throws IOException {
        // load global lexicon
        Lexicon global_lex = LexiconFactory.createLexicon(
                Paths.get(MLP.GLOBAL_LEXICON_PATH,
                        "Lexicons",
                        "global-lexicon.txt"
                ).toString(),
                "Symbol: ",
                "Feature Set:",
                "-",
                "\\|\\|"
        );

        // load macro lexicon
        Lexicon macro_lex = LexiconFactory.createLexicon(
                Paths.get(LOCAL_RESOURCE_PATH, MACRO_LEXICON).toString(),
                "Symbol: ",
                "Feature Set:",
                "-",
                "\\|\\|"
        );

        // add all macros to global lexicon
        Set<String> entries = macro_lex.getEntryKeys();
        for ( String macro : entries ){
            global_lex.addEntry(
                    macro,
                    macro_lex.getFeatureSets(macro)
            );
        }

        return global_lex;
    }
}
