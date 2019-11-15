package gov.nist.drmf.interpreter.semantic;

import java.util.ArrayList;

/**
 * Java abstract class representing generic LaTeX mode (either text or math)
 */
public abstract class LaTeXMode {

    /**
     * Starting index of LaTeXMode
     */
    private int startIndex;

    /**
     * Ending index of LaTeXMode
     */
    private int endIndex;

    /**
     * Sections of the opposite mode contained inside this mode
     */
    private ArrayList<LaTeXMode> sections;

    /**
     * Initializes a LaTeXMode object given the start index of LaTeX segment
     * @param start
     */
    public LaTeXMode(int start) {
        startIndex = start;
        sections = new ArrayList<>();
    }

    /**
     * Adds a section of the opposite mode to the sections field
     * @param section
     */
    public void addSection(LaTeXMode section) {
        sections.add(section);
    }

    /**
     * Returns the start index
     * @return
     */
    public int getStart() {
        return startIndex;
    }

    /**
     * Sets the end index
     * @param end
     */
    public void setEnd(int end) {
        endIndex = end;
    }

    /**
     * Returns the end index
     * @return
     */
    public int getEnd() {
        return endIndex;
    }

    /**
     * Performs replacements in contained sections and then aggregates these new sections to form a fully replaced section
     * @param content
     * @return
     */
    public String makeReplacements(String content) {
        int offset = 0;
        for (LaTeXMode section : sections) {
            int start = section.getStart() + offset;
            int end = section.getEnd() + offset + 1;
            String temp = section.makeReplacements(content.substring(start,end));
            content = content.substring(0,start) + temp + content.substring(end);
            offset += temp.length() - end + start;
        }
        return content;
    }

}
