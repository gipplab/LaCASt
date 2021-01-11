package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextPageExtractorMapper;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.pojo.*;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.interfaces.IGenericLatexSemanticEnhancerAPI;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.Document;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.generic.mlp.WikitextDocument;
import gov.nist.drmf.interpreter.generic.mlp.cas.CASTranslators;
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
import java.time.Duration;
import java.time.Instant;
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

    public SemanticEnhancedDocument generateAnnotatedDocument(Path filePath) throws IOException {
        String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        return generateAnnotatedDocument(fileContent);
    }

    @Override
    public SemanticEnhancedDocument appendTranslationsToDocument(SemanticEnhancedDocument annotatedDocument) throws MinimumRequirementNotFulfilledException {
        annotatedDocument.requires(SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED);

        MLPDependencyGraph graph = new MLPDependencyGraph(annotatedDocument.getFormulae());
        for ( MOIPresentations formula : annotatedDocument.getFormulae() ) {
            appendTranslationToMOI(formula, graph.getNode(formula.getId()));
        }

        return annotatedDocument;
    }

    @Override
    public SemanticEnhancedDocument appendCASComputationsToDocument(SemanticEnhancedDocument semanticDocument) throws MinimumRequirementNotFulfilledException {
        semanticDocument.requires(SemanticEnhancedAnnotationStatus.TRANSLATED);

        List<MOIPresentations> formulae = semanticDocument.getFormulae();
        for ( MOIPresentations moi : formulae ) {
            try {
                semanticEnhancer.appendComputationResults(moi);
            } catch ( MinimumRequirementNotFulfilledException m ) {
                // ignore it... because maybe this case could simply not be translated ;)
            }
        }

        return semanticDocument;
    }

    @Override
    public MOIPresentations generateMOIPresentationFromDocument(SemanticEnhancedDocument annotatedDocument, String formula) throws MinimumRequirementNotFulfilledException, ParseException {
        annotatedDocument.requires(SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED);

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
    public MOIPresentations computeMOI(MOIPresentations translatedMOI) throws MinimumRequirementNotFulfilledException {
        semanticEnhancer.appendComputationResults( translatedMOI );
        return translatedMOI;
    }

    @Override
    public CASResult computeMOI(MOIPresentations translatedMOI, String cas) throws MinimumRequirementNotFulfilledException {
        semanticEnhancer.appendComputationResults(translatedMOI, cas);
        return translatedMOI.getCasRepresentations().get(cas);
    }

    @Override
    public CASResult computeMOI(MOIPresentations translatedMOI, String cas, ComputationTask task) throws MinimumRequirementNotFulfilledException {
        translatedMOI.requires( SemanticEnhancedAnnotationStatus.TRANSLATED );
        CASResult casResult = translatedMOI.getCasResults(cas);
        switch ( task ) {
            case NUMERIC:
                NumericResult nr = semanticEnhancer.computeNumerically( translatedMOI.getSemanticLatex(), cas );
                casResult.setNumericResults(nr);
                break;
            case SYMBOLIC:
                SymbolicResult sr = semanticEnhancer.computeSymbolically( translatedMOI.getSemanticLatex(), cas );
                casResult.setSymbolicResults(sr);
        }

        return casResult;
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

//        List<SemanticEnhancedDocument> docs = enhancer.getSemanticEnhancedDocumentsFromWikitext(p);
        Instant start = Instant.now();
        LOG.warn("START GENERATING ANNOTATED DOCUMENT");
        SemanticEnhancedDocument sed = enhancer.generateAnnotatedDocument(p);
        LOG.warn("FINISHED GENERATING ANNOTATED DOCUMENT");
        LOG.warn("START TRANSLATING ANNOTATED DOCUMENT");
        sed = enhancer.appendTranslationsToDocument(sed);
        LOG.warn("FINISHED TRANSLATING ANNOTATED DOCUMENT");
        LOG.warn("START COMPUTING TRANSLATED DOCUMENT");
        sed = enhancer.appendCASComputationsToDocument(sed);
        LOG.warn("FINISHED COMPUTING TRANSLATED DOCUMENT");
        Duration elapsed = Duration.between(start, Instant.now());
        LOG.warn("FINISHED entire document analysis... [" + elapsed.toString() + "]");

        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

//        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(docs);
        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(sed);
//        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-03-12-2020.json"), serializedDoc );
//        Files.writeString( Paths.get("Result-Bessel.json"), serializedDoc );
        Files.writeString( Paths.get("ResultsFULL.json"), serializedDoc );

//        for ( SemanticEnhancedDocument doc : docs ) {
//            System.out.println("Document: " + doc.getTitle());
//            List<MOIPresentations> moi = doc.getFormulae();
//            for ( MOIPresentations m : moi ) {
//                System.out.println(m);
//            }
//        }
        
        // TODO run experiment on entire dataset

        // TODO NEXT: WRITE THE DAMN PAPER DUDE
    }
}
