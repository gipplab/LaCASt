package gov.nist.drmf.interpreter.semantic;

/**
 * Created by jrp4 on 11/29/16.
 */
public class MathTree {

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

    public static String replaceText(String latex) {
        try {
            return parseText(latex, 0).makeReplacements(latex);
        } catch (InvalidLaTeXException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(replaceText("potato $\\pochhammer{\\frac{7}{2}}{\\sqrt{\\CatalansConstant}}$ potato")); //for testing
    }
}