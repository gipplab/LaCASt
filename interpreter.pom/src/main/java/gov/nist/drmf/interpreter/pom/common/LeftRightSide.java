package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.common.latex.Relations;

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

    public String getOriginal() {
        return orig;
    }

    public String getLHS() {
        return one;
    }

    public String getRHS() {
        return two;
    }

    public void addCases(Collection<String> collection) {
        collection.add(one);
        collection.add(two);
    }
}
