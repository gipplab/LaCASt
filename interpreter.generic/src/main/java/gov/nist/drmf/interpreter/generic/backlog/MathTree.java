package gov.nist.drmf.interpreter.generic.backlog;

/**
 * Java class that represents tree of embedded math modes and text modes
 */
public class MathTree {

    /**
     * Parses the math mode section beginning at the given start index and any contained text sections
     * @param latex
     * @param startIndex
     * @return
     * @throws InvalidLaTeXException
     */
    public static MathMode parseMath(String latex, int startIndex) throws InvalidLaTeXException {
        MathMode mathSection = new MathMode(startIndex);
        mathSection.setDelim(MathModeUtils.firstDelim(latex, true));
        int i = mathSection.getDelim().length();
        String end = MathModeUtils.mathMode.get(mathSection.getDelim());
        while (i < latex.length()) {
            i += MathModeUtils.skipEscaped(latex.substring(i));
            String sub = latex.substring(i);
            if (MathModeUtils.doesExit(sub)) {
                TextMode textSection = parseText(sub, i);
                mathSection.addSection(textSection);
                i += textSection.getEnd() - textSection.getStart();
            }
            if (sub.startsWith(end)) {
                mathSection.setEnd(mathSection.getStart() + i + end.length() - 1);
                return mathSection;
            }
            i++;
        }
        throw new InvalidLaTeXException("Unterminated math sequence");
    }

    /**
     * Parses the text mode section beginning at the given start index and any contained math sections
     * @param latex
     * @param startIndex
     * @return
     * @throws InvalidLaTeXException
     */
    public static TextMode parseText(String latex, int startIndex) throws InvalidLaTeXException {
        TextMode textSection = new TextMode(startIndex);
        String delim = MathModeUtils.firstDelim(latex, false);
        if (!latex.startsWith(delim)) {
            delim = "";
        }
        int level = 0;
        int i = delim.length();
        while (i < latex.length()) {
            i += MathModeUtils.skipEscaped(latex.substring(i));
            if (i >= latex.length()) {
                break;
            }
            String sub = latex.substring(i);
            if (MathModeUtils.doesEnter(sub)) {
                MathMode mathSection = parseMath(sub,i);
                textSection.addSection(mathSection);
                i += mathSection.getEnd() - mathSection.getStart();
            } else if (sub.charAt(0) == '{') {
                level++;
            } else if (sub.charAt(0) == '}') {
                if (level == 0 && !delim.isEmpty()) {
                    textSection.setEnd(startIndex + i);
                    return textSection;
                }
                level--;
            }
            i++;
        }
        textSection.setEnd(startIndex + i - 1);
        return textSection;
    }

    /**
     * Performs macro replacements on a given LaTeX string
     * @param latex
     * @return
     */
    public static String replaceText(String latex) {
        try {
            return parseText(latex, 0).makeReplacements(latex);
        } catch (InvalidLaTeXException e) {
            e.printStackTrace();
        }
        return null;
    }
}