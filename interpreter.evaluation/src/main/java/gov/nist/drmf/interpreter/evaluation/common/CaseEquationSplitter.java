package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.pom.common.CaseMetaData;
import gov.nist.drmf.interpreter.pom.common.EquationSplitter;

import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class CaseEquationSplitter extends EquationSplitter {

    public CaseEquationSplitter() {
        super();
    }

    public LinkedList<Case> split(String latex, CaseMetaData metaData) {
        analyzeTex(latex);
        return listCases(getParts(), getRelations(), metaData);
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
                CaseLeftRightSide l = new CaseLeftRightSide(parts.removeFirst());
                CaseLeftRightSide r = new CaseLeftRightSide(parts.get(0));
                cases.addAll(l.getCases(r, rel, metaData));
//            }
        }

        return cases;
    }
}
