package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.evaluation.constraints.Constraints;
import gov.nist.drmf.interpreter.evaluation.constraints.MLPConstraintAnalyzer;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CaseAnalyzer {
    private static final Logger LOG = LogManager.getLogger(CaseAnalyzer.class.getName());

    private static final Pattern META_INFO_PATTERN = Pattern.compile(
            "\\\\constraint\\{(.*?)} |" +
            "\\\\url\\{(.*?)}|" +
            "\\\\symbolDefined\\[(.*?)]\\{([a-zA-Z0-9.]*?)}|" +
            "\\\\symbolUsed\\[(.*?)]\\{([a-zA-Z0-9.]*?)}|" +
            "\\\\source|\\\\authorproof|\\\\keyphrase|\\\\cite|\\\\comments"
    );

    private static final String EOL = "<EOL>";

    private static final Pattern END_OF_MATH_MATCHER = Pattern.compile(
            "^(.*?)[\\\\,;.\\s]*"+EOL+".*$"
    );

//    private static final Pattern CONSTRAINT_SPLITTER_PATTERN = Pattern.compile(
//            "\\$.+?\\$"
//    );

    private static final int CONSTRAINT_GRP = 1;
    private static final int URL_GRP = 2;
    private static final int SYMB_DEF_GRP_SYMB = 3;
    private static final int SYMB_DEF_GRP_ID = 4;
    private static final int SYMB_USED_GRP_ID = 5;
    private static final int SYMB_USED_GRP_SYMB = 6;

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
    public static LinkedList<Case> analyzeLine(String line, int lineNumber, SymbolDefinedLibrary symbDefLib ){
        // matching group
        Matcher metaDataMatcher = META_INFO_PATTERN.matcher(line);
        StringBuffer mathSB = new StringBuffer();

        String link = null;
        LinkedList<String> constraints = new LinkedList();
        LinkedList<SymbolTag> symbolsUsed = new LinkedList<>();

        String symbolDefSymb = null;
        String symbolDefID = null;

        // extract all information
        while( metaDataMatcher.find() ) {
            if ( metaDataMatcher.group(CONSTRAINT_GRP) != null ) {
                constraints.add(metaDataMatcher.group(CONSTRAINT_GRP));
            } else if ( metaDataMatcher.group(URL_GRP) != null ) {
                link = metaDataMatcher.group(URL_GRP);
            } else if ( metaDataMatcher.group(SYMB_DEF_GRP_ID) != null ) {
                symbolDefSymb = metaDataMatcher.group(SYMB_DEF_GRP_SYMB);
                symbolDefID = metaDataMatcher.group(SYMB_DEF_GRP_ID);
                if ( symbolDefSymb.contains("\\NVar") ) {
                    LOG.warn("Found potential definition of macros. Ignore this definition and treat it as normal test case.");
                    symbolDefSymb = null;
                    symbolDefID = null;
                }
            } else if ( metaDataMatcher.group(SYMB_USED_GRP_ID) != null ) {
                String id = metaDataMatcher.group(SYMB_USED_GRP_ID);
                String symb = metaDataMatcher.group(SYMB_USED_GRP_SYMB);

                if ( !symb.contains("\\NVar") ){
                    SymbolTag used = new SymbolTag(id, symb);
                    symbolsUsed.add(used);
                }
            }
            metaDataMatcher.appendReplacement(mathSB, EOL);
        }
        metaDataMatcher.appendTail(mathSB);

        // clip meta data from test expression
        Matcher mathMatcher = END_OF_MATH_MATCHER.matcher(mathSB.toString());
        String eq = "";
        if ( !mathMatcher.matches() ){
            eq = mathSB.toString();
        } else eq = mathMatcher.group(1);

        CaseMetaData metaData = extractMetaData(constraints, symbolsUsed, link, lineNumber);

        if ( symbolDefID != null && !symbolDefSymb.contains("\\NVar") ) {
            // TODO you know what, fuck \zeta(z)
            if ( symbolDefSymb.equals("\\zeta(z)") ) symbolDefSymb = "\\zeta";

            LinkedList<Case> caseList = equationSplitter(eq, metaData);
            if ( caseList == null || caseList.isEmpty() ) return null;

            Case c = caseList.get(0);
            if ( c.getLHS().equals( symbolDefSymb ) ) {
                LOG.info("Store line definition: " + symbolDefSymb + " is defined as " + c.getRHS());
                symbDefLib.add(
                        symbolDefID,
                        symbolDefSymb,
                        c.getRHS(),
                        metaData
                );
            } else {
                LOG.warn("LHS does not match defined symbol:" + c.getLHS() + " vs " + symbolDefSymb);
            }

            return null;
        }

//        if ( eq.contains("\\pm") || eq.contains("\\mp") ) {
//            String one = eq.replaceAll("\\\\pm", "+");
//            one = one.replaceAll("\\\\mp", "-");
//            String two = eq.replaceAll("\\\\pm", "-");
//            two = two.replaceAll("\\\\mp", "+");
//            LinkedList<Case> firstCases = equationSplitter(one, metaData);
//            LinkedList<Case> secondCases = equationSplitter(two, metaData);
//            firstCases.addAll(secondCases);
//            return firstCases;
//        } else {
            return equationSplitter(eq, metaData);
//        }
    }

    private static Pattern RELATION_MATCHER = Pattern.compile(
            "\\s*(?:([<>=][<>=]?)|(\\\\[ngl]eq?)([^a-zA-Z])|([()\\[\\]{}|]))\\s*"
    );

    public static LinkedList<Case> equationSplitter( String latex, CaseMetaData metaData ) {
        LinkedList<Brackets> bracketStack = new LinkedList<>();
        LinkedList<String> parts = new LinkedList<>();
        LinkedList<Relations> rels = new LinkedList<>();

        StringBuffer bf = new StringBuffer();

        Matcher relM = RELATION_MATCHER.matcher(latex);
        String cacheReplacement = null;

        while ( relM.find() ) {
            if ( relM.group(1) != null || relM.group(2) != null ) {
                String relStr = relM.group(1) != null ? relM.group(1) : relM.group(2);
                Relations rel = getRelation(relStr);
                if ( rel == null ) {
                    return null;
                }

                if ( bracketStack.isEmpty() ){
                    relM.appendReplacement(bf, "");
                    String p = bf.toString();
                    if ( cacheReplacement != null ) {
                        p = cacheReplacement + p;
                        cacheReplacement = null;
                    }
                    p = p.trim();
                    parts.addLast(p);
                    rels.addLast(rel);
                    bf = new StringBuffer(); // reset buffer
                    if ( relM.group(2) != null ) {
                        cacheReplacement = relM.group(3);
                    }
                }
            } else if ( relM.group(4) != null ) {
                String relStr = relM.group(4);
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
        String lastPart = bf.toString();
        if ( cacheReplacement != null ) {
            lastPart = cacheReplacement + lastPart;
            cacheReplacement = null;
        }
        parts.add( lastPart.trim() );

        // well, tests working better without this setting o.O
        boolean allequals = false;
        for ( Relations r : rels ) {
            if ( !r.equals(Relations.EQUAL) ){
                allequals = false;
                break;
            }
        }

        LinkedList<Case> cases = new LinkedList<>();

        if ( allequals ) {
            String left = parts.removeFirst();
            while ( !parts.isEmpty() ) {
                String right = parts.removeFirst();
                splitPMs(left, right, Relations.EQUAL, metaData, cases);
            }
        } else {
            while ( !rels.isEmpty() ) {
                Relations r = rels.removeFirst();
                String left = parts.removeFirst();
                String right = parts.get(0);
                splitPMs(left, right, r, metaData, cases);
            }
        }

        return cases;
    }

    private static LinkedList<Case> splitPMs( String left, String right, Relations rel, CaseMetaData metaData, LinkedList<Case> cases ) {
        String lone = null, ltwo = null, rone = null, rtwo = null;
        if ( left.contains("\\pm") || left.contains("\\mp") ) {
            lone = left.replaceAll("\\\\pm", "+");
            lone = lone.replaceAll("\\\\mp", "-");
            ltwo = left.replaceAll("\\\\pm", "-");
            ltwo = ltwo.replaceAll("\\\\mp", "+");
        }

        if ( right.contains("\\pm") || right.contains("\\mp") ) {
            rone = right.replaceAll("\\\\pm", "+");
            rone = rone.replaceAll("\\\\mp", "-");
            rtwo = right.replaceAll("\\\\pm", "-");
            rtwo = rtwo.replaceAll("\\\\mp", "+");
        }

        if ( lone != null && rone != null ) {
            cases.add(new Case(lone, rone, rel, metaData));
            cases.add(new Case(ltwo, rtwo, rel, metaData));
        } else if ( lone == null && rone != null ) {
            cases.add(new Case(left, rone, rel, metaData));
            cases.add(new Case(left, rtwo, rel, metaData));
        } else if ( lone != null && rone == null ) {
            cases.add(new Case(lone, right, rel, metaData));
            cases.add(new Case(ltwo, right, rel, metaData));
        } else {
            cases.add(new Case(left, right, rel, metaData));
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

    private static CaseMetaData extractMetaData(
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

        // second, build list of constraints
//        LinkedList<String> cons = new LinkedList<>();
//        for ( String con : constraints ) {
//            Matcher consMatcher = CONSTRAINT_SPLITTER_PATTERN.matcher(con);
//
//            while( consMatcher.find() ){
//                cons.add(consMatcher.group());
//            }
//        }

        LinkedList<String> sieved = new LinkedList<>();
        LinkedList<String[][]> varVals = new LinkedList<>();
        int length = 0;

        while ( !constraints.isEmpty() ){
            String con = constraints.removeFirst();
            try {
                String[][] rule = analyzer.checkForBlueprintRules(con);
                // some constraints are buggy... consider \nu\geq 1,x\in\Reals
                if ( rule == null ) {
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
                        for ( String c : innerSieved ) {
                            sieved.add(c);
                        }
                        continue;
                    }
                }

                if ( rule != null && ACTIVE_BLUEPRINTS ) {
                    varVals.add(rule);
                    length += rule[0].length;
                }
                else sieved.add(con);
            } catch ( ParseException | RuntimeException pe ){
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
        Constraints finalConstr = new Constraints(conArr, specialVars, specialVals);
        return new CaseMetaData(lineNumber, label, finalConstr, symbolsUsed);
    }
}
