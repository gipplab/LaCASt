package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;
import gov.nist.drmf.interpreter.common.latex.Relations;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class DefaultNumericalTestCaseBuilder {
    private final NumericalConfig config;
    private final ICASEngineNumericalEvaluator<?> evaluator;
    private final IConstraintTranslator translator;
    private final INumericalEvaluationScripts scriptMapper;

    public DefaultNumericalTestCaseBuilder(
            NumericalConfig config,
            ICASEngineNumericalEvaluator<?> evaluator,
            IConstraintTranslator translator,
            INumericalEvaluationScripts scriptMapper
    ) {
        this.config = config;
        this.evaluator = evaluator;
        this.translator = translator;
        this.scriptMapper = scriptMapper;
    }

    public NumericalConfig getConfig() {
        return config;
    }

    public List<NumericalTest> buildTestCases(
            TranslationInformation ti,
            INumericTestCase testCase
    ) {
        LinkedList<NumericalTest> tests = new LinkedList<>();
        RelationalComponents relComps = ti.getRelationalComponents();
        if ( relComps.getRelations().isEmpty() || relComps.getComponents().size() < 2 ) {
            // now LHS/RHS
            NumericalTest test = buildNoRelationTestCase(relComps.getComponents().get(0), testCase);
            appendInfoToTest(test, ti, true);
            tests.add(test);
        } else {
            LinkedList<String> tmpCompList = new LinkedList<>(relComps.getComponents());
            LinkedList<Relations> tmpRelList = new LinkedList<>(relComps.getRelations());
            while ( !tmpRelList.isEmpty() ) {
                Relations rel = tmpRelList.removeFirst();
                NumericalTest test = buildSimpleTestCase(
                        tmpCompList.removeFirst(),
                        tmpCompList.getFirst(),
                        rel,
                        testCase
                );
                appendInfoToTest(test, ti, Relations.EQUAL.equals(rel));
                tests.add(test);
            }
        }
        return tests;
    }

    private void appendInfoToTest(NumericalTest test, TranslationInformation ti, boolean isEquation) {
        test.setVariables(ti.getFreeVariables().getFreeVariables());
        test.setPostProcessingMethodName( scriptMapper.getPostProcessingScriptName(isEquation) );
    }

    private NumericalTest buildSimpleTestCase(String lhs, String rhs, Relations rel, INumericTestCase c) {
        String testExpression;

        if ( !Relations.EQUAL.equals(rel) ) {
            testExpression = lhs + rel.getSymbol(translator.getTargetLanguage()) + rhs;
        } else {
            testExpression = config.getTestExpression(evaluator, lhs, rhs);
        }

        return buildNoRelationTestCase(testExpression, c);
    }

    private NumericalTest buildNoRelationTestCase(String component, INumericTestCase c) {
        return new NumericalTest(
                component,
                c,
                config,
                translator
        );
    }
}
