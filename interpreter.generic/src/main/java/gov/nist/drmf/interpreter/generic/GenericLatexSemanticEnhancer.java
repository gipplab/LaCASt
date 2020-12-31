package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextPageExtractorMapper;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.pojo.ComputationTask;
import gov.nist.drmf.interpreter.generic.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.interfaces.IGenericLatexSemanticEnhancerAPI;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.Document;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.generic.mlp.WikitextDocument;
import gov.nist.drmf.interpreter.generic.mlp.pojo.*;
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

    private final SemanticEnhancer semanticEnhancer;

    private final CASTranslators translators;

    /**
     * Constructs a new instance of the class
     */
    public GenericLatexSemanticEnhancer() {
        this.semanticEnhancer = new SemanticEnhancer();
        this.translators = new CASTranslators();
    }

    @Override
    public SemanticEnhancedDocument generateAnnotatedDocument(String context) {
        Document document = ContextAnalyzer.getDocument(context);
        return getSemanticEnhancedDocument(document);
    }

    @Override
    public SemanticEnhancedDocument appendTranslationsToDocument(SemanticEnhancedDocument annotatedDocument) throws MinimumRequirementNotFulfilledException {
        checkRank( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED, annotatedDocument );

        MLPDependencyGraph graph = new MLPDependencyGraph(annotatedDocument.getFormulae());
        for ( MOIPresentations formula : annotatedDocument.getFormulae() ) {
            appendTranslationToMOI(formula, graph.getNode(formula.getId()));
        }

        return annotatedDocument;
    }

    @Override
    public SemanticEnhancedDocument appendCASComputationsToDocument(SemanticEnhancedDocument semanticDocument) throws MinimumRequirementNotFulfilledException {
        checkRank( SemanticEnhancedAnnotationStatus.TRANSLATED, semanticDocument );

        return null;
    }

    @Override
    public MOIPresentations generateMOIPresentationFromDocument(SemanticEnhancedDocument annotatedDocument, String formula) throws MinimumRequirementNotFulfilledException, ParseException {
        checkRank( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED, annotatedDocument );

        String id = MathTag.getID(formula);
        MLPDependencyGraph graph = new MLPDependencyGraph(annotatedDocument.getFormulae());
        MOIPresentations moi = annotatedDocument.getFormulae().stream().filter( m -> m.getId().equals(id) ).findFirst()
                .orElse( null );
        if ( moi == null ) {
            moi = semanticEnhancer.generateAnnotatedLatex(formula, graph);
        }

        appendTranslationToMOI(moi, graph.getNode(id));
        return moi;
    }

    private void appendTranslationToMOI(MOIPresentations moi, MOINode<MOIAnnotation> node) {
        // node cannot be null unless something serious broke before...
        try {
            semanticEnhancer.appendSemanticLatex( moi, node );
            translators.getTranslators().forEach( (k, t) -> {
                try { semanticEnhancer.appendCASRepresentation(moi, k, t); }
                catch (TranslationException te) {
                    LOG.warn(te.toString());
                }
            } );
        } catch (ParseException p) {
            LOG.error("Unable to generate semantic latex due to a parsing error for " + moi.getId() + ": " + moi.getGenericLatex(), p);
        }
    }

    @Override
    public MOIPresentations computeMOI(MOIPresentations translatedMOI) {
        return null;
    }

    @Override
    public CASResult computeMOI(MOIPresentations translatedMOI, String cas) {
        return null;
    }

    @Override
    public CASResult computeMOI(MOIPresentations translatedMOI, String cas, ComputationTask task) {
        return null;
    }

    private void checkRank(SemanticEnhancedAnnotationStatus min, SemanticEnhancedDocument sed) throws MinimumRequirementNotFulfilledException {
        if ( !sed.getSemanticState().hasPassed(min) ) throw new MinimumRequirementNotFulfilledException(min, sed.getSemanticState());
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
