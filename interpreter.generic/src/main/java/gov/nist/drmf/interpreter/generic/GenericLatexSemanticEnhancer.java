package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextPageExtractorMapper;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class GenericLatexSemanticEnhancer implements IGenericLatexSemanticEnhancerAPI {
    public GenericLatexSemanticEnhancer() {}

    public SemanticEnhancedDocument getSemanticEnhancedDocument(ContextAnalyzer analyzer) {
        analyzer.analyze();
        MLPDependencyGraph annotatedGraph = analyzer.getDependencyGraph();
        return new SemanticEnhancedDocument(analyzer.getTitle(), annotatedGraph);
    }

    public List<SemanticEnhancedDocument> getSemanticEnhancedDocuments(String context) {
        List<String> pages = new LinkedList<>();
        pages.add(context);
        return getSemanticEnhancedDocuments(pages);
    }

    public List<SemanticEnhancedDocument> getSemanticEnhancedDocuments(Path filePath) throws IOException {
        String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        return getSemanticEnhancedDocuments(fileContent);
    }

    public List<SemanticEnhancedDocument> getSemanticEnhancedDocuments(Collection<String> contents) {
        WikiTextPageExtractorMapper pageMapper = new WikiTextPageExtractorMapper();
        return contents.stream()
                .parallel()
                .flatMap( pageMapper::streamFlatMap )
                .map( ContextAnalyzer::new )
                .map( this::getSemanticEnhancedDocument )
                .collect(Collectors.toList());
    }

    @Override
    public PrintablePomTaggedExpression enhanceGenericLaTeX(String latex, String context, String dlmfLabel) throws ParseException {
        throw new RuntimeException("enhanceGenericLaTeX is not yet implemented.");
    }

    public static void main(String[] args) throws IOException {
//        Path p = Paths.get("/mnt/share/data/wikipedia/dlmf-template-pages-26-11-2020.xml");
//        Path p = Paths.get("BesselFunction.xml");
        Path p = Paths.get("Jacobi_polynomials.xml");
        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        List<SemanticEnhancedDocument> docs = enhancer.getSemanticEnhancedDocuments(p);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(docs);
//        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-03-12-2020.json"), serializedDoc );
//        Files.writeString( Paths.get("Result-Bessel.json"), serializedDoc );
        Files.writeString( Paths.get("Results.json"), serializedDoc );

        for ( SemanticEnhancedDocument doc : docs ) {
            System.out.println("Document: " + doc.getTitle());
            List<MOIPresentations> moi = doc.getFormulae();
            for ( MOIPresentations m : moi ) {
                System.out.println(m);
            }
        }

        ElasticSearchConnector.getDefaultInstance().stop();
    }
}
