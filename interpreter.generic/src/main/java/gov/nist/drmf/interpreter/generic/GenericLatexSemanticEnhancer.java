package gov.nist.drmf.interpreter.generic;

import gov.nist.drmf.interpreter.generic.interfaces.IGenericLatexSemanticEnhancerAPI;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.struct.ContextContentType;
import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public class GenericLatexSemanticEnhancer implements IGenericLatexSemanticEnhancerAPI {
    public GenericLatexSemanticEnhancer() {

    }

    public SemanticEnhancedDocument getSemanticEnhancedDocument(String context) {
        ContextAnalyzer contextAnalyzer = new ContextAnalyzer(context, ContextContentType.WIKITEXT);
        MLPDependencyGraph annotatedGraph = contextAnalyzer.extractDefiniens();

        return new SemanticEnhancedDocument(annotatedGraph);
    }

    @Override
    public PrintablePomTaggedExpression enhanceGenericLaTeX(String latex, String context, String dlmfLabel) throws ParseException {
        throw new RuntimeException("enhanceGenericLaTeX is not yet implemented.");
    }
}
