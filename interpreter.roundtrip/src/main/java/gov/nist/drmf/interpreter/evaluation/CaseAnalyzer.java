package gov.nist.drmf.interpreter.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CaseAnalyzer {

    private static final Logger LOG = LogManager.getLogger(CaseAnalyzer.class.getName());

    private static final String LABEL = "\\\\label";
    private static final String CONSTRAINT = "\\\\constraint";

    private static final Pattern CONST_LABEL_PATTERN = Pattern.compile("[\\s{,;'\\$]*(.*)");

    /**
     * Creates a case element from a line that contains a test case.
     * Tries to find constraints, labels and split the test case into left- and right-hand site.
     *
     * @param line the entire line with a test case, constraints and so on
     * @param lineNumber the current line number of this test case
     * @param linker a linker object to transform the label in the line to a hyperlink to DLMF
     * @return Case object
     */
    public static Case analyzeLine( String line, int lineNumber, DLMFLinker linker ){
        String eq, constraint = null, label = null;
        String[] constSplit = line.split( CONSTRAINT );
        String[] labelSplit = line.split( LABEL );

        // there is a constraint
        if ( constSplit.length > 1 ){
            labelSplit = constSplit[1].split( LABEL );
            eq = constSplit[0];
            constraint = labelSplit[0];
            if ( labelSplit.length > 1 )
                label = labelSplit[1];
        } else if ( labelSplit.length > 1 ){
            eq = labelSplit[0];
            label = labelSplit[1];
        } else {
            eq = line;
        }

        String ending_deletion = "[\\s,;.]*$";

        if ( constraint != null ){
            Matcher m = CONST_LABEL_PATTERN.matcher(constraint);
            if ( m.matches() ){
                constraint = m.group(1);
                constraint = constraint.replaceAll("[.$]*","");
                constraint = constraint.replaceAll("[}\\s,;.$]*$", "");
            }
        }

        if ( label != null ){
            Matcher m = CONST_LABEL_PATTERN.matcher(label);
            if ( m.matches() ){
                label = m.group(1);
                label = label.replaceAll("[{}$,;]","");
                label = label.replaceAll(ending_deletion, "");
            }
        }

        eq = eq.replaceAll(ending_deletion,"");
        String[] lrHS = eq.split("=");

        if ( lrHS.length == 2 ){
            return new Case( lrHS[0], lrHS[1], Relations.EQUAL, constraint, linker.getLink(label), lineNumber );
        } else {
            String[] f = null;
            Relations rel = null;
            if ( eq.contains("<") ){
                f = eq.split( "<" );
                rel = Relations.LESS_THAN;
            } else if ( eq.contains("\\leq") ){
                f = eq.split( "\\\\leq" );
                rel = Relations.LESS_EQ_THAN;
            } else if ( eq.contains("\\le") ){
                f = eq.split( "\\\\le" );
                rel = Relations.LESS_EQ_THAN;
            } else if ( eq.contains( ">" ) ){
                f = eq.split( ">" );
                rel = Relations.GREATER_THAN;
            } else if ( eq.contains( "\\geq" ) ){
                f = eq.split( "\\\\geq" );
                rel = Relations.GREATER_EQ_THAN;
            } else if ( eq.contains("\\ge") ){
                f = eq.split( "\\\\geq" );
                rel = Relations.GREATER_EQ_THAN;
            } else if ( eq.contains( "\\neq" ) ){
                f = eq.split( "\\\\neq" );
                rel = Relations.UNEQUAL;
            }

            if ( f == null || f.length < 2 ){
                LOG.error("Line cannot be split into LHS and RHS and therefore will be skipped: " + lineNumber );
                return null;
            }

            return new Case(f[0], f[1], rel, constraint, linker.getLink(label), lineNumber);
        }
    }

}
