package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.grammar.Brackets;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class EquationSplitter {

    private final static Pattern RELATION_MATCHER = Pattern.compile(
            "\\s*(?:([<>=][<>=]?)|(\\\\[ngl]eq?)([^a-zA-Z])|([()\\[\\]{}|]))\\s*"
    );

    private LinkedList<Brackets> bracketStack = new LinkedList<>();
    private LinkedList<String> parts = new LinkedList<>();
    private LinkedList<Relations> rels = new LinkedList<>();
    private StringBuffer bf = new StringBuffer();

    private final CaseMetaData metaData;

    public EquationSplitter(CaseMetaData metaData) {
        this.metaData = metaData;
    }

    public LinkedList<Case> split(String latex) {
        Matcher relM = RELATION_MATCHER.matcher(latex);
        String cacheReplacement = null;

        while ( relM.find() ) {
            cacheReplacement = handleNextMatch(relM, cacheReplacement);
        }

        relM.appendTail(bf);
        String lastPart = bf.toString();
        if ( cacheReplacement != null ) {
            lastPart = cacheReplacement + lastPart;
        }
        parts.add( lastPart.trim() );

        return listCases(parts, rels, metaData);
    }

    private String handleNextMatch(Matcher relM, String cacheReplacement) {
        if ( relM.group(1) != null || relM.group(2) != null ) {
            String relStr = relM.group(1) != null ? relM.group(1) : relM.group(2);
            Relations rel = CaseAnalyzer.getRelation(relStr);
            if ( rel == null ) {
                return null;
            }
            cacheReplacement = handleRelationCase(relM, cacheReplacement, rel);
        } else if ( relM.group(4) != null ) {
            checkBracket(relM);
        }
        return cacheReplacement;
    }

    private String handleRelationCase(Matcher relM, String cacheReplacement, Relations rel) {
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
        return cacheReplacement;
    }

    private void checkBracket(Matcher relM) {
        String relStr = relM.group(4);
        Brackets b = Brackets.getBracket(relStr);
        if ( !bracketStack.isEmpty() ){
            Brackets last = bracketStack.getLast();
            if ( last.opened && last.counterpart.equals(b.symbol) ) {
                bracketStack.removeLast();
            } else bracketStack.addLast(b);
        } else bracketStack.addLast(b);
    }

    private static boolean allEquals(LinkedList<Relations> rels) {
        // well, tests working better without this setting o.O
        boolean allequals = true;
        for ( Relations r : rels ) {
            if ( !r.equals(Relations.EQUAL) ){
                allequals = false;
                break;
            }
        }
        return allequals;
    }

    private static LinkedList<Case> listCases(
            LinkedList<String> parts,
            LinkedList<Relations> rels,
            CaseMetaData metaData
    ) {
        LinkedList<Case> cases = new LinkedList<>();

        // TODO tests are working better without the option of "allEquals"
//        if ( allEquals(rels) ) {
//            String left = parts.removeFirst();
//            while ( !parts.isEmpty() ) {
//                String right = parts.removeFirst();
//                LeftRightSide l = new LeftRightSide(left);
//                LeftRightSide r = new LeftRightSide(right);
//                cases.addAll(l.getCases(r, Relations.EQUAL, metaData));
//            }
//        } else {
            while ( !rels.isEmpty() ) {
                Relations rel = rels.removeFirst();
                LeftRightSide l = new LeftRightSide(parts.removeFirst());
                LeftRightSide r = new LeftRightSide(parts.get(0));
                cases.addAll(l.getCases(r, rel, metaData));
//            }
        }

        return cases;
    }

}
