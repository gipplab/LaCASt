package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.nist.drmf.interpreter.generic.elasticsearch.ElasticSearchConnector;
import gov.nist.drmf.interpreter.generic.interfaces.IGenericLatexSemanticEnhancerAPI;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.struct.ContextContentType;
import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIPresentations;
import gov.nist.drmf.interpreter.generic.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class GenericLatexSemanticEnhancer implements IGenericLatexSemanticEnhancerAPI {
    public GenericLatexSemanticEnhancer() {

    }

    public SemanticEnhancedDocument getSemanticEnhancedDocument(String context) {
        ContextAnalyzer contextAnalyzer = new ContextAnalyzer(context, ContextContentType.WIKITEXT);
        contextAnalyzer.analyze();
        MLPDependencyGraph annotatedGraph = contextAnalyzer.getDependencyGraph();
        return new SemanticEnhancedDocument(contextAnalyzer.getTitle(), annotatedGraph);
    }

    @Override
    public PrintablePomTaggedExpression enhanceGenericLaTeX(String latex, String context, String dlmfLabel) throws ParseException {
        throw new RuntimeException("enhanceGenericLaTeX is not yet implemented.");
    }

    public static void main(String[] args) throws IOException {
        Path p = Paths.get("Jacobi_polynomials.xml");
        String jacobiContext = Files.readString(p);
        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        SemanticEnhancedDocument doc = enhancer.getSemanticEnhancedDocument(jacobiContext);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(doc);
        Files.writeString( Paths.get("Results.json"), serializedDoc );

        List<MOIPresentations> moi = doc.getFormulae();
        for ( MOIPresentations m : moi ) {
            System.out.println(m);
        }
        ElasticSearchConnector.getDefaultInstance().stop();
    }
}
