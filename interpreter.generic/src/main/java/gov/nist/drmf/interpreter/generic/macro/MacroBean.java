package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
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
    public static final String OPTIONAL_PAR_PREFIX = "opPar";

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

    @JsonProperty("numberOfParameters")
    private int numberOfParameters;

    @JsonProperty("numberOfOptionalParameters")
    private int numberOfOptionalParameters = 0;

    @JsonProperty("numberOfArguments")
    private int numberOfArguments;

    /**
     * @param name the macro
     */
    public MacroBean(String name) {
        this.name = name;
        this.genericLaTeXParameters = new LinkedList<>();
        this.genericLaTeXArguments = new LinkedList<>();
    }

    /**
     * Sets the generic latex expression with parameters.
     * @param genericLaTeX the pure generic latex code
     */
    public void addAdditionalGenericLaTeXParameters(String genericLaTeX) {
        this.genericLaTeXParameters.add(genericLaTeX.replaceAll("#", PAR_PREFIX));
    }

    /**
     * @param genericLaTeX the pure generic latex code
     */
    public void setGenericLaTeXParametersWithOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfOptionalParameters = 1;
        this.numberOfParameters = numberOfParameters-1;
        this.genericLaTeXParameters.addFirst(genericLaTeX.replaceAll("#", PAR_PREFIX));
    }

    public void setGenericLaTeXParametersWithoutOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfParameters = numberOfParameters;
        this.numberOfOptionalParameters = 0;
        this.genericLaTeXParameters.addFirst(genericLaTeX.replaceAll("#", PAR_PREFIX));
    }

    /**
     * Sets the generic latex list of arguments
     * @param numOfArgs number of arguments
     * @param argumentsList list of arguments
     */
    public void setGenericLaTeXArguments(int numOfArgs, String argumentsList) {
        this.numberOfArguments = numOfArgs;
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
        LinkedList<String> genericLaTeX = new LinkedList<>();
        for ( String para : genericLaTeXParameters ) {
            if ( genericLaTeXArguments.isEmpty() ) genericLaTeX.add(para);
            else {
                for ( String args : genericLaTeXArguments ) {
                    genericLaTeX.add(para + " " + args);
                }
            }
        }
        return genericLaTeX;
    }

    @JsonGetter("semantictex")
    public String getSemanticLaTeX() {
        StringBuilder sb = new StringBuilder("\\");
        sb.append(name);

        int argCounter = 0;
        for (int i = 0; i < numberOfOptionalParameters; i++ ) {
            sb.append("[$").append(argCounter).append("]");
            argCounter++;
        }

        for ( int i = 0; i < numberOfParameters; i++ ) {
            sb.append("{$").append(argCounter).append("}");
            argCounter++;
        }

        // only if elements are following, we will add an @
        if ( numberOfArguments != 0 )
            sb.append("@");

        for ( int i = 0; i < numberOfArguments; i++ ) {
            sb.append("{$").append(argCounter).append("}");
            argCounter++;
        }

        return sb.toString();
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

    public int getNumberOfParameters() {
        return numberOfParameters;
    }

    public int getNumberOfOptionalParameters() {
        return numberOfOptionalParameters;
    }

    public int getNumberOfArguments() {
        return numberOfArguments;
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
