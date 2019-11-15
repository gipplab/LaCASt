package gov.nist.drmf.interpreter.mlp.extensions.mathml;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Andre Greiner-Petter
 */
public class MathMLInterpreter {

    private PomParser parser;

    private File outputFile;

    private PomTaggedExpression topExpression;

    public MathMLInterpreter ( Path relative_output ){
        outputFile = relative_output.toFile();
    }

    public void init( String string_expression ){
        try {
            parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA);
            outputFile.createNewFile();
            topExpression = parser.parse(string_expression);
        } catch ( IOException | ParseException e ) {
            e.printStackTrace();
        }
    }

    public void parse(){

    }
}
