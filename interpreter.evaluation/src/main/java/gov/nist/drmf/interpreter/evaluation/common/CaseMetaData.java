package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.evaluation.constraints.MLPConstraintAnalyzer;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class CaseMetaData {
    private static final Logger LOG = LogManager.getLogger(CaseMetaData.class.getName());

    private Label label;
    private Constraints constraints;

    private LinkedList<SymbolTag> symbolsUsed;

    private int linenumber;

    private static MLPConstraintAnalyzer analyzer = MLPConstraintAnalyzer.getAnalyzerInstance();

    public CaseMetaData(int linenumber, Label label, Constraints constraints, LinkedList<SymbolTag> symbolsUsed){
        this.label = label;
        this.constraints = constraints;
        this.linenumber = linenumber;
        this.symbolsUsed = symbolsUsed;
    }

    public Label getLabel() {
        return label;
    }

    public Constraints getConstraints() {
        return constraints;
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

    public static CaseMetaData extractMetaData(
            LinkedList<String> constraints,
            LinkedList<SymbolTag> symbolsUsed,
            String labelStr,
            int lineNumber
    ) {
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

        String[] conArr = sieved.stream().toArray(String[]::new);
        Constraints finalConstr = new Constraints(conArr, specialVars, specialVals);
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

                if ( rule != null && CaseAnalyzer.ACTIVE_BLUEPRINTS ) {
                    varVals.add(rule);
                    length += rule[0].length;
                } else sieved.add(con);
            } catch ( ParseException | RuntimeException pe ){
                LOG.warn("Cannot parse constraint of line " + lineNumber + ". Reason: " + pe.getMessage());
            }
        }
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
            String[][] innerRule = analyzer.checkForBlueprintRules(c);
            if ( innerRule != null ) innerRulesList.addLast(innerRule);
            else innerSieved.add(c);
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
