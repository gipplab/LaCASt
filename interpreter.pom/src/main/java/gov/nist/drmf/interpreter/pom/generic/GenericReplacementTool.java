package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.common.interfaces.TranslationFeature;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class GenericReplacementTool implements TranslationFeature<PrintablePomTaggedExpression> {
    private static final Logger LOG = LogManager.getLogger(GenericReplacementTool.class.getName());

    @Override
    public PrintablePomTaggedExpression preProcess(PrintablePomTaggedExpression obj) {
        TranslationFeature<PrintablePomTaggedExpression> fixer = defaultGenericReplacements();
        return fixer.preProcess(obj);
    }

    public static TranslationFeature<PrintablePomTaggedExpression> defaultGenericReplacements() {
        return TranslationFeature.combine(
                new GenericDifferentialDFixer(),
                new GenericFractionDerivFixer(),
                new GenericConstantReplacer(),
                new GenericFunctionAnnotator(),
                new GenericNormalizeOperatorNameCarets()
        );
    }

    public PrintablePomTaggedExpression getSemanticallyEnhancedExpression(PrintablePomTaggedExpression ppte) {
        return preProcess(ppte);
    }
}
