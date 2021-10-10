package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.latex.CaseSplitter;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;
import gov.nist.drmf.interpreter.common.latex.Relations;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicalTest implements Serializable {

    private ISymbolicTestCases[] testCases;

    private List<SymbolicalTestBaseCase> testExpressions;
    private List<String> expectedValues;

    private Set<String> requiredPackages;

    public SymbolicalTest() {
        testCases = new ISymbolicTestCases[]{};
        testExpressions = new LinkedList<>();
        expectedValues = new LinkedList<>();
        requiredPackages = new HashSet<>();
    }

    /**
     * This setup is required for ov.nist.drmf.interpreter.evaluation.core.symbolic.SymbolicEvaluator
     * Here the setup was performed prior and we simple add all necessary values to perform the tests.
     * The given testExpression for example is fixed. All splitting was also performed prior so that the
     * {@link #testExpressions} will always only contain a single value.
     * <p>
     * This is in contrast to the setup from generic evaluation which presumes the setup will be performed
     * here in this class.
     *
     * @param lhs
     * @param rhs
     * @param testExpression
     * @param testCases
     * @param config
     * @param translator
     */
    public SymbolicalTest(
            String lhs, String rhs,
            String testExpression,
            ISymbolicTestCases[] testCases,
            SymbolicalConfig config,
            IConstraintTranslator translator
    ) {
        super();
        this.testCases = testCases;

        this.testExpressions = new LinkedList<>();
        this.expectedValues = new LinkedList<>();

        SymbolicalTestBaseCase stbc = new SymbolicalTestBaseCase(lhs, rhs, testExpression);
        this.testExpressions.add(stbc);
        this.expectedValues.add(config.getExpectationValue());
    }

    /**
     * This setup is required for generic translator, specifically: gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer
     * This setup is different because it presumes the given argument was NOT translated so the splitting in multiple cases
     * etc is done here. In contrast to the real SymbolEvaluator in gov.nist.drmf.interpreter.evaluation.core.symbolic.SymbolicEvaluator
     *
     * @param config
     * @param translator
     * @param latex
     * @param testCases
     */
    public SymbolicalTest(
            SymbolicalConfig config,
            IConstraintTranslator translator,
            String latex,
            ISymbolicTestCases[] testCases
    ) {
        this.testCases = testCases;

        this.testExpressions = new LinkedList<>();
        this.expectedValues = new LinkedList<>();

        List<String> cases = CaseSplitter.splitPMSymbols(latex);
        for (String texCase : cases) {
            TranslationInformation ti = translator.translateToObject(texCase);
            this.requiredPackages = ti.getRequiredPackages();

            if (ti.getPartialTranslations().isEmpty()) {
                addTranslationTests(ti, config, translator);
            } else {
                for (TranslationInformation subTi : ti.getPartialTranslations()) {
                    addTranslationTests(subTi, config, translator);
                }
            }
        }
    }

    private void addTranslationTests(TranslationInformation ti, SymbolicalConfig config, IConstraintTranslator translator) {
        RelationalComponents relComps = ti.getRelationalComponents();
        if (relComps.getComponents().size() == 1) {
            testExpressions.add(new SymbolicalTestBaseCase(relComps.getComponents().getFirst()));
            expectedValues.add(config.getExpectationValue());
        } else {
            LinkedList<String> comps = new LinkedList<>(relComps.getComponents());
            LinkedList<Relations> rels = new LinkedList<>(relComps.getRelations());
            while (!rels.isEmpty()) {
                String lhs = comps.removeFirst();
                String rhs = comps.getFirst();
                Relations rel = rels.removeFirst();

                if (Relations.EQUAL.equals(rel)) {
                    SymbolicalTestBaseCase test = new SymbolicalTestBaseCase(lhs, rhs, config.getTestExpression(lhs, rhs));
                    this.testExpressions.add(test);
                    this.expectedValues.add(config.getExpectationValue());
                } else {
                    // rebuild expression and add it as it is... expecting true now :)
                    String testExpression = lhs + rel.getSymbol(translator.getTargetLanguage()) + rhs;
                    SymbolicalTestBaseCase test = new SymbolicalTestBaseCase(lhs, rhs, testExpression);
                    this.testExpressions.add(test);
                    this.expectedValues.add("true");
                }
            }
        }
    }

    public void setTestCases(ISymbolicTestCases[] testCases) {
        this.testCases = testCases;
    }

    public void setTestExpressions(List<SymbolicalTestBaseCase> testExpressions) {
        this.testExpressions = testExpressions;
    }

    public void setExpectedValues(List<String> expectedValues) {
        this.expectedValues = expectedValues;
    }

    public void setRequiredPackages(Set<String> requiredPackages) {
        this.requiredPackages = requiredPackages;
    }

    public ISymbolicTestCases[] getTestCases() {
        return testCases;
    }

    public List<SymbolicalTestBaseCase> getTestExpression() {
        return testExpressions;
    }

    public List<String> getExpectedOutcome() {
        return expectedValues;
    }

    public Set<String> getRequiredPackages() {
        return requiredPackages;
    }
}
