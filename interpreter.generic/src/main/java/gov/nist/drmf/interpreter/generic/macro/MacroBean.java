package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.LinkedList;

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

    @JsonIgnore
    private final LinkedList<Boolean[]> genericLateXDefaultArguments;

    private MacroMetaBean metaInformation;

    private int numberOfParameters;

    private int numberOfOptionalParameters = 0;

    private int numberOfArguments;

    private LinkedList<String> genericLaTeX;

    private LinkedList<String> semanticLaTeX;

    private boolean ifxAdded = false;

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
        this.genericLateXDefaultArguments = new LinkedList<>();
        this.genericLaTeX = new LinkedList<>();
    }

    /**
     * Sets the generic latex expression with parameters.
     * @param genericLaTeX the pure generic latex code
     */
    @JsonIgnore
    public void addAdditionalGenericLaTeXParameters(String genericLaTeX) {
        this.genericLaTeXParameters.add(genericLaTeX.replaceAll("#", MacroHelper.PAR_PREFIX));
    }

    /**
     * @param genericLaTeX the pure generic latex code
     */
    @JsonIgnore
    public void setGenericLaTeXParametersWithOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfOptionalParameters = 1;
        this.numberOfParameters = numberOfParameters-1;
        this.genericLaTeXParameters.addFirst(
                MacroHelper.fixInvisibleComma(genericLaTeX.replaceAll("#", MacroHelper.OPTIONAL_PAR_PREFIX))
        );
    }

    @JsonIgnore
    public void setGenericLaTeXParametersWithoutOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfParameters = numberOfParameters;
        this.genericLaTeXParameters.addLast(
                MacroHelper.fixInvisibleComma(genericLaTeX.replaceAll("#", MacroHelper.PAR_PREFIX))
        );
    }

    @JsonIgnore
    public void flipLastToOptionalParameter() {
        String latest = this.genericLaTeXParameters.removeLast();
        latest = latest.replaceAll( MacroHelper.PAR_PREFIX, MacroHelper.OPTIONAL_PAR_PREFIX );
        setGenericLaTeXParametersWithOptionalParameter(numberOfParameters, latest);
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
            MacroHelper.fillListWithArguments(
                    numberOfArguments,
                    this.genericLaTeXArguments,
                    this.genericLateXDefaultArguments,
                    argumentsList
            );
        } else {
            // one @ always means \mleft( ... \mright) depending on number of args
            this.genericLaTeXArguments.add(MacroHelper.generateArgumentList(numOfArgs));
            this.genericLateXDefaultArguments.add(MacroHelper.allTrueArr(numOfArgs));
        }
    }

    @JsonIgnore
    public boolean isIfxAdded() {
        return ifxAdded;
    }

    @JsonIgnore
    public void setIfxAdded(boolean ifxAdded) {
        this.ifxAdded = ifxAdded;
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
    public void setSemanticLaTeX(LinkedList<String> semanticLaTeX) {
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
            MacroHelper.fillInnerList(para, genericLaTeX, genericLaTeXArguments);
        }

        return genericLaTeX;
    }

    @JsonGetter("semanticTeX")
    public LinkedList<String> getSemanticLaTeX() {
        if ( this.semanticLaTeX != null ) return this.semanticLaTeX;
        this.semanticLaTeX = new LinkedList<>();


        for ( int i = numberOfOptionalParameters; i >= 0; i-- ) {
            StringBuilder sb = new StringBuilder("\\");
            sb.append(name);
            MacroHelper.addIdx( MacroHelper.OPTIONAL_PAR_PREFIX, i, new Character[]{'[', ']'}, sb );
            MacroHelper.addIdx( MacroHelper.PAR_PREFIX, numberOfParameters, new Character[]{'{', '}'}, sb );

            if ( genericLaTeXArguments.size() == 0 ) {
                this.semanticLaTeX.add(sb.toString());
            }

            for ( int j = 0; j < genericLateXDefaultArguments.size(); j++ ) {
                StringBuilder innerSB = new StringBuilder(sb.toString());
                // only if elements are following, we will add an @
                if ( numberOfArguments != 0 ){
                    innerSB.append("@".repeat(j+1));
                }

                Boolean[] defArgs = genericLateXDefaultArguments.get(j);
                for ( int k = 1; k <= numberOfArguments; k++ ) {
                    Boolean useDef = defArgs[k-1];
                    innerSB.append("{");
                    if ( useDef != null && useDef ) {
                        innerSB.append(MacroHelper.VAR_PREFIX).append(k);
                    } else {
                        String def = metaInformation.getStandardArguments().getStandardVariables().get(k-1);
                        innerSB.append(def);
                    }
                    innerSB.append("}");
                }

                this.semanticLaTeX.add(innerSB.toString());
            }
        }

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
