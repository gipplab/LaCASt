package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.LinkedList;

import static gov.nist.drmf.interpreter.generic.macro.MacroHelper.*;

/**
 * Java class representing a Macro from a .sty file
 */
@SuppressWarnings("unused")
public class MacroBean {
    /**
     * Unique identifier (quasi-final)
     */
    private String name;

    /**
     * The pure generic LaTeX for parameters
     */
    private final LinkedList<String> genericLaTeXParameters;

    /**
     * The pure generic LaTeX for arguments
     */
    private final LinkedList<String> genericLaTeXArguments;

    private MacroMetaBean metaInformation;

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

    @JsonSetter("TeX")
    public void setGenericLaTeX(LinkedList<String> genericLaTeX) {
        this.genericLaTeX = genericLaTeX;
    }

    @JsonSetter("semanticTeX")
    public void setSemanticLaTeX(String semanticLaTeX) {
        this.semanticLaTeX = semanticLaTeX;
    }

    @JsonSetter("meta")
    public void setMetaInformation(MacroMetaBean metaInformation) {
        this.metaInformation = metaInformation;
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

    @JsonGetter("macro")
    public String getName() {
        return name;
    }

    @JsonGetter("TeX")
    public LinkedList<String> getGenericLatex() {
        if ( !genericLaTeX.isEmpty() ) return genericLaTeX;

        for ( String para : genericLaTeXParameters ) {
            if ( genericLaTeXArguments.isEmpty() ) {
                genericLaTeX.add(para);
            } else {
                for ( String args : genericLaTeXArguments ) {
                    genericLaTeX.add(para + " " + args);
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

        int argCounter = addIdx( numberOfOptionalParameters, 0, new Character[]{'[', ']'}, sb );
        argCounter = addIdx( numberOfParameters, argCounter, new Character[]{'{', '}'}, sb );

        // only if elements are following, we will add an @
        if ( numberOfArguments != 0 )
            sb.append("@");

        addIdx( numberOfArguments, argCounter, new Character[]{'{', '}'}, sb );

        this.semanticLaTeX = sb.toString();
        return this.semanticLaTeX;
    }

    @JsonGetter("meta")
    public MacroMetaBean getMetaInformation() {
        return metaInformation;
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
}
