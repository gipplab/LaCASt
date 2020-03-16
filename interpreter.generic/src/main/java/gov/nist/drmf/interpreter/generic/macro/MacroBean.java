package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class representing a Macro from a .sty file
 */
@JsonIgnoreProperties(
        ignoreUnknown = true,
        value = {"genericLaTeXParameters", "genericLaTeXArguments"}
)
public class MacroBean {
    public static final Pattern CLEAN_PATTERN = Pattern.compile(
            "\\\\m(?:left|right)"
    );

    public static final String VAR_PREFIX = "var";
    public static final String PAR_PREFIX = "par";

    /**
     * Unique identifier
     */
    @JsonProperty("macro")
    private final String name;

    /**
     * The pure generic LaTeX for parameters
     */
    private LinkedList<String> genericLaTeXParameters;

    /**
     * The pure generic LaTeX for arguments
     */
    private LinkedList<String> genericLaTeXArguments;

    /**
     * The possible list of all generic LaTeX presentations
     */
    @JsonProperty("tex")
    private LinkedList<String> genericLaTeX;

    /**
     * Textual description text
     */
    @JsonProperty("description")
    private String description;

    /**
     * Unique string of description
     */
    @JsonProperty("meaning")
    private String meaning;

    /**
     * OpenMath name
     */
    @JsonProperty("openMathID")
    private String openMathID;

    /**
     * The standard parameters
     */
    @JsonProperty("standardparameters")
    private LinkedList<String> standardParameters;

    /**
     * The standard arguments
     */
    @JsonProperty("standardarguments")
    private LinkedList<String> standardArguments;

    /**
     * @param name the macro
     */
    public MacroBean(String name) {
        this.name = name;
        this.genericLaTeXParameters = new LinkedList<>();
        this.genericLaTeXArguments = new LinkedList<>();
    }

    /**
     * Sets the generic latex expression with parameters. May include if/else clauses.
     * @param genericLaTeX the pure generic latex code
     */
    public void setGenericLaTeXParameters(String genericLaTeX) {
        // TODO handle \if and \else cases
        this.genericLaTeXParameters.add(genericLaTeX.replaceAll("#", PAR_PREFIX));
    }

    /**
     * Sets the generic latex list of arguments
     * @param numOfArgs number of arguments
     * @param argumentsList list of arguments
     */
    public void setGenericLaTeXArguments(int numOfArgs, String argumentsList) {
        if (numOfArgs == 0) {
            // just generate an empty list
            return;
        }

        if ( argumentsList != null && argumentsList.length() > 3 ) {
            argumentsList = argumentsList.substring(1, argumentsList.length()-1);
            String[] elements = argumentsList.split("]\\[");
            for ( String e : elements ) {
                e = e.replaceAll("#", VAR_PREFIX);
                e = cleanString(e);
                this.genericLaTeXArguments.add(e);
            }
        } else {
            // one @ always means \mleft( ... \mright) depending on number of args
            this.genericLaTeXArguments.add(generateArgumentList(numOfArgs));
        }
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public void setOpenMathID(String openMathID) {
        this.openMathID = openMathID;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStandardParameters(String para) {
        this.standardParameters = generateListOfArguments(para);
    }

    public void setStandardArguments(String args) {
        this.standardArguments = generateListOfArguments(args);
    }

    public String getName() {
        return name;
    }

    @JsonGetter("tex")
    public LinkedList<String> getGenericLatex() {
        genericLaTeX = new LinkedList<>();
        for ( String para : genericLaTeXParameters ) {
            if ( genericLaTeXArguments.isEmpty() ) genericLaTeX.add(para);
            else {
                for ( String args : genericLaTeXArguments ) {
                    genericLaTeX.add(para + args);
                }
            }
        }
        return genericLaTeX;
    }

    public String getDescription() {
        return description;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getOpenMathID() {
        return openMathID;
    }

    public LinkedList<String> getStandardParameters() {
        return standardParameters;
    }

    public LinkedList<String> getStandardArguments() {
        return standardArguments;
    }

    private static String cleanString(String arg) {
        StringBuilder sb = new StringBuilder();
        Matcher cleanMatcher = CLEAN_PATTERN.matcher(arg);
        while ( cleanMatcher.find() ) {
            cleanMatcher.appendReplacement(sb, "");
        }
        cleanMatcher.appendTail(sb);
        return sb.toString();
    }

    private static String generateArgumentList(int numberOfArgs) {
        if ( numberOfArgs == 1 ) return "(" + VAR_PREFIX+1 + ")";

        StringBuilder sb = new StringBuilder("(" + VAR_PREFIX + 1);
        for ( int i = 2; i <= numberOfArgs; i++ ) {
            sb.append(",").append(VAR_PREFIX).append(i);
        }
        sb.append(")");
        return sb.toString();
    }

    private static LinkedList<String> generateListOfArguments(String args) {
        if (args.length() < 1) {
            throw new IllegalArgumentException("Empty arguments list cannot be handled.");
        }

        if (args.matches("^\\{.*}$"))
            args = args.substring(1, args.length() - 1);

        String[] elements = args.split("}\\{");
        return new LinkedList<>(Arrays.asList(elements));
    }
}
