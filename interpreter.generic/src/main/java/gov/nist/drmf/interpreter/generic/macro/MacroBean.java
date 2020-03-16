package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class representing a Macro from a .sty file
 */
//@JsonIgnoreProperties(
//        ignoreUnknown = true
//)
public class MacroBean {
    public static final Pattern CLEAN_PATTERN = Pattern.compile(
            "\\\\m(?:left|right)"
    );

    public static final String VAR_PREFIX = "var";
    public static final String PAR_PREFIX = "par";
    public static final String OPTIONAL_PAR_PREFIX = "opPar";

    /**
     * Unique identifier (quasi-final)
     */
    private String name;

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
    private String description;

    /**
     * Unique string of description
     */
    private String meaning;

    /**
     * OpenMath name
     */
    private String openMathID;

    /**
     * The standard parameters
     */
    private LinkedList<String> standardParameters;

    /**
     * The standard arguments
     */
    private LinkedList<String> standardArguments;

    private int numberOfParameters;

    private int numberOfOptionalParameters = 0;

    private int numberOfArguments;

    private LinkedList<String> genericLaTeX;

    private String semanticLaTeX;

    /**
     * Only for serialization.
     */
    private MacroBean() {
        this(null);
    }

    /**
     * @param name the macro
     */
    public MacroBean(String name) {
        this.name = name;
        this.genericLaTeXParameters = new LinkedList<>();
        this.genericLaTeXArguments = new LinkedList<>();
        this.genericLaTeX = new LinkedList<>();
    }

    /**
     * Sets the generic latex expression with parameters.
     * @param genericLaTeX the pure generic latex code
     */
    @JsonIgnore
    public void addAdditionalGenericLaTeXParameters(String genericLaTeX) {
        this.genericLaTeXParameters.add(genericLaTeX.replaceAll("#", PAR_PREFIX));
    }

    /**
     * @param genericLaTeX the pure generic latex code
     */
    @JsonIgnore
    public void setGenericLaTeXParametersWithOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfOptionalParameters = 1;
        this.numberOfParameters = numberOfParameters-1;
        this.genericLaTeXParameters.addFirst(genericLaTeX.replaceAll("#", PAR_PREFIX));
    }

    @JsonIgnore
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
    @JsonIgnore
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

    @JsonSetter("macro")
    public void setMacroName(String macro){
        this.name = macro;
    }

    @JsonSetter("meaning")
    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    @JsonSetter("openMathID")
    public void setOpenMathID(String openMathID) {
        this.openMathID = openMathID;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public void setStandardParameters(String para) {
        this.standardParameters = generateListOfArguments(para);
    }

    @JsonIgnore
    public void setStandardArguments(String args) {
        this.standardArguments = generateListOfArguments(args);
    }

    @JsonSetter("standardParameters")
    public void setStandardParameters(LinkedList<String> standardParameters) {
        this.standardParameters = standardParameters;
    }

    @JsonSetter("standardArguments")
    public void setStandardArguments(LinkedList<String> standardArguments) {
        this.standardArguments = standardArguments;
    }

    @JsonSetter("numberOfParameters")
    public void setNumberOfParameters(int numberOfParameters) {
        this.numberOfParameters = numberOfParameters;
    }

    @JsonSetter("numberOfOptionalParameters")
    public void setNumberOfOptionalParameters(int numberOfOptionalParameters) {
        this.numberOfOptionalParameters = numberOfOptionalParameters;
    }

    @JsonSetter("numberOfArguments")
    public void setNumberOfArguments(int numberOfArguments) {
        this.numberOfArguments = numberOfArguments;
    }

    @JsonSetter("TeX")
    public void setGenericLaTeX(LinkedList<String> genericLaTeX) {
        this.genericLaTeX = genericLaTeX;
    }

    @JsonSetter("semanticTeX")
    public void setSemanticLaTeX(String semanticLaTeX) {
        this.semanticLaTeX = semanticLaTeX;
    }

    @JsonGetter("macro")
    public String getName() {
        return name;
    }

    @JsonGetter("TeX")
    public LinkedList<String> getGenericLatex() {
        if ( genericLaTeX.isEmpty() ) {
            for ( String para : genericLaTeXParameters ) {
                if ( genericLaTeXArguments.isEmpty() ) genericLaTeX.add(para);
                else {
                    for ( String args : genericLaTeXArguments ) {
                        genericLaTeX.add(para + " " + args);
                    }
                }
            }
        }
        return genericLaTeX;
    }

    @JsonGetter("semanticTeX")
    public String getSemanticLaTeX() {
        if ( this.semanticLaTeX != null ) return this.semanticLaTeX;
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

        this.semanticLaTeX = sb.toString();
        return this.semanticLaTeX;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonGetter("meaning")
    public String getMeaning() {
        return meaning;
    }

    @JsonGetter("openMathID")
    public String getOpenMathID() {
        return openMathID;
    }

    @JsonGetter("standardParameters")
    public LinkedList<String> getStandardParameters() {
        return standardParameters;
    }

    @JsonGetter("standardArguments")
    public LinkedList<String> getStandardArguments() {
        return standardArguments;
    }

    @JsonGetter("numberOfParameters")
    public int getNumberOfParameters() {
        return numberOfParameters;
    }

    @JsonGetter("numberOfOptionalParameters")
    public int getNumberOfOptionalParameters() {
        return numberOfOptionalParameters;
    }

    @JsonGetter("numberOfArguments")
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
