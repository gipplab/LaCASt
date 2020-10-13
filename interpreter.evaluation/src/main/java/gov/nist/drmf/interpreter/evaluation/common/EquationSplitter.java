package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class EquationSplitter {
    private static final Logger LOG = LogManager.getLogger(EquationSplitter.class.getName());

    private final static Pattern RELATION_MATCHER = Pattern.compile(
            "\\s*(?:([<>=][<>=]?)|(\\\\[ngl]eq?)([^a-zA-Z])|([()\\[\\]{}|]))\\s*"
    );

    private LinkedList<Brackets> bracketStack = new LinkedList<>();
    private LinkedList<String> parts = new LinkedList<>();
    private LinkedList<Relations> rels = new LinkedList<>();
    private StringBuffer bf = new StringBuffer();

    public EquationSplitter() {}

    private void reset() {
        bracketStack.clear();
        parts.clear();
        rels.clear();
        bf = new StringBuffer();
    }

    public LinkedList<Case> split(String latex, CaseMetaData metaData) {
        buildPartsAndRels(latex);
        return listCases(parts, rels, metaData);
    }

    public Collection<String> constraintSplitter(String latex) {
        buildPartsAndRels(latex);
        List<String> constraints = new LinkedList<>();

        while ( !rels.isEmpty() ) {
            String left = parts.removeFirst();
            String right = parts.getFirst();
            Relations rel = rels.removeFirst();

            String[] leftElements = left.split(",");
            String[] rightElements = right.split(",");
            boolean numberChain = true;
            if ( rightElements.length > 1 ) {
                if ( rightElements.length == 2 && !rels.isEmpty() ) numberChain = false;
                else {
                    for ( String r : rightElements ) {
                        numberChain &= r.matches("\\s*(-?\\s*[0-9.]+|\\\\[lc]?dots)\\s*");
                    }
                }
            } else numberChain = false;

            if ( numberChain ) {
                LOG.error("Unable to analyze constraint (list of number after blueprints): " + latex);
                return constraints;
            }

            for ( String el : leftElements ) {
                String e = el + " " + rel.getTexSymbol() + " " + rightElements[0];
                splitPMConstraints(e, constraints);
            }

            if ( rightElements.length > 1 ) {
                parts.removeFirst();
                parts.addFirst( right.substring(right.indexOf(",")+1) );
            }
        }

        return constraints;
    }

    private static void splitPMConstraints(String latex, Collection<String> constraints) {
        LeftRightSide lrs = new LeftRightSide(latex);
        if ( lrs.wasSplitted() ) lrs.addCases(constraints);
        else constraints.add(latex);
    }

    private void buildPartsAndRels(String latex) {
        reset();
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
