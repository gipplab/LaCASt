package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
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

    private static final Pattern META_INFO_PATTERN = Pattern.compile(
                    "\\\\constraint\\{(.*?)}|" +
                    "\\\\label\\{(.*?)}|" +
                    "\\\\ccode\\{(.*?)}|" +
                    "\\\\source|\\\\authorproof|\\\\keyphrase|\\\\cite"
    );

    private static final String EOL = "<EOL>";

    private static final Pattern END_OF_MATH_MATCHER = Pattern.compile(
            "^(.*?)[\\\\,;.\\s]*"+EOL+".*$"
    );

    private static final Pattern CONSTRAINT_SPLITTER_PATTERN = Pattern.compile(
            "\\$.+?\\$"
    );

    private static final int CONSTRAINT_GRP = 1;
    private static final int LABEL_GRP = 2;
    private static final int CODE_GRP = 3;

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
    public static LinkedList<Case> analyzeLine( String line, int lineNumber ){
        if ( line.contains("\\pm") || line.contains("\\mp") ) {
            String one = line.replaceAll("\\\\pm", "+");
            one = one.replaceAll("\\\\mp", "-");
            String two = line.replaceAll("\\\\pm", "-");
            two = two.replaceAll("\\\\mp", "+");
            LinkedList<Case> firstCases = analyzeLine(one, lineNumber);
            LinkedList<Case> secondCases = analyzeLine(two, lineNumber);
            firstCases.addAll(secondCases);
            return firstCases;
        }

        Matcher metaDataMatcher = META_INFO_PATTERN.matcher(line);
        StringBuffer mathSB = new StringBuffer();

        String constraint = null;
        String label = null;
        String code = null;

        while( metaDataMatcher.find() ) {
            if ( metaDataMatcher.group(CONSTRAINT_GRP) != null ) {
                constraint = metaDataMatcher.group(CONSTRAINT_GRP);
            } else if ( metaDataMatcher.group(LABEL_GRP) != null ) {
                label = metaDataMatcher.group(LABEL_GRP);
            } else if ( metaDataMatcher.group(CODE_GRP) != null ) {
                code = metaDataMatcher.group(CODE_GRP);
            }
            metaDataMatcher.appendReplacement(mathSB, EOL);
        }

        metaDataMatcher.appendTail(mathSB);

        Matcher mathMatcher = END_OF_MATH_MATCHER.matcher(mathSB.toString());
        if ( !mathMatcher.matches() )
            throw new IllegalArgumentException("Cannot analyze line! " + line);

        String eq = mathMatcher.group(1);
        CaseMetaData metaData = extractMetaData(constraint, label, code, lineNumber);

        LinkedList<Case> cases = equationSplitter(eq, metaData);
        return cases;
    }

    private static Pattern RELATION_MATCHER = Pattern.compile(
            "(\\s*[<>=][<>=]?|\\s*\\\\[ngl]eq?)[^a-zA-Z]\\s*|\\s*([()\\[\\]{}|])\\s*"
    );

    public static LinkedList<Case> equationSplitter( String latex, CaseMetaData metaData ) {
        LinkedList<Brackets> bracketStack = new LinkedList<>();
        LinkedList<String> parts = new LinkedList<>();
        LinkedList<Relations> rels = new LinkedList<>();

        StringBuffer bf = new StringBuffer();

        Matcher relM = RELATION_MATCHER.matcher(latex);
        while ( relM.find() ) {
            if ( relM.group(1) != null ) {
                String relStr = relM.group(1);
                Relations rel = getRelation(relStr);
                if ( rel == null ) {
                    return null;
                }

                if ( bracketStack.isEmpty() ){
                    relM.appendReplacement(bf, "");
                    String p = bf.toString();
                    p = p.trim();
                    parts.addLast(p);
                    rels.addLast(rel);
                    bf = new StringBuffer(); // reset buffer
                } else {
//                    relM.appendReplacement(bf, relStr);
                }
            } else if ( relM.group(2) != null ) {
                String relStr = relM.group(2);
                Brackets b = Brackets.getBracket(relStr);
                if ( !bracketStack.isEmpty() ){
                    Brackets last = bracketStack.getLast();
                    if ( last.opened && last.counterpart.equals(b.symbol) ) {
                        bracketStack.removeLast();
                    } else bracketStack.addLast(b);
                } else bracketStack.addLast(b);
            }
        }

        relM.appendTail(bf);
        parts.add( bf.toString() );

        LinkedList<Case> cases = new LinkedList<>();
        while ( !rels.isEmpty() ) {
            Relations r = rels.removeFirst();
            String left = parts.removeFirst();
            String right = parts.get(0);
            cases.add(new Case(left, right, r, metaData));
        }

        return cases;
    }

    public static Relations getRelation(String eq) {
        if ( eq.matches(".*(?:\\\\leq?|<=).*") ){
            return Relations.LESS_EQ_THAN;
        }
        else if ( eq.matches( ".*(?:\\\\geq?|=>).*" ) ){
            return Relations.GREATER_EQ_THAN;
        }
        else if ( eq.matches( ".*(?:\\\\neq?|<>).*" ) ){
            return Relations.UNEQUAL;
        }
        else if ( eq.contains("<") ){
            return Relations.LESS_THAN;
        }
        else if ( eq.contains( ">" ) ){
            return Relations.GREATER_THAN;
        }
        else if ( eq.contains( "=" ) ) {
            return Relations.EQUAL;
        }
        else return null;
    }

    private static CaseMetaData extractMetaData(String constraintStr, String labelStr, String codeStr, int lineNumber) {
        // first, create label
        Label label = null;
        if ( labelStr != null ){
//            labelStr = labelStr.substring("\\\\label{".length()-1, labelStr.length()-1);
            label = new Label(labelStr);
            System.out.println(lineNumber + ": " + label.getHyperlink());
        }

        if ( constraintStr == null )
            return new CaseMetaData(lineNumber, label, null, codeStr);

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
        return new CaseMetaData(lineNumber, label, constraints, codeStr);
    }
}
