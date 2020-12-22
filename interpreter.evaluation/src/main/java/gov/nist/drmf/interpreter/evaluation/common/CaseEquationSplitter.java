package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.pom.common.EquationSplitter;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
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
public class CaseEquationSplitter {
    private static final Logger LOG = LogManager.getLogger(CaseEquationSplitter.class.getName());

    private final static Pattern RELATION_MATCHER = Pattern.compile(
            "\\s*(?:([<>=][<>=]?)|(\\\\[ngl]eq?)([^a-zA-Z])|([()\\[\\]{}|]))\\s*"
    );

    private final EquationSplitter equationSplitter;

    public CaseEquationSplitter() {
        this.equationSplitter = new EquationSplitter();
    }

    public LinkedList<Case> split(String latex, CaseMetaData metaData) {
        this.equationSplitter.analyzeTex(latex);
        return listCases(equationSplitter.getParts(), equationSplitter.getRelations(), metaData);
    }

    public Collection<String> constraintSplitter(String latex) {
        this.equationSplitter.analyzeTex(latex);
        List<String> constraints = new LinkedList<>();

        while ( !equationSplitter.getRelations().isEmpty() ) {
            String left = equationSplitter.getParts().removeFirst();
            String right = equationSplitter.getParts().getFirst();
            Relations rel = equationSplitter.getRelations().removeFirst();

            String[] leftElements = left.split(",");
            String[] rightElements = right.split(",");
            boolean numberChain = true;
            if ( rightElements.length > 1 ) {
                if ( rightElements.length == 2 && !equationSplitter.getRelations().isEmpty() ) numberChain = false;
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
                equationSplitter.getParts().removeFirst();
                equationSplitter.getParts().addFirst( right.substring(right.indexOf(",")+1) );
            }
        }

        return constraints;
    }

    private static void splitPMConstraints(String latex, Collection<String> constraints) {
        LeftRightSide lrs = new LeftRightSide(latex);
        if ( lrs.wasSplitted() ) lrs.addCases(constraints);
        else constraints.add(latex);
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
