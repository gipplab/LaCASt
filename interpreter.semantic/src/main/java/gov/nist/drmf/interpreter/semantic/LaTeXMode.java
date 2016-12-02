package gov.nist.drmf.interpreter.semantic;

import java.util.ArrayList;

/**
 * Created by jrp4 on 11/29/16.
 */
public abstract class LaTeXMode {

    private int startIndex;
    private int endIndex;
    private ArrayList<LaTeXMode> sections;

    public LaTeXMode(int start) {
        startIndex = start;
        sections = new ArrayList<LaTeXMode>();
    }

    public void addSection(LaTeXMode section) {
        sections.add(section);
    }

    public int getStart() {
        return startIndex;
    }

    public void setEnd(int end) {
        endIndex = end;
    }

    public int getEnd() {
        return endIndex;
    }

    public abstract String makeReplacements(String content);

}
