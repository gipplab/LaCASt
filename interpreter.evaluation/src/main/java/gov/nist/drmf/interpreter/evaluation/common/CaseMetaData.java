package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.evaluation.constraints.MLPConstraintAnalyzer;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class CaseMetaData {
    private static final Logger LOG = LogManager.getLogger(CaseMetaData.class.getName());

    private Label label;
    private Constraints constraints;
    private boolean isDefinition = false;

    private LinkedList<SymbolTag> symbolsUsed;

    private Map<Integer, String> defConVarSlot;

    private int linenumber;

    private static MLPConstraintAnalyzer analyzer = MLPConstraintAnalyzer.getAnalyzerInstance();

    public CaseMetaData(int linenumber, Label label, Constraints constraints, LinkedList<SymbolTag> symbolsUsed){
        this.label = label;
        this.constraints = constraints;
        this.linenumber = linenumber;
        this.symbolsUsed = symbolsUsed;
        this.defConVarSlot = new HashMap<>();
    }

    public void addVariableSlot(int slot, String var) {
        this.defConVarSlot.put(slot, var);
    }

    public Map<Integer, String> getVariableSlots() {
        return defConVarSlot;
    }

    public boolean isDefinition() {
        return isDefinition;
    }

    public void tagAsDefinition() {
        this.isDefinition = true;
    }

    public Label getLabel() {
        return label;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void addConstraints(Constraints c) {
        if ( constraints == null ) this.constraints = c;
        else constraints.addConstraints(c);
    }

    public int getLinenumber() {
        return linenumber;
    }

    public void deleteConstraints() {
        this.constraints = null;
    }

    public LinkedList<SymbolTag> getSymbolsUsed() {
        return symbolsUsed;
    }

    private static final Pattern COMMA_PATTERN = Pattern.compile("(.*\\\\[lc]?dots)\\s*,\\s*([^a-zA-Z]+)");

    public static CaseMetaData extractMetaData(
            LinkedList<String> constraints,
            LinkedList<SymbolTag> symbolsUsed,
            String labelStr,
            int lineNumber
    ) {
        LinkedList<String> copyConstraints = new LinkedList<>(constraints);

        constraints = constraints.stream()
                .flatMap(c -> {
                    Matcher m = COMMA_PATTERN.matcher(c);
                    Collection<String> col = new LinkedList<>();
                    if (m.matches()) {
                        col.add(m.group(1));
                        col.add(m.group(2));
                    } else col.add(c);
                    return col.stream();
                }).collect(Collectors.toCollection(LinkedList::new));

        // first, create label
        Label label = null;
        if ( labelStr != null ){
            label = new Label(labelStr);
        }

        if ( constraints.isEmpty() )
            return new CaseMetaData(lineNumber, label, null, symbolsUsed);

        LinkedList<String> sieved = new LinkedList<>();
        LinkedList<String[][]> varVals = new LinkedList<>();

        int length = sieve(constraints, sieved, varVals, lineNumber);

        String[] specialVars = new String[length];
        String[] specialVals = new String[length];

        int idx = 0;
        while ( !varVals.isEmpty() ){
            String[][] varval = varVals.removeFirst();
            for ( int i = 0; i < varval[0].length; i++, idx++ ){
                specialVars[idx] = varval[0][i];
                specialVals[idx] = varval[1][i];
            }
        }

        String[] conArr = sieved.stream()
                .flatMap( c -> {
                    EquationSplitter eq = new EquationSplitter();
                    Collection<String> col = eq.constraintSplitter(c);
                    return col.stream();
                })
                .toArray(String[]::new);
        Constraints finalConstr = new Constraints(copyConstraints, conArr, specialVars, specialVals);
        return new CaseMetaData(lineNumber, label, finalConstr, symbolsUsed);
    }

    private static int sieve(
            LinkedList<String> constraints,
            LinkedList<String> sieved,
            LinkedList<String[][]> varVals,
            int lineNumber
    ) {
        int length = 0;
        while ( !constraints.isEmpty() ){
            String con = constraints.removeFirst();
            try {
                String[][] rule = analyzer.checkForBlueprintRules(con);
                // some constraints are buggy... consider \nu\geq 1,x\in\Reals
                if ( rule == null ) {
                    length = withRule(varVals, sieved, con, length);
                }

                length += updateLists(rule, con, varVals, sieved);
            } catch ( ParseException | RuntimeException pe ){
                LOG.warn("Cannot parse constraint of line " + lineNumber + ". Reason: " + pe.getMessage());
            }
        }
        return length;
    }

    private static int updateLists(String[][] rule, String con, LinkedList<String[][]> varVals, LinkedList<String> sieved) {
        int length = 0;
        if ( rule != null && CaseAnalyzer.ACTIVE_BLUEPRINTS ) {
            varVals.add(rule);
            length = rule[0].length;
        } else sieved.add(con);
        return length;
    }

    private static int withRule(
            LinkedList<String[][]> varVals,
            LinkedList<String> sieved,
            String con,
            int length
    ) throws ParseException {
        LinkedList<String[][]> innerRulesList = new LinkedList<>();
        LinkedList<String> innerSieved = new LinkedList<>();
        String[] multiCon = con.split(",");
        for ( String c : multiCon ) {
            try {
                String[][] innerRule = analyzer.checkForBlueprintRules(c);
                if ( innerRule != null ) innerRulesList.addLast(innerRule);
                else innerSieved.add(c);
            } catch (Error | Exception e) {
                // in case it didn't work, we simple don't do something
            }
        }
        if ( !innerRulesList.isEmpty() ){
            for ( String[][] tmp : innerRulesList ) {
                varVals.add(tmp);
                length += tmp[0].length;
            }
            sieved.addAll(innerSieved);
        }
        return length;
    }
}
