package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class EvaluationHelper {
    private static final Logger LOG = LogManager.getLogger(EvaluationHelper.class.getName());

    private final ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
    private final DefaultPrettyPrinter prettyPrinter;

    private final Path path;

    public EvaluationHelper(Path path) {
        this.path = path;
        prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    }

    public String getString(SemanticEnhancedDocument[] docs) throws JsonProcessingException {
        return mapper.writer(prettyPrinter).writeValueAsString(docs);
    }

    public SemanticEnhancedDocument[] loadData() throws IOException {
        return mapper.readValue(path.toFile(), SemanticEnhancedDocument[].class);
    }

    public SemanticEnhancedDocument[] pickEquationsRandomlyAndJacobi(SemanticEnhancedDocument[] docs) {
        long oldFormulaeSize = Arrays.stream(docs).filter(Objects::nonNull).map( SemanticEnhancedDocument::getFormulae )
                .mapToLong(Collection::size).sum();

        Arrays.stream(docs)
                .filter(Objects::nonNull)
                .map(this::removeNonEquations)
                .filter(d -> !d.getFormulae().isEmpty())
                .forEach(this::pickRemainingRandomly);

        long newFormulaeSize = Arrays.stream(docs).filter(Objects::nonNull).map( SemanticEnhancedDocument::getFormulae )
                .mapToLong(Collection::size).sum();

        LOG.warn("Before removing and selecting the total number of formulae was: " + oldFormulaeSize);
        LOG.warn("After removing and selecting " + newFormulaeSize + " formulae remain");
        return docs;
    }

    private SemanticEnhancedDocument removeNonEquations(SemanticEnhancedDocument doc) {
        if ( doc.getTitle().equals("Jacobi polynomials") ) {
            LOG.debug("Skip removing equations from jacobi polynomials article");
            return doc;
        }
        List<MOIPresentations> formulae = doc.getFormulae();
        LOG.debug("Removing non equations from " + doc.getTitle() + ". Before had " + formulae.size() + " formulae");
        formulae.removeIf( this::remove );
        LOG.debug("After removing non-equations " + formulae.size() + " formulae remain");
        return doc;
    }

    private boolean remove(MOIPresentations moi) {
        String latex = moi.getGenericLatex();
        return latex.matches(".*(?:color|text).*") || !latex.contains("=");
    }

    private void pickRemainingRandomly(SemanticEnhancedDocument doc) {
        if ( doc.getTitle().equals("Jacobi polynomials") ) {
            LOG.debug("Skip pick randomly from jacobi polynomials article");
            return;
        }
        LOG.debug("Pick one equation randomly from " + doc.getTitle());
        List<MOIPresentations> formulae = doc.getFormulae();
        Random randomizer = new Random();
        int pickId = randomizer.nextInt(formulae.size());
        LOG.debug("Pick " + pickId + "/" + formulae.size());
        MOIPresentations pick = formulae.get(pickId);
        formulae.clear();
        formulae.add(pick);
    }

    private void analyzeStatisticsOfDoc(SemanticEnhancedDocument[] docs) {

    }

    public static void buildGoldenDataset() throws IOException {
        Path p = Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-TRANSLATED.json");
        EvaluationHelper helper = new EvaluationHelper(p);
        SemanticEnhancedDocument[] docs = helper.loadData();
        docs = helper.pickEquationsRandomlyAndJacobi(docs);

        String serializedDoc = helper.getString(docs);
        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/gold-data-otherSet.json"), serializedDoc );
    }

    public static void statisticsResults() throws IOException {
        Path p = Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-TRANSLATED.json");
        EvaluationHelper helper = new EvaluationHelper(p);
        SemanticEnhancedDocument[] docs = helper.loadData();
    }

    public static void main(String[] args) throws IOException {

    }

}
