package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class GenericReplacementTool {
    private static final Logger LOG = LogManager.getLogger(GenericReplacementTool.class.getName());

    private final GenericDifferentialDFixer diffFixer;
    private final GenericFractionDerivFixer derivFixer;
    private final GenericConstantReplacer constantFixer;
    private final GenericFunctionAnnotator functionAnnotator;
    private final GenericNormalizeOperatorNameCarets normalizeOperatorNameCarets;

    public GenericReplacementTool(PrintablePomTaggedExpression ppte) {
        this.diffFixer = new GenericDifferentialDFixer(ppte);
        this.derivFixer = new GenericFractionDerivFixer(ppte);
        this.constantFixer = new GenericConstantReplacer(ppte);
        this.functionAnnotator = new GenericFunctionAnnotator();
        this.normalizeOperatorNameCarets = new GenericNormalizeOperatorNameCarets(ppte);
    }

    public PrintablePomTaggedExpression getSemanticallyEnhancedExpression() {
        this.normalizeOperatorNameCarets.normalize();
        this.constantFixer.fixConstants();
        this.diffFixer.fixDifferentialD();
        PrintablePomTaggedExpression ppte = this.derivFixer.fixGenericDeriv();
        return this.functionAnnotator.preProcess(ppte);
    }
}
