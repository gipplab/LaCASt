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
        for ( String texCase : cases ) {
            TranslationInformation ti = translator.translateToObject(texCase);
            this.requiredPackages = ti.getRequiredPackages();

            if ( ti.getPartialTranslations().isEmpty() ) {
                addTranslationTests(ti, config, translator);
            } else {
                for ( TranslationInformation subTi : ti.getPartialTranslations() ) {
                    addTranslationTests(subTi, config, translator);
                }
            }
        }
    }

    private void addTranslationTests(TranslationInformation ti, SymbolicalConfig config, IConstraintTranslator translator) {
        RelationalComponents relComps = ti.getRelationalComponents();
        if ( relComps.getComponents().size() == 1 ) {
            testExpressions.add( new SymbolicalTestBaseCase(relComps.getComponents().getFirst()) );
            expectedValues.add( config.getExpectationValue() );
        } else {
            LinkedList<String> comps = new LinkedList<>(relComps.getComponents());
            LinkedList<Relations> rels = new LinkedList<>(relComps.getRelations());
            while ( !rels.isEmpty() ) {
                String lhs = comps.removeFirst();
                String rhs = comps.getFirst();
                Relations rel = rels.removeFirst();

                if ( Relations.EQUAL.equals(rel) ) {
                    SymbolicalTestBaseCase test = new SymbolicalTestBaseCase(lhs, rhs, config.getTestExpression(lhs, rhs));
                    this.testExpressions.add( test );
                    this.expectedValues.add( config.getExpectationValue() );
                } else {
                    // rebuild expression and add it as it is... expecting true now :)
                    String testExpression = lhs + rel.getSymbol(translator.getTargetLanguage() ) + rhs;
                    SymbolicalTestBaseCase test = new SymbolicalTestBaseCase(lhs, rhs, testExpression);
                    this.testExpressions.add( test );
                    this.expectedValues.add( "true" );
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
