package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class GenericReplacementTool {
    private static final Logger LOG = LogManager.getLogger(GenericReplacementTool.class.getName());

    private final GenericDifferentialDFixer diffFixer;
    private final GenericFractionDerivFixer derivFixer;

    public GenericReplacementTool(PrintablePomTaggedExpression ppte) {
        this.diffFixer = new GenericDifferentialDFixer(ppte);
        this.derivFixer = new GenericFractionDerivFixer(ppte);
    }

    public PrintablePomTaggedExpression getSemanticallyEnhancedExpression() {
        this.diffFixer.fixDifferentialD();
        return this.derivFixer.fixGenericDeriv();
    }
}
