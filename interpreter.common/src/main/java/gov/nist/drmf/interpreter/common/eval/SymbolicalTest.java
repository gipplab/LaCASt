package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
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

    private List<String> testExpressions;
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

        TranslationInformation ti = translator.translateToObject(latex);
        this.requiredPackages = ti.getRequiredPackages();

        this.testExpressions = new LinkedList<>();
        this.expectedValues = new LinkedList<>();
        RelationalComponents relComps = ti.getRelationalComponents();
        if ( relComps.getComponents().size() == 1 ) {
            testExpressions.add( relComps.getComponents().getFirst() );
            expectedValues.add( config.getExpectationValue() );
        } else {
            LinkedList<String> comps = new LinkedList<>(relComps.getComponents());
            LinkedList<Relations> rels = new LinkedList<>(relComps.getRelations());
            while ( !rels.isEmpty() ) {
                String lhs = comps.removeFirst();
                String rhs = comps.getFirst();
                Relations rel = rels.removeFirst();

                if ( Relations.EQUAL.equals(rel) ) {
                    this.testExpressions.add( config.getTestExpression(lhs, rhs) );
                    this.expectedValues.add( config.getExpectationValue() );
                } else {
                    // rebuild expression and add it as it is... expecting true now :)
                    String testExpression = lhs + rel.getSymbol(translator.getTargetLanguage() ) + rhs;
                    this.testExpressions.add( testExpression );
                    this.expectedValues.add( "true" );
                }
            }
        }
    }

    public void setTestCases(ISymbolicTestCases[] testCases) {
        this.testCases = testCases;
    }

    public void setTestExpressions(List<String> testExpressions) {
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

    public List<String> getTestExpression() {
        return testExpressions;
    }

    public List<String> getExpectedOutcome() {
        return expectedValues;
    }

    public Set<String> getRequiredPackages() {
        return requiredPackages;
    }
}
