package gov.nist.drmf.interpreter.evaluation.common;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class LeftRightSide {

    private String one, two;
    private String orig;

    private boolean splitted = false;

    public LeftRightSide(String side) {
        if ( side.contains("\\pm") || side.contains("\\mp") ) {
            one = side.replaceAll("\\\\pm", "+");
            one = one.replaceAll("\\\\mp", "-");
            two = side.replaceAll("\\\\pm", "-");
            two = two.replaceAll("\\\\mp", "+");
            splitted = true;
        }
        this.orig = side;
    }

    public boolean wasSplitted() {
        return splitted;
    }

    public void addCases(Collection<String> collection) {
        collection.add(one);
        collection.add(two);
    }

    public List<Case> getCases(LeftRightSide opposite, Relations rel, CaseMetaData metaData) {
        List<Case> cases = new LinkedList<>();
        if ( splitted && opposite.splitted ) {
            cases.add(new Case(one, opposite.one, rel, metaData));
            cases.add(new Case(two, opposite.two, rel, metaData));
        } else if (splitted) {
            cases.add(new Case(one, opposite.orig, rel, metaData));
            cases.add(new Case(two, opposite.orig, rel, metaData));
        } else if (opposite.splitted) {
            cases.add(new Case(orig, opposite.one, rel, metaData));
            cases.add(new Case(orig, opposite.two, rel, metaData));
        } else {
            cases.add(new Case(orig, opposite.orig, rel, metaData));
        }
        return cases;
    }
}
