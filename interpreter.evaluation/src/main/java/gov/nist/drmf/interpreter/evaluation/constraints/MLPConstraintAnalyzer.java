package gov.nist.drmf.interpreter.evaluation.constraints;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Stream;

/**
 * Ok let's start simple without constraint splitting.
 * Assume an input like: "n = 1,2,3,\dots"
 * <p>
 * 1) Translate to MLP-Parse-Tree
 * 2) Compare with BlueprintTrees
 * 3) Hit? -> take it as Tuple<String,String> with "n" and "1".
 *
 * @author Andre Greiner-Petter
 */
public class MLPConstraintAnalyzer {
    private static final Logger LOG = LogManager.getLogger(MLPConstraintAnalyzer.class.getName());

    private Path btPath;

    private LinkedList<ConstraintBlueprint> blueprints;

    private static MLPConstraintAnalyzer analyzer;

    private MLPConstraintAnalyzer() {
        btPath = GlobalPaths.PATH_CONSTRAINT_BLUEPRINTS;
        blueprints = new LinkedList<>();
    }

    public void init() throws IOException {
        Stream<String> lines = Files.readAllLines(btPath).stream();
        lines.map(l -> l.split(" ==> "))
                .forEach(a -> {
                    String[] values = a[1].split(",");
                    try {
                        ConstraintBlueprint bt = new ConstraintBlueprint(a[0], values);
                        blueprints.add(bt);
                    } catch (ParseException pe) {
                        LOG.error("Cannot parse Constraint-Blueprint!", pe);
                    }
                });
    }

    public String[][] checkForBlueprintRules(String constraint) throws ParseException {
//        MLPBlueprintNode con = MLPBlueprintTree.parseTree(constraint);
        return internalCheck(constraint);
    }

//    public String[][] checkForBlueprintRules(PomTaggedExpression constraintParentNode) {
//        MLPBlueprintNode con = MLPBlueprintTree.parseTree(constraintParentNode);
//        return internalCheck(con);
//    }

    private String[][] internalCheck(String con){
//        return null;
        try {
            for ( ConstraintBlueprint bt : blueprints ) {
                if ( bt.match(con) ) {
                    return bt.getConstraintVariables();
                }
            }
            return null;
        } catch (Exception | Error re) {
            return null;
        }
    }

    public static MLPConstraintAnalyzer getAnalyzerInstance() {
        if (analyzer == null) {
            analyzer = new MLPConstraintAnalyzer();
            try {
                analyzer.init();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return analyzer;
    }

    public static void main(String[] args) throws Exception {
        String s = "var1-\\frac{1}{2} var2 - \\frac{1}{2} =0,1,2,\\dots";
        String test = "\\kappa-\\frac{1}{2}n-\\frac{1}{2}=0,1,2,\\dots";
        ConstraintBlueprint cb = new ConstraintBlueprint(s, "1", "2");
        System.out.println(cb.match(test));
        System.out.println(Arrays.toString(cb.getConstraintVariables()[0]));
        System.out.println(Arrays.toString(cb.getConstraintVariables()[1]));

    }

}
