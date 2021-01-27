package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.common.latex.FreeVariables;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationInformation {
    private String expression, translatedExpression;

    private List<String> translatedConstraints;

    private InformationLogger information;

    private Set<String> requiredPackages;

    private FreeVariables freeVariables;

    private RelationalComponents relationalComponents;

    private List<TranslationInformation> partialTranslations;

    public TranslationInformation() {
        this.expression = "";
        this.translatedExpression = "";
        this.information = new InformationLogger();
        this.requiredPackages = new HashSet<>();
        this.translatedConstraints = new LinkedList<>();
        this.freeVariables = new FreeVariables();
        this.relationalComponents = new RelationalComponents();
        this.partialTranslations = new LinkedList<>();
    }

    public TranslationInformation(String expression, String translatedExpression) {
        this();
        this.expression = expression;
        this.translatedExpression = translatedExpression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setTranslatedExpression(String translatedExpression) {
        this.translatedExpression = translatedExpression;
    }

    public void setInformation(InformationLogger information) {
        this.information = new InformationLogger(information);
    }

    public void setRequiredPackages(Set<String> requiredPackages) {
        this.requiredPackages = new HashSet<>(requiredPackages);
    }

    public void setFreeVariables(FreeVariables freeVariables) {
        this.freeVariables = freeVariables;
    }

    public void setRelationalComponents(RelationalComponents relationalComponents) {
        this.relationalComponents = relationalComponents;
    }

    public void addTranslatedConstraints(String... translatedConstraint) {
        this.translatedConstraints.addAll(Arrays.asList(translatedConstraint));
    }

    public void addTranslatedConstraints(Collection<String> translatedConstraint) {
        this.translatedConstraints.addAll(translatedConstraint);
    }

    public void addTranslations(TranslationInformation... translationInformation) {
        this.partialTranslations.addAll(Arrays.asList(translationInformation));
    }

    public void addTranslations(Collection<TranslationInformation> translationInformation) {
        this.partialTranslations.addAll(translationInformation);
    }

    public String getExpression() {
        return expression;
    }

    public String getTranslatedExpression() {
        return translatedExpression;
    }

    public InformationLogger getTranslationInformation() {
        return information;
    }

    public FreeVariables getFreeVariables() {
        return freeVariables;
    }

    public RelationalComponents getRelationalComponents() {
        return relationalComponents;
    }

    public Set<String> getRequiredPackages() {
        return requiredPackages;
    }

    public List<String> getTranslatedConstraints() {
        return translatedConstraints;
    }

    public List<TranslationInformation> getPartialTranslations() {
        return partialTranslations;
    }
}
