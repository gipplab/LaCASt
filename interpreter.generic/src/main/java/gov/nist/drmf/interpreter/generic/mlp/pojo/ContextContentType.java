package gov.nist.drmf.interpreter.generic.mlp.pojo;

import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ContextContentType {
    WIKITEXT(
            "(<mediawiki.*?>|<page>)|" +
                    "\\{\\{.*?}}"
    ),
    LATEX(
            "(\\\\documentclass(?:\\[.*?])?\\{.*?}|\\\\begin\\{document})|" +
                    "\\${1,2}.*?\\${1,2}|\\\\begin\\{equation}.*?\\\\end\\{equation}"
    ),
    INDETERMINATE(null);
    ;

    private Pattern hintPattern = null;

    ContextContentType(@Language("RegExp") String hintPatternString) {
        if ( hintPatternString != null )
            this.hintPattern = Pattern.compile(hintPatternString);
    }

    private int hints(String context) {
        if ( this.equals(INDETERMINATE) ) return -1;
        Matcher matcher = hintPattern.matcher(context);
        int hints = 0;
        while( matcher.find() ) {
            if ( matcher.group(1) != null ) return Integer.MAX_VALUE;
            hints++;
        }
        return hints;
    }

    public static ContextContentType guessContentType(String context) {
        return Arrays.stream(ContextContentType.values())
                .max(Comparator.comparingInt(a -> a.hints(context)))
                .orElse(INDETERMINATE);
    }
}
