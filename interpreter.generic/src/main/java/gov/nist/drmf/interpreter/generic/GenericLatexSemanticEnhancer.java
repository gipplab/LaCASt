package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextPageExtractorMapper;
import gov.nist.drmf.interpreter.generic.interfaces.IGenericLatexSemanticEnhancerAPI;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.Document;
import gov.nist.drmf.interpreter.generic.mlp.WikitextDocument;
import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIPresentations;
import gov.nist.drmf.interpreter.generic.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is the API endpoint to semantically enhance math in entire documents.
 *
 * @author Andre Greiner-Petter
 */
public class GenericLatexSemanticEnhancer implements IGenericLatexSemanticEnhancerAPI {
    private static final Logger LOG = LogManager.getLogger(GenericLatexSemanticEnhancer.class.getName());
    /**
     * Constructs a new instance of the class
     */
    public GenericLatexSemanticEnhancer() {}

    /**
     * Generates a semantic enhanced document for the given string of a document.
     * The string should be a single document ont multiple documents! If you want to provide
     * multi documents, e.g., a wiki document with multi-pages use
     * {@link #getSemanticEnhancedDocumentsFromWikitext(String)} instead.
     *
     * This class uses the {@link ContextAnalyzer} to determine the type of document.
     *
     * @param document the document
     * @return semantically enhanced document
     */
    public SemanticEnhancedDocument getSemanticEnhancedDocument(String document) {
        return this.getSemanticEnhancedDocument(ContextAnalyzer.getDocument(document));
    }

    /**
     * Generates a semantic enhanced document for the given document
     * @param document the document
     * @return semantically enhanced document
     */
    public SemanticEnhancedDocument getSemanticEnhancedDocument(Document document) {
        MLPDependencyGraph annotatedGraph = document.getMOIDependencyGraph();
        return new SemanticEnhancedDocument(document.getTitle(), annotatedGraph);
    }

    @Override
    public MOIPresentations enhanceGenericLaTeX(String context, String latex, String dlmfLabel) throws ParseException {
        Document document = ContextAnalyzer.getDocument(context);
        MOINode<MOIAnnotation> annotatedMoiNode = document.getAnnotatedMOINode(latex);
        return new MOIPresentations(annotatedMoiNode);
    }

    public List<SemanticEnhancedDocument> getSemanticEnhancedDocumentsFromWikitext(String context) {
        List<String> pages = new LinkedList<>();
        pages.add(context);
        return getSemanticEnhancedDocumentsFromWikitext(pages);
    }

    public List<SemanticEnhancedDocument> getSemanticEnhancedDocumentsFromWikitext(Path filePath) throws IOException {
        String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        return getSemanticEnhancedDocumentsFromWikitext(fileContent);
    }

    public List<SemanticEnhancedDocument> getSemanticEnhancedDocumentsFromWikitext(Collection<String> contents) {
        WikiTextPageExtractorMapper pageMapper = new WikiTextPageExtractorMapper();
        return contents.stream()
                .parallel()
                .flatMap( pageMapper::streamFlatMap )
                .map( WikitextDocument::new )
                .map( this::getSemanticEnhancedDocument )
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
//        Path p = Paths.get("/mnt/share/data/wikipedia/dlmf-template-pages-26-11-2020.xml");
//        Path p = Paths.get("BesselFunction.xml");
        Path p = Paths.get("Jacobi_polynomials.xml");
        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        List<SemanticEnhancedDocument> docs = enhancer.getSemanticEnhancedDocumentsFromWikitext(p);

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
        
        // TODO run experiment on entire dataset

        // TODO NEXT: WRITE THE DAMN PAPER DUDE
    }
}
