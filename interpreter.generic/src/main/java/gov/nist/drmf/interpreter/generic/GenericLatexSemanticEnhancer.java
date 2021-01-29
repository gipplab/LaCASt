package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextPageExtractorMapper;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.pojo.ComputationTask;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.generic.interfaces.IGenericLatexSemanticEnhancerAPI;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.Document;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.generic.mlp.WikitextDocument;
import gov.nist.drmf.interpreter.generic.mlp.cas.CASTranslators;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is the API endpoint to semantically enhance math in entire documents.
 *
 * @author Andre Greiner-Petter
 */
public class GenericLatexSemanticEnhancer implements IGenericLatexSemanticEnhancerAPI {
    private static final Logger LOG = LogManager.getLogger(GenericLatexSemanticEnhancer.class.getName());

    private final SemanticEnhancer semanticEnhancer;

    /**
     * Constructs a new instance of the class
     */
    public GenericLatexSemanticEnhancer() {
        this(GenericLacastConfig.getDefaultConfig());
    }

    /**
     * Constructs a new instance of the class
     */
    public GenericLatexSemanticEnhancer(GenericLacastConfig config) {
        this.semanticEnhancer = new SemanticEnhancer(config);
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
        CASTranslators casTranslators = new CASTranslators();
        for ( MOIPresentations formula : annotatedDocument.getFormulae() ) {
            appendTranslationToMOI(formula, graph.getNode(formula.getId()), casTranslators);
        }

        return annotatedDocument;
    }

    public MOIPresentations appendTranslationToMoi(SemanticEnhancedDocument context, MOIPresentations moi) {
        context.requires(SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED);

        MLPDependencyGraph graph = new MLPDependencyGraph(context.getFormulae());
        appendTranslationToMOI(moi, graph.getNode(moi.getId()), new CASTranslators());
        return moi;
    }

    public SemanticEnhancedDocument appendCASComputationsIfChanged(SemanticEnhancedDocument sed) {
        if ( sed == null || sed.getFormulae() == null ) return sed;

        MLPDependencyGraph graph = new MLPDependencyGraph(sed.getFormulae());
        CASTranslators casTranslators = new CASTranslators();
        int max = sed.getFormulae().size();
        int counter = 0;
        for ( MOIPresentations formula : sed.getFormulae() ) {
            counter++;
            String oldSemantic = formula.getSemanticLatex();
            LOG.info("Update translation for " + counter + "/" + max);
            appendTranslationToMOI(formula, graph.getNode(formula.getId()), casTranslators);
            String newSemantic = formula.getSemanticLatex();

            if ( oldSemantic == null || !oldSemantic.equals(newSemantic) ) {
                LOG.info("Append CAS computation for " + counter + "/" + max);
                try {
                    semanticEnhancer.appendComputationResults(formula);
                }
                catch ( Exception e ) {
                    LOG.warn("Unable to append computation for " + formula.getSemanticLatex());
                }
            }
        }

        return sed;
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

        LOG.info("Requesting MOI Presentation for given formula. Preprocessing: " + formula);
        formula = TeXPreProcessor.preProcessingTeX(formula);
        LOG.info("Pre-processed formula: " + formula);

        String id = MathTag.getID(formula);
        MLPDependencyGraph graph = new MLPDependencyGraph(annotatedDocument.getFormulae());
        MOIPresentations moi = annotatedDocument.getFormulae().stream().filter( m -> m.getId().equals(id) ).findFirst()
                .orElse( null );
        if ( moi == null ) {
            moi = semanticEnhancer.generateAnnotatedLatex(formula, graph);
        }

        appendTranslationToMOI(moi, graph.getNode(id), new CASTranslators());
        return moi;
    }

    private void appendSemanticLatexToMOI(MOIPresentations moi, MOINode<MOIAnnotation> node) {
        try {
            semanticEnhancer.appendSemanticLatex( moi, node );
        } catch (ParseException e) {
            LOG.error("Unable to generate semantic latex due to a parsing error for " + moi.getId() + ": " + moi.getGenericLatex(), e);
        }
    }

