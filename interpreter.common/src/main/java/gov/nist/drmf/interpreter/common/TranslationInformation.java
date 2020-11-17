package gov.nist.drmf.interpreter.common;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationInformation {

    private String expression, translatedExpression;

    private List<String> translatedConstraints;

    private InformationLogger information;

    private Set<String> requiredPackages;

    private List<TranslationInformation> partialTranslations;

    public TranslationInformation(
            String expression,
            String translatedExpression
    ) {
        this.expression = expression;
        this.translatedExpression = translatedExpression;
        this.information = new InformationLogger();
        this.requiredPackages = new HashSet<>();
        this.translatedConstraints = new LinkedList<>();
        this.partialTranslations = new LinkedList<>();
    }

    public void setInformation(InformationLogger information) {
        this.information = new InformationLogger(information);
    }

    public void setRequiredPackages(Set<String> requiredPackages) {
        this.requiredPackages = new HashSet<>(requiredPackages);
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
        return information.getFreeVariables();
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
