package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.pom.common.CaseMetaData;
import gov.nist.drmf.interpreter.pom.common.LeftRightSide;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class CaseLeftRightSide extends LeftRightSide {

    public CaseLeftRightSide(String side) {
        super(side);
    }

    public List<Case> getCases(LeftRightSide opposite, Relations rel, CaseMetaData metaData) {
        List<Case> cases = new LinkedList<>();
        if ( wasSplitted() && opposite.wasSplitted() ) {
            cases.add(new Case(getLHS(), opposite.getLHS(), rel, metaData));
            cases.add(new Case(getRHS(), opposite.getRHS(), rel, metaData));
        } else if (wasSplitted()) {
            cases.add(new Case(getLHS(), opposite.getOriginal(), rel, metaData));
            cases.add(new Case(getRHS(), opposite.getOriginal(), rel, metaData));
        } else if (opposite.wasSplitted()) {
            cases.add(new Case(getOriginal(), opposite.getLHS(), rel, metaData));
            cases.add(new Case(getOriginal(), opposite.getRHS(), rel, metaData));
        } else {
            cases.add(new Case(getOriginal(), opposite.getOriginal(), rel, metaData));
        }
        return cases;
    }
}