    private void appendSemanticLatexToCasTranslationToMOI(MOIPresentations moi, CASTranslators translators) {
        translators.getTranslators().forEach( (k, t) -> {
            try { semanticEnhancer.appendCASRepresentation(moi, k, t); }
            catch (TranslationException te) {
                LOG.warn(te.toString());
            }
        } );
    }

    private void appendTranslationToMOI(MOIPresentations moi, MOINode<MOIAnnotation> node, CASTranslators translators) {
        // node cannot be null unless something serious broke before...
        appendSemanticLatexToMOI(moi, node);
        appendSemanticLatexToCasTranslationToMOI(moi, translators);
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

    private static String makeKey(SemanticEnhancedDocument sed, MOIPresentations moi) {
        return sed.getTitle() + "-" + moi.getId();
    }

    private static void translateGoldenOnly() throws IOException {
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        Path p = Paths.get("./misc/Results/Wikipedia/gold-data.json");
        Path annotedP = Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-ANNOTATED.json");

        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();

        Instant start = Instant.now();
        SemanticEnhancedDocument[] goldenDocs = mapper.readValue(p.toFile(), SemanticEnhancedDocument[].class);
        SemanticEnhancedDocument[] annotatedDocs = mapper.readValue(annotedP.toFile(), SemanticEnhancedDocument[].class);

        HashMap<String, MOIPresentations> goldenDocMoiMapping = new HashMap<>();
        for ( SemanticEnhancedDocument sed : goldenDocs ) {
            if ( sed == null ) continue;

            for ( MOIPresentations moi : sed.getFormulae() ) {
                goldenDocMoiMapping.put( makeKey(sed, moi), moi );
            }
        }

        Set<String> requestMoiSet = goldenDocMoiMapping.keySet();
        for ( SemanticEnhancedDocument sed : annotatedDocs ) {
            if ( sed == null ) continue;

            for ( MOIPresentations moi : sed.getFormulae() ) {
                String key = makeKey(sed, moi);
                if ( !requestMoiSet.contains( key ) ) continue;

                moi = enhancer.appendTranslationToMoi( sed, moi );
                MOIPresentations goldenMoi = goldenDocMoiMapping.get(key);
                goldenMoi.setSemanticLatex( moi.getSemanticLatex() );
                goldenMoi.setCasRepresentations( moi.getCasRepresentations() );
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        LOG.warn("FINISHED entire document analysis... [" + elapsed.toString() + "]");

        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(goldenDocs);
        Files.writeString( Paths.get("./misc/Results/Wikipedia/gold-data-TRANSLATED.json"), serializedDoc );
    }

    private void setBaseline(SemanticEnhancedDocument sed) {
        if ( sed == null || sed.getFormulae().isEmpty() ) return;

        CASTranslators casTranslators = new CASTranslators();
        for ( MOIPresentations moi : sed.getFormulae() ) {
            moi.setSemanticLatex(moi.getGenericLatex());
            appendSemanticLatexToCasTranslationToMOI(moi, casTranslators);
        }
    }

    public static void main(String[] args) throws IOException {
//        translateGoldenOnly();

        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        Path p = Paths.get("/mnt/share/data/wikipedia/Results/gold-data.json");
        Path ref = Paths.get("/mnt/share/data/wikipedia/Results/pages/");

        SemanticEnhancedDocument[] goldDocs = mapper.readValue(p.toFile(), SemanticEnhancedDocument[].class);
        List<SemanticEnhancedDocument> docs = SemanticEnhancedDocument.deserialize(ref);

        Map<String, MOIPresentations> goldenMap = new HashMap<>();
        for ( SemanticEnhancedDocument doc : goldDocs ) {
            goldenMap.putAll(doc.getMoiMapping(s -> (doc.getTitle()+"-"+s)));
        }

        for ( SemanticEnhancedDocument doc : docs ) {
            for ( MOIPresentations moi : doc.getFormulae() ) {
                if ( goldenMap.containsKey( doc.getTitle() + "-" + moi.getId() ) ) {
                    MOIPresentations gold = goldenMap.get(doc.getTitle() + "-" + moi.getId());
                    gold.overwrite(moi);
                }
            }
        }

        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(goldDocs);
        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/gold-data-Updated.json"), serializedDoc );

//        Path annotedP = Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-ANNOTATED.json");
//        Path p = Paths.get("/mnt/share/data/wikipedia/dlmf-template-pages-26-11-2020.xml");
//        Path p = Paths.get("BesselFunction.xml");
//        Path p = Paths.get("Jacobi_polynomials.xml");
//        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
//
//        Instant start = Instant.now();
//        SemanticEnhancedDocument goldenDoc = mapper.readValue(p.toFile(), SemanticEnhancedDocument.class);
//        List<SemanticEnhancedDocument> docs = SemanticEnhancedDocument.deserialize(ref);
//        SemanticEnhancedDocument[] docs = mapper.readValue(annotedP.toFile(), SemanticEnhancedDocument[].class);

//        LOG.info("Finished loading documents from annotated file.");
//        LOG.warn("START GENERATING ANNOTATED DOCUMENT");
//        List<SemanticEnhancedDocument> docs = enhancer.getSemanticEnhancedDocumentsFromWikitext(p);
//        SemanticEnhancedDocument sed = enhancer.generateAnnotatedDocument(p);
//        LOG.warn("FINISHED GENERATING ANNOTATED DOCUMENT");
//        LOG.warn("START TRANSLATING ANNOTATED DOCUMENT");
//        sed = enhancer.appendTranslationsToDocument(sed);
//        LOG.warn("FINISHED TRANSLATING ANNOTATED DOCUMENT");
//        LOG.warn("START COMPUTING TRANSLATED DOCUMENT");
//        sed = enhancer.appendCASComputationsToDocument(sed);
//        LOG.warn("FINISHED COMPUTING TRANSLATED DOCUMENT");

//        for ( int i = 0; i < docs.size(); i++ ) {
//            SemanticEnhancedDocument sed = docs.get(i);
//
//            if ( Files.exists(
//                    Paths.get("/mnt/share/data/wikipedia/Results/" +
//                            "baselinePages/" + sed.getTitle().replaceAll(" ", "_").replaceAll("/","-") + ".json")
//            ) ) continue;
//
//            if ( sed == null ) continue;
//            if ( sed.getFormulae() == null || sed.getFormulae().isEmpty() ) {
//                LOG.warn("The document " + sed.getTitle() + " does not contain any formulae. Remove it!");
////                docs[i] = null;
//                continue;
//            }
//
//            LOG.warn("Translating and Evaluating document: " + sed.getTitle());
////            enhancer.appendCASComputationsIfChanged(sed);
//            enhancer.setBaseline(sed);
//            enhancer.appendCASComputationsToDocument(sed);
//            String serializedSingleDoc = mapper.writer(prettyPrinter).writeValueAsString(sed);
//            Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/" +
//                    "baselinePages/" + sed.getTitle().replaceAll(" ", "_").replaceAll("/","-") + ".json"), serializedSingleDoc );
//        }
//
//        Duration elapsed = Duration.between(start, Instant.now());
//        LOG.warn("FINISHED entire document analysis... [" + elapsed.toString() + "]");

//        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(docs);
//        String serializedDoc = mapper.writer(prettyPrinter).writeValueAsString(sed);
//        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-TRANSLATED.json"), serializedDoc );
//        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-28-01-2021-COMPUTED.json"), serializedDoc );
//        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/gold-data-TRANSLATED.json"), serializedDoc );
//        Files.writeString( Paths.get("Result-Bessel.json"), serializedDoc );
//        Files.writeString( Paths.get("ResultsFULL.json"), serializedDoc );

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
