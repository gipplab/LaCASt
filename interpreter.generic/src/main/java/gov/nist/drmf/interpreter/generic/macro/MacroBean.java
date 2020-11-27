package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Java class representing a Macro from a .sty file
 */
@SuppressWarnings("unused")
public class MacroBean {
    @JsonIgnore
    private static final Logger LOG = LogManager.getLogger(MacroBean.class.getName());

    @JsonIgnore
    private final MacroDistributionAnalyzer macroDistributions;

    /**
     * Unique identifier (quasi-final)
     */
    @JsonProperty("macro")
    private String name;

    /**
     * The pure generic LaTeX for parameters
     */
    @JsonIgnore
    private final LinkedList<String> genericLaTeXParameters;

    /**
     * The pure generic LaTeX for arguments
     */
    @JsonIgnore
    private final LinkedList<String> genericLaTeXArguments;

    @JsonIgnore
    private final LinkedList<Boolean[]> genericLateXDefaultArguments;

    @JsonProperty("meta")
    private MacroMetaBean metaInformation;

    @JsonProperty("numberOfParameters")
    private int numberOfParameters;

    @JsonProperty("numberOfOptionalParameters")
    private int numberOfOptionalParameters = 0;

    @JsonProperty("numberOfArguments")
    private int numberOfArguments;

    @JsonProperty("TeX")
    private LinkedList<MacroGenericSemanticEntry> tex;

    @JsonIgnore
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
        this.tex = new LinkedList<>();
        this.macroDistributions = MacroDistributionAnalyzer.getStandardInstance();
    }

    /**
     * Sets the generic latex expression with parameters.
     *
     * @param genericLaTeX the pure generic latex code
     */
    @JsonIgnore
    public void addAdditionalGenericLaTeXParameters(String genericLaTeX) {
        genericLaTeX = MacroHelper.cleanArgument(genericLaTeX, MacroHelper.PAR_PREFIX);
        this.genericLaTeXParameters.add(genericLaTeX);
    }

    /**
     * @param genericLaTeX the pure generic latex code
     */
    @JsonIgnore
    public void setGenericLaTeXParametersWithOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfOptionalParameters = 1;
        this.numberOfParameters = numberOfParameters - 1;
        this.genericLaTeXParameters.addFirst(MacroHelper.cleanArgument(genericLaTeX, MacroHelper.OPTIONAL_PAR_PREFIX));
    }

    @JsonIgnore
    public void setGenericLaTeXParametersWithoutOptionalParameter(int numberOfParameters, String genericLaTeX) {
        this.numberOfParameters = numberOfParameters;
        genericLaTeX = MacroHelper.cleanArgument(genericLaTeX, MacroHelper.PAR_PREFIX);
        this.genericLaTeXParameters.addLast(genericLaTeX);
    }

    @JsonIgnore
    public void flipLastToOptionalParameter() {
        String latest = this.genericLaTeXParameters.removeLast();
        latest = latest.replaceAll(MacroHelper.PAR_PREFIX, MacroHelper.OPTIONAL_PAR_PREFIX);
        setGenericLaTeXParametersWithOptionalParameter(numberOfParameters, latest);
    }

    /**
     * Sets the generic latex list of arguments
     *
     * @param numOfArgs     number of arguments
     * @param argumentsList list of arguments
     */
    @JsonIgnore
    public void setGenericLaTeXArguments(int numOfArgs, String argumentsList) {
        this.numberOfArguments = numOfArgs;
        if (numOfArgs == 0) {
            // just generate an empty list
            return;
        }

        if (argumentsList != null && argumentsList.length() > 3) {
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
    public void setMacroName(String macro) {
        this.name = macro;
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

    @JsonSetter("TeX")
    public void setTex(List<MacroGenericSemanticEntry> tex) {
        this.tex = new LinkedList<>(tex);
    }

    @JsonGetter("TeX")
    public LinkedList<MacroGenericSemanticEntry> getTex() {
        if (!this.tex.isEmpty()) return this.tex;
        this.tex = buildTex();
        return this.tex;
    }

    @JsonIgnore
    private LinkedList<MacroGenericSemanticEntry> buildTex() {
        LinkedList<MacroGenericSemanticEntry> texList = new LinkedList<>();
        LinkedList<String> genericLatexList = new LinkedList<>();

        MacroCounter counter = macroDistributions.getMacroCounter("\\" + name);

        for (String para : genericLaTeXParameters) {
            MacroHelper.fillInnerList(para, genericLatexList, genericLaTeXArguments);
        }

        for (int i = numberOfOptionalParameters; i >= 0; i--) {
            StringBuilder sb = new StringBuilder("\\");
            sb.append(name);
            MacroHelper.addIdx(MacroHelper.OPTIONAL_PAR_PREFIX, i, new Character[]{'[', ']'}, sb);
            MacroHelper.addIdx(MacroHelper.PAR_PREFIX, numberOfParameters, new Character[]{'{', '}'}, sb);

            String noArgExpression = genericLatexList.remove(0);

            MacroGenericSemanticEntry entry;
            for (int j = 0; j < genericLateXDefaultArguments.size(); j++) {
                StringBuilder sbCopy = new StringBuilder(sb);
                int atSymbols = j+1;
                double score = counter != null ? counter.getScore( i > 0, atSymbols ) : 0;

                if (numberOfArguments != 0) {
                    sbCopy.append("@".repeat(atSymbols));
                }

                MacroHelper.generateDefaultArgList(
                        sbCopy,
                        genericLateXDefaultArguments.get(j),
                        metaInformation.getStandardArguments().getStandardVariables()
                );

                entry = new MacroGenericSemanticEntry(
                        genericLatexList.remove(0),
                        sbCopy.toString(),
                        score
                );
                texList.addLast(entry);
            }

            if ( numberOfArguments > 0 ) sb.append("@");

            MacroHelper.generateDefaultArgList(
                    sb,
                    MacroHelper.fillArr(numberOfArguments, false),
                    metaInformation.getStandardArguments().getStandardVariables()
            );

            entry = new MacroGenericSemanticEntry(
                    noArgExpression,
                    sb.toString(),
                    counter != null ? counter.getScore( false, 0 ) : 0
            );

            texList.add(entry);
        }

        return texList;
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

    @JsonIgnore
    @Override
    public String toString() {
        return this.name;
    }
}
