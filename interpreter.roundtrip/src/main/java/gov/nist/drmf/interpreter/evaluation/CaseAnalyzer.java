package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.constraints.Constraints;
import gov.nist.drmf.interpreter.constraints.MLPConstraintAnalyzer;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CaseAnalyzer {

    private static final Logger LOG = LogManager.getLogger(CaseAnalyzer.class.getName());

    private static final Pattern CONSTRAINT_LABEL_PATTERN = Pattern.compile(
            "[\\s,;.]*(\\\\constraint\\{.*?}\\s?)?(\\\\label\\{.*})*\\s*$"
    );

    private static final Pattern CONSTRAINT_SPLITTER_PATTERN = Pattern.compile(
            "\\$.+?\\$"
    );

    private static final int CONSTRAINT_GRP = 1;
    private static final int LABEL_GRP = 2;

    public static boolean ACTIVE_BLUEPRINTS = true;

    private static MLPConstraintAnalyzer analyzer = MLPConstraintAnalyzer.getAnalyzerInstance();

    /**
     * Creates a case element from a line that contains a test case.
     * Tries to find constraints, labels and split the test case into left- and right-hand site.
     *
     * @param line the entire line with a test case, constraints and so on
     * @param lineNumber the current line number of this test case
     * @return Case object
     */
    public static Case analyzeLine( String line, int lineNumber ){
        StringBuffer rawLineBuffer = new StringBuffer();

        Matcher metaDataMatcher = CONSTRAINT_LABEL_PATTERN.matcher(line);

        if ( !metaDataMatcher.find() ){
            throw new IllegalArgumentException("Cannot analyze line! " + line);
        }

        String constraint = metaDataMatcher.group(CONSTRAINT_GRP);
        String label = metaDataMatcher.group(LABEL_GRP);

        metaDataMatcher.appendReplacement(rawLineBuffer, "");
        metaDataMatcher.appendTail(rawLineBuffer);

        String eq = rawLineBuffer.toString();

//        try {
//            NumericalEvaluator.getTranslator().translateFromLaTeXToMapleClean(eq);
//            System.out.println(lineNumber + " SUCCESS");
//        } catch ( Exception e ){
//            System.out.println(lineNumber + " ERROR");
//        }
//        return null;

        CaseMetaData metaData = extractMetaData(constraint, label, lineNumber);

        String[] lrHS = eq.split("=");

        if ( lrHS.length == 2 ){
            return new Case( lrHS[0], lrHS[1], Relations.EQUAL, metaData );
        } else {
            String[] f = null;
            Relations rel = null;
            if ( eq.contains("<") ){
                f = eq.split( "<" );
                rel = Relations.LESS_THAN;
            } else if ( eq.matches(".*\\\\leq[^a-zA-Z].*") ){
                f = eq.split( "\\\\leq[^a-zA-Z]" );
                rel = Relations.LESS_EQ_THAN;
            } else if ( eq.matches(".*\\\\le[^a-zA-Z].*") ){
                f = eq.split( "\\\\le[^a-zA-Z]" );
                rel = Relations.LESS_EQ_THAN;
            } else if ( eq.contains( ">" ) ){
                f = eq.split( ">" );
                rel = Relations.GREATER_THAN;
            } else if ( eq.matches( ".*\\\\geq[^a-zA-Z].*" ) ){
                f = eq.split( "\\\\geq[^a-zA-Z]" );
                rel = Relations.GREATER_EQ_THAN;
            } else if ( eq.matches(".*\\\\ge[^a-zA-Z].*") ){
                f = eq.split( "\\\\ge[^a-zA-Z]" );
                rel = Relations.GREATER_EQ_THAN;
            } else if ( eq.matches( ".*\\\\neq[^a-zA-Z].*" ) ){
                f = eq.split( "\\\\neq[^a-zA-Z]" );
                rel = Relations.UNEQUAL;
            } else if ( eq.matches( ".*\\\\ne[^a-zA-Z].*" ) ){
                f = eq.split( "\\\\ne[^a-zA-Z]" );
                rel = Relations.UNEQUAL;
            }

            if ( f == null || f.length < 2 ){
                LOG.warn("Line cannot be split into LHS and RHS and therefore will be skipped: " + lineNumber );
                return null;
            }

            return new Case(f[0], f[1], rel, metaData);
        }
    }

    private static CaseMetaData extractMetaData(String constraintStr, String labelStr, int lineNumber) {
        // first, create label
        Label label = null;
        if ( labelStr != null ){
            labelStr = labelStr.substring("\\\\label{".length()-1, labelStr.length()-1);
            label = new Label(labelStr);
            System.out.println(lineNumber + ": " + label.getHyperlink());
        }

        if ( constraintStr == null )
            return new CaseMetaData(lineNumber, label, null);

        // second, build list of constraints
        LinkedList<String> cons = new LinkedList<>();
        Matcher consMatcher = CONSTRAINT_SPLITTER_PATTERN.matcher(constraintStr);

        while( consMatcher.find() ){
            cons.add(consMatcher.group());
        }

        LinkedList<String> sieved = new LinkedList<>();
        LinkedList<String[][]> varVals = new LinkedList<>();
        int length = 0;

        while ( !cons.isEmpty() ){
            String con = cons.removeFirst();
            try {
                String[][] rule = analyzer.checkForBlueprintRules(con);
                if ( rule != null && ACTIVE_BLUEPRINTS ) {
                    varVals.add(rule);
                    length += rule[0].length;
                }
                else sieved.add(con);
            } catch ( ParseException pe ){
                LOG.warn("Cannot parse constraint of line " + lineNumber + ". Reason: " + pe.getMessage());
            }
        }

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
        Constraints constraints = new Constraints(conArr, specialVars, specialVals);
        return new CaseMetaData(lineNumber, label, constraints);
    }
}
