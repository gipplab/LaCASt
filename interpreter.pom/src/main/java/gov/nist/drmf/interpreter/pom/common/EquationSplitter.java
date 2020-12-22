package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;

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

    private final LinkedList<Brackets> bracketStack = new LinkedList<>();
    private final LinkedList<String> parts = new LinkedList<>();
    private final LinkedList<Relations> rels = new LinkedList<>();
    private StringBuffer bf = new StringBuffer();

    private void reset() {
        bracketStack.clear();
        parts.clear();
        rels.clear();
        bf = new StringBuffer();
    }

    /**
     * Analyzes the given latex string and internally splits the equation into the parts.
     * You can access the parts and relation symbols in between via {@link #getParts()} and
     * {@link #getRelations()}. The length of relations is always one shorter than parts.
     *
     * For example, after analyzing the line
     *  x = y < z = m
     * would generate the parts {@link #getParts()}
     *  [x, y, z, m]
     * and the relations {@link #getRelations()}
     *  [=, <, =]
     *
     * @param latex the latex that may contain an equation or relation or contains multiple equation/relations
     */
    public void analyzeTex(String latex) {
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

    public LinkedList<String> getParts() {
        return parts;
    }

    public LinkedList<Relations> getRelations() {
        return rels;
    }

    private String handleNextMatch(Matcher relM, String cacheReplacement) {
        if ( relM.group(1) != null || relM.group(2) != null ) {
            String relStr = relM.group(1) != null ? relM.group(1) : relM.group(2);
            Relations rel = Relations.getRelation(relStr);
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
}
