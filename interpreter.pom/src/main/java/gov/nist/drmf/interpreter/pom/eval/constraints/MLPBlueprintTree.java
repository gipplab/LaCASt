package gov.nist.drmf.interpreter.pom.eval.constraints;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.pom.MLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MLPBlueprintTree {

    public static final String VAR = "var(\\d?)";
    public static final Pattern VAR_PATTERN = Pattern.compile(VAR);

    private MLPBlueprintNode parent;

    private HashMap<Integer, String> texVariables;
    private final String[] mapleValues;

    private MLPWrapper mlp;

    public MLPBlueprintTree(String[] mapleValues) {
        this.mapleValues = mapleValues;
        this.texVariables = new HashMap<>();
        this.mlp = SemanticMLPWrapper.getStandardInstance();
    }

    public void setBlueprint(String blueprint) throws ParseException {
        blueprint = preCleaning(blueprint);
        PomTaggedExpression pte = mlp.parse(blueprint);
        setBlueprint(pte);
    }

    private void setBlueprint(PomTaggedExpression element){
        this.parent = createBlueprint(this, element);
    }

    public static MLPBlueprintNode parseTree(String constraint) throws ParseException {
        String blueprint = preCleaning(constraint);
        PomTaggedExpression pte = SemanticMLPWrapper.getStandardInstance().parse(blueprint);
        return createBlueprint(pte);
    }

    public static MLPBlueprintNode parseTree(PomTaggedExpression constraintParentNode){
        return createBlueprint(constraintParentNode);
    }

    private static MLPBlueprintNode createBlueprint(PomTaggedExpression element){
        return createBlueprint(null, element);
    }

    private static MLPBlueprintNode createBlueprint(MLPBlueprintTree bt, PomTaggedExpression element) {
        MathTerm term = element.getRoot();
        if (term == null || term.isEmpty()) {
            // its a sequence, first analyze all kids
            List<PomTaggedExpression> list = element.getComponents();
            ArrayList<MLPBlueprintNode> kids = new ArrayList<>();

            for (PomTaggedExpression pte : list) {
                kids.add(createBlueprint(bt, pte));
            }

            return new MLPBlueprintNode(bt, kids);
        } else {
            // its a child / mathterm -> take it
            if (MathTermUtility.isGreekLetter(term)) {
                return new MLPBlueprintNode(bt, term.getTermText(), MLPBlueprintNode.GREEK);
            } else {
                String tag = term.getTag();
                if ( tag == null || tag.isEmpty() ){
                    List<String> tags = term.getSecondaryTags();
                    if ( !tags.isEmpty() ) tag = tags.get(0);
                }
                return new MLPBlueprintNode(bt, term.getTermText(), tag);
            }
        }
    }

    public boolean matches(MLPBlueprintNode other){
        texVariables.clear();
        boolean r = parent.equals(other);
        if (!r) texVariables.clear();
        return r;
    }

    protected void setVariable(String varPattern, String texVariable) {
        Matcher matcher = VAR_PATTERN.matcher(varPattern);
        if (matcher.matches()) {
            String digit = matcher.group(1);
            Integer i;
            if ( digit == null || digit.isEmpty() )
                i = 0;
            else i = Integer.parseInt(digit);
            this.texVariables.put(i, texVariable);
        } else {
            throw new IllegalArgumentException("Illegal VarPatter. Should be 'var<digit>' but was " + varPattern);
        }
    }

    public String[][] getConstraintVariablesAndValues() {
        String[][] out = new String[2][];
        Set<Integer> vars = texVariables.keySet();

        out[0] = new String[vars.size()];
        out[1] = mapleValues;

        int idx = 0;
        for (Integer i : vars) {
            out[0][idx] = texVariables.get(i);
            idx++;
        }

        return out;
    }

    public static String preCleaning(String constraint){
        constraint = TeXPreProcessor.preProcessingTeX(constraint);
        constraint = constraint.replaceAll("\\\\[lc]?dots[cbmio]?", "\\\\dots");
        constraint = constraint.replaceAll("([^,\\s])\\s*\\\\dots", "$1, \\\\dots");
        constraint = constraint.replaceAll("\\\\[it]?frac", "\\\\frac");
        constraint = constraint.replaceAll("\\\\ne[^\\w]", "\\\\neq");
        constraint = constraint.replaceAll("[\\s,;.]*$", "");
        constraint = constraint.replaceAll("@*", "");
        return constraint;
    }
}
