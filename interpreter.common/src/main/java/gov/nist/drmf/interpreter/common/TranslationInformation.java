package gov.nist.drmf.interpreter.common;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationInformation {

    private String expression, translatedExpression;

    private InformationLogger information;

    private Set<String> requiredPackages;

    public TranslationInformation(
            String expression,
            String translatedExpression
    ) {
        this.expression = expression;
        this.translatedExpression = translatedExpression;
        this.information = new InformationLogger();
        this.requiredPackages = new HashSet<>();
    }

    public void setInformation(InformationLogger information) {
        this.information = new InformationLogger(information);
    }

    public void setRequiredPackages(Set<String> requiredPackages) {
        this.requiredPackages = new HashSet<>(requiredPackages);
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
}
