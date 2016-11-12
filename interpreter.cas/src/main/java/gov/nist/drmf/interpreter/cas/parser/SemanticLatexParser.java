package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.parser.components.EmptyExpressionParser;
import gov.nist.drmf.interpreter.cas.parser.components.MathTermParser;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.nio.file.Path;
import java.util.List;

/**
 * This parser parse semantic LaTeX formula using
 * the math processor language by Abdou Youssef.
 * It based on BNF grammar programmed with JavaCC.
 *
 * It is the top level parser objects. That means
 * you can use {@link #parse(String)} to parse an
 * expression in general. To do so, you have to
 * invoke {@link #init(Path)} before you use this
 * parse method. On the other hand this parser can
 * handle also general PomTaggedExpression to parse.
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class SemanticLatexParser extends AbstractParser {
    private PomParser parser;

    public SemanticLatexParser(){
        // setup
    }

    /**
     * Initialize parser.
     * @param reference_dir_path
     */
    public void init( Path reference_dir_path ){
        parser = new PomParser(reference_dir_path.toString());
    }

    /**
     *
     * @param expression
     * @return
     */
    public boolean parse(String expression){
        try {
            PomTaggedExpression exp = parser.parse(expression);
            return parse(exp);
        } catch ( ParseException pe ){
            return false;
        }
    }

    @Override
    public boolean parse(PomTaggedExpression expression) {
        MathTerm root = expression.getRoot();
        translatedExp = parseGeneralExpression(expression);
        return !isInnerError();
    }
}
