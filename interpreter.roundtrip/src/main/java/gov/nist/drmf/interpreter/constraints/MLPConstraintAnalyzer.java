package gov.nist.drmf.interpreter.constraints;

import gov.nist.drmf.interpreter.common.GlobalPaths;
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

    private LinkedList<MLPBlueprintTree> blueprints;

    private static MLPConstraintAnalyzer analyzer;

    private MLPConstraintAnalyzer() {
        btPath = GlobalPaths.PATH_LIBS.resolve("blueprints.txt");
        blueprints = new LinkedList<>();
    }

    public void init() throws IOException {
        Stream<String> lines = Files.readAllLines(btPath).stream();
        lines.map(l -> l.split(" ==> "))
                .forEach(a -> {
                    String[] values = a[1].split(",");
                    MLPBlueprintTree bt = new MLPBlueprintTree(values);
                    try {
                        bt.setBlueprint(a[0]);
                        blueprints.add(bt);
                    } catch (ParseException pe) {
                        LOG.error("Cannot parse Constraint-Blueprint!", pe);
                    }
                });
    }

    public String[][] checkForBlueprintRules(String constraint) throws ParseException {
        MLPBlueprintNode con = MLPBlueprintTree.parseTree(constraint);
        return internalCheck(con);
    }

    public String[][] checkForBlueprintRules(PomTaggedExpression constraintParentNode) {
        MLPBlueprintNode con = MLPBlueprintTree.parseTree(constraintParentNode);
        return internalCheck(con);
    }

    private String[][] internalCheck(MLPBlueprintNode con){
        MLPBlueprintTree bt = blueprints.stream()
                .filter(b -> b.matches(con))
                .findAny()
                .orElse(null);
        if (bt != null) {
            return bt.getConstraintVariablesAndValues();
        } else {
            return null;
        }
    }

    public static MLPConstraintAnalyzer getAnalyzerInstance(){
        if ( analyzer == null ){
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
        MLPConstraintAnalyzer a = new MLPConstraintAnalyzer();
        a.init();

        String[][] varvals = a.checkForBlueprintRules("2 q \\ne -1,-2,-3, \\dotsc");
        System.out.println(Arrays.toString(varvals[0]) + " - " + Arrays.toString(varvals[1]));

        varvals = a.checkForBlueprintRules("2 q \\neq -1,-2,-3 \\ldotsc");
        System.out.println(Arrays.toString(varvals[0]) + " - " + Arrays.toString(varvals[1]));

        varvals = a.checkForBlueprintRules("2 q \\ne -1,-2,-3 \\cdotsb");
        System.out.println(Arrays.toString(varvals[0]) + " - " + Arrays.toString(varvals[1]));
    }

}
