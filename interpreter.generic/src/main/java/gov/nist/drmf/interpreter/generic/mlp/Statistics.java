package gov.nist.drmf.interpreter.generic.mlp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("unused")
public class Statistics {
    @JsonIgnore
    private static final Logger LOG = LogManager.getLogger(Statistics.class.getName());

    private int numberOfDocs;

    private int numberOfFormulae;

    private int docsWithoutFormulae;

    private int numberOfExistingSemanticTranslations;

    private int numberOfTranslationsWithConfidenceScoreOverZero;

    private int numberOfSemanticGenericDifferenceTranslations;

    private int numberOfTranslationsToAtLeastOneCAS;

    private int numberOfShouldNotBeEvaluatedExpressions;

    private int numberOfTranslationsToMaple;

    private int numberOfTranslationsToMathematica;

    private int mapleNumberOfSkippedTests;
    private int mapleNumberOfAbortedTests;
    private int mapleNumberOfStartedSymbolicTests;
    private int mapleNumberOfStartedNumericTests;
    private int mapleNumberOfSuccessfulSymbolicTests;
    private int mapleNumberOfSuccessfulNumericTests;
    private int mapleNumberOfFailedSymbolicTests;
    private int mapleNumberOfFailedNumericTests;
    private int mapleNumberOfErrorSymbolicTests;
    private int mapleNumberOfErrorNumericTests;

    private int mathematicaNumberOfSkippedTests;
    private int mathematicaNumberOfAbortedTests;
    private int mathematicaNumberOfStartedSymbolicTests;
    private int mathematicaNumberOfStartedNumericTests;
    private int mathematicaNumberOfSuccessfulSymbolicTests;
    private int mathematicaNumberOfSuccessfulNumericTests;
    private int mathematicaNumberOfFailedSymbolicTests;
    private int mathematicaNumberOfFailedNumericTests;
    private int mathematicaNumberOfErrorSymbolicTests;
    private int mathematicaNumberOfErrorNumericTests;

    public Statistics() {}

    public int getNumberOfDocs() {
        return numberOfDocs;
    }

    public void setNumberOfDocs(int numberOfDocs) {
        this.numberOfDocs = numberOfDocs;
    }

    public int getNumberOfFormulae() {
        return numberOfFormulae;
    }

    public void setNumberOfFormulae(int numberOfFormulae) {
        this.numberOfFormulae = numberOfFormulae;
    }

    public int getDocsWithoutFormulae() {
        return docsWithoutFormulae;
    }

    public void setDocsWithoutFormulae(int docsWithoutFormulae) {
        this.docsWithoutFormulae = docsWithoutFormulae;
    }

    public int getNumberOfExistingSemanticTranslations() {
        return numberOfExistingSemanticTranslations;
    }

    public void setNumberOfExistingSemanticTranslations(int numberOfExistingSemanticTranslations) {
        this.numberOfExistingSemanticTranslations = numberOfExistingSemanticTranslations;
    }

    public int getNumberOfTranslationsWithConfidenceScoreOverZero() {
        return numberOfTranslationsWithConfidenceScoreOverZero;
    }

    public void setNumberOfTranslationsWithConfidenceScoreOverZero(int numberOfTranslationsWithConfidenceScoreOverZero) {
        this.numberOfTranslationsWithConfidenceScoreOverZero = numberOfTranslationsWithConfidenceScoreOverZero;
    }

    public int getNumberOfSemanticGenericDifferenceTranslations() {
        return numberOfSemanticGenericDifferenceTranslations;
    }

    public void setNumberOfSemanticGenericDifferenceTranslations(int numberOfSemanticGenericDifferenceTranslations) {
        this.numberOfSemanticGenericDifferenceTranslations = numberOfSemanticGenericDifferenceTranslations;
    }

    public int getNumberOfTranslationsToAtLeastOneCAS() {
        return numberOfTranslationsToAtLeastOneCAS;
    }

    public void setNumberOfTranslationsToAtLeastOneCAS(int numberOfTranslationsToAtLeastOneCAS) {
        this.numberOfTranslationsToAtLeastOneCAS = numberOfTranslationsToAtLeastOneCAS;
    }

    public int getNumberOfTranslationsToMaple() {
        return numberOfTranslationsToMaple;
    }

    public void setNumberOfTranslationsToMaple(int numberOfTranslationsToMaple) {
        this.numberOfTranslationsToMaple = numberOfTranslationsToMaple;
    }

    public int getNumberOfTranslationsToMathematica() {
        return numberOfTranslationsToMathematica;
    }

    public void setNumberOfTranslationsToMathematica(int numberOfTranslationsToMathematica) {
        this.numberOfTranslationsToMathematica = numberOfTranslationsToMathematica;
    }

    public int getMapleNumberOfSuccessfulSymbolicTests() {
        return mapleNumberOfSuccessfulSymbolicTests;
    }

    public void setMapleNumberOfSuccessfulSymbolicTests(int mapleNumberOfSuccessfulSymbolicTests) {
        this.mapleNumberOfSuccessfulSymbolicTests = mapleNumberOfSuccessfulSymbolicTests;
    }

    public int getMapleNumberOfSuccessfulNumericTests() {
        return mapleNumberOfSuccessfulNumericTests;
    }

    public void setMapleNumberOfSuccessfulNumericTests(int mapleNumberOfSuccessfulNumericTests) {
        this.mapleNumberOfSuccessfulNumericTests = mapleNumberOfSuccessfulNumericTests;
    }

    public int getMathematicaNumberOfSuccessfulSymbolicTests() {
        return mathematicaNumberOfSuccessfulSymbolicTests;
    }

    public void setMathematicaNumberOfSuccessfulSymbolicTests(int mathematicaNumberOfSuccessfulSymbolicTests) {
        this.mathematicaNumberOfSuccessfulSymbolicTests = mathematicaNumberOfSuccessfulSymbolicTests;
    }

    public int getMathematicaNumberOfSuccessfulNumericTests() {
        return mathematicaNumberOfSuccessfulNumericTests;
    }

    public void setMathematicaNumberOfSuccessfulNumericTests(int mathematicaNumberOfSuccessfulNumericTests) {
        this.mathematicaNumberOfSuccessfulNumericTests = mathematicaNumberOfSuccessfulNumericTests;
    }

    public int getMapleNumberOfStartedSymbolicTests() {
        return mapleNumberOfStartedSymbolicTests;
    }

    public void setMapleNumberOfStartedSymbolicTests(int mapleNumberOfStartedSymbolicTests) {
        this.mapleNumberOfStartedSymbolicTests = mapleNumberOfStartedSymbolicTests;
    }

    public int getMapleNumberOfStartedNumericTests() {
        return mapleNumberOfStartedNumericTests;
    }

    public void setMapleNumberOfStartedNumericTests(int mapleNumberOfStartedNumericTests) {
        this.mapleNumberOfStartedNumericTests = mapleNumberOfStartedNumericTests;
    }

    public int getMathematicaNumberOfStartedSymbolicTests() {
        return mathematicaNumberOfStartedSymbolicTests;
    }

    public void setMathematicaNumberOfStartedSymbolicTests(int mathematicaNumberOfStartedSymbolicTests) {
        this.mathematicaNumberOfStartedSymbolicTests = mathematicaNumberOfStartedSymbolicTests;
    }

    public int getMathematicaNumberOfStartedNumericTests() {
        return mathematicaNumberOfStartedNumericTests;
    }

    public void setMathematicaNumberOfStartedNumericTests(int mathematicaNumberOfStartedNumericTests) {
        this.mathematicaNumberOfStartedNumericTests = mathematicaNumberOfStartedNumericTests;
    }

    public int getMapleNumberOfFailedSymbolicTests() {
        return mapleNumberOfFailedSymbolicTests;
    }

    public void setMapleNumberOfFailedSymbolicTests(int mapleNumberOfFailedSymbolicTests) {
        this.mapleNumberOfFailedSymbolicTests = mapleNumberOfFailedSymbolicTests;
    }

    public int getMapleNumberOfFailedNumericTests() {
        return mapleNumberOfFailedNumericTests;
    }

    public void setMapleNumberOfFailedNumericTests(int mapleNumberOfFailedNumericTests) {
        this.mapleNumberOfFailedNumericTests = mapleNumberOfFailedNumericTests;
    }

    public int getMathematicaNumberOfFailedSymbolicTests() {
        return mathematicaNumberOfFailedSymbolicTests;
    }

    public void setMathematicaNumberOfFailedSymbolicTests(int mathematicaNumberOfFailedSymbolicTests) {
        this.mathematicaNumberOfFailedSymbolicTests = mathematicaNumberOfFailedSymbolicTests;
    }

    public int getMathematicaNumberOfFailedNumericTests() {
        return mathematicaNumberOfFailedNumericTests;
    }

    public void setMathematicaNumberOfFailedNumericTests(int mathematicaNumberOfFailedNumericTests) {
        this.mathematicaNumberOfFailedNumericTests = mathematicaNumberOfFailedNumericTests;
    }

    public int getMapleNumberOfErrorSymbolicTests() {
        return mapleNumberOfErrorSymbolicTests;
    }

    public void setMapleNumberOfErrorSymbolicTests(int mapleNumberOfErrorSymbolicTests) {
        this.mapleNumberOfErrorSymbolicTests = mapleNumberOfErrorSymbolicTests;
    }

    public int getMapleNumberOfErrorNumericTests() {
        return mapleNumberOfErrorNumericTests;
    }

    public void setMapleNumberOfErrorNumericTests(int mapleNumberOfErrorNumericTests) {
        this.mapleNumberOfErrorNumericTests = mapleNumberOfErrorNumericTests;
    }

    public int getMathematicaNumberOfErrorSymbolicTests() {
        return mathematicaNumberOfErrorSymbolicTests;
    }

    public void setMathematicaNumberOfErrorSymbolicTests(int mathematicaNumberOfErrorSymbolicTests) {
        this.mathematicaNumberOfErrorSymbolicTests = mathematicaNumberOfErrorSymbolicTests;
    }

    public int getMathematicaNumberOfErrorNumericTests() {
        return mathematicaNumberOfErrorNumericTests;
    }

    public void setMathematicaNumberOfErrorNumericTests(int mathematicaNumberOfErrorNumericTests) {
        this.mathematicaNumberOfErrorNumericTests = mathematicaNumberOfErrorNumericTests;
    }

    public int getNumberOfShouldNotBeEvaluatedExpressions() {
        return numberOfShouldNotBeEvaluatedExpressions;
    }

    public void setNumberOfShouldNotBeEvaluatedExpressions(int numberOfShouldNotBeEvaluatedExpressions) {
        this.numberOfShouldNotBeEvaluatedExpressions = numberOfShouldNotBeEvaluatedExpressions;
    }

    public int getMapleNumberOfSkippedTests() {
        return mapleNumberOfSkippedTests;
    }

    public void setMapleNumberOfSkippedTests(int mapleNumberOfSkippedTests) {
        this.mapleNumberOfSkippedTests = mapleNumberOfSkippedTests;
    }

    public int getMapleNumberOfAbortedTests() {
        return mapleNumberOfAbortedTests;
    }

    public void setMapleNumberOfAbortedTests(int mapleNumberOfAbortedTests) {
        this.mapleNumberOfAbortedTests = mapleNumberOfAbortedTests;
    }

    public int getMathematicaNumberOfSkippedTests() {
        return mathematicaNumberOfSkippedTests;
    }

    public void setMathematicaNumberOfSkippedTests(int mathematicaNumberOfSkippedTests) {
        this.mathematicaNumberOfSkippedTests = mathematicaNumberOfSkippedTests;
    }

    public int getMathematicaNumberOfAbortedTests() {
        return mathematicaNumberOfAbortedTests;
    }

    public void setMathematicaNumberOfAbortedTests(int mathematicaNumberOfAbortedTests) {
        this.mathematicaNumberOfAbortedTests = mathematicaNumberOfAbortedTests;
    }

    @JsonIgnore
    public void addDocument(SemanticEnhancedDocument sed) {
        if ( sed == null ) return;

        numberOfDocs++;
        numberOfFormulae += sed.getFormulae().size();
        if ( sed.getFormulae().isEmpty() ) docsWithoutFormulae++;

        sed.getFormulae().forEach(this::addSingleMoi);
    }

    @JsonIgnore
    public void addSingleMoi(MOIPresentations moi) {
        if ( moi == null ) return;

        if ( moi.getSemanticLatex() != null && !moi.getSemanticLatex().isBlank() )
            numberOfExistingSemanticTranslations++;
        else return;

        if ( EvaluationSkipper.shouldNotBeEvaluated(moi.getSemanticLatex()) )
            numberOfShouldNotBeEvaluatedExpressions++;

        if ( moi.getScore() != null && moi.getScore() > 0 ) numberOfTranslationsWithConfidenceScoreOverZero++;

        String gen = moi.getGenericLatex();
        String sem = moi.getSemanticLatex();
        if ( !gen.equals(sem) ) numberOfSemanticGenericDifferenceTranslations++;

        Map<String, CASResult> casResults = moi.getCasRepresentations();
        if ( casResults == null || casResults.isEmpty() ) return;
        else numberOfTranslationsToAtLeastOneCAS++;

        CASResult mapleRes = casResults.get(Keys.KEY_MAPLE);
        CASResult matheRes = casResults.get(Keys.KEY_MATHEMATICA);

        if ( mapleRes != null ) {
            String trans = mapleRes.getCasRepresentation();
            if ( trans != null && !trans.isBlank() ) numberOfTranslationsToMaple++;
            updateCasResult(mapleRes.getSymbolicResults(), mapleRes.getNumericResults(), true);
        }

        if ( matheRes != null ) {
            String trans = matheRes.getCasRepresentation();
            if ( trans != null && !trans.isBlank() ) numberOfTranslationsToMathematica++;
            updateCasResult(matheRes.getSymbolicResults(), matheRes.getNumericResults(), false);
        }
    }

    @JsonIgnore
    private void updateCasResult(ITestResultCounter sym, ITestResultCounter num, boolean maple) {
//        if ( sym != null || num != null ) {
//            if ( maple ) mapleNumberOfStartedTests++;
//            else mathematicaNumberOfStartedTests++;
//        }

        if ( sym != null ) {
            switch ( sym.overallResult() ) {
                case SKIPPED: // nothing happen
                    break;
                case SUCCESS:
                    if ( maple ) {
                        mapleNumberOfSuccessfulSymbolicTests++;
                        mapleNumberOfStartedSymbolicTests++;
                    }
                    else {
                        mathematicaNumberOfSuccessfulSymbolicTests++;
                        mathematicaNumberOfStartedSymbolicTests++;
                    }
                    break;
                case FAILURE:
                    if ( maple ) {
                        mapleNumberOfFailedSymbolicTests++;
                        mapleNumberOfStartedSymbolicTests++;
                    }
                    else {
                        mathematicaNumberOfFailedSymbolicTests++;
                        mathematicaNumberOfStartedSymbolicTests++;
                    }
                    break;
                case ERROR:
                    if ( maple ) {
                        mapleNumberOfErrorSymbolicTests++;
                        mapleNumberOfStartedSymbolicTests++;
                    }
                    else {
                        mathematicaNumberOfErrorSymbolicTests++;
                        mathematicaNumberOfStartedSymbolicTests++;
                    }
                    break;
            }
        }

        if ( num != null ) {
            switch ( num.overallResult() ) {
                case SKIPPED:
                    if ( num.wasAborted() ) {
                        if (maple) {
                            mapleNumberOfAbortedTests++;
                            mapleNumberOfStartedNumericTests++;
                        } else {
                            mathematicaNumberOfAbortedTests++;
                            mathematicaNumberOfStartedNumericTests++;
                        }
                    } else {
                        if ( maple ) {
                            mapleNumberOfSkippedTests++;
                        } else {
                            mathematicaNumberOfSkippedTests++;
                        }
                        // not counting for actual performed test cases
                    }
                    break;
                case SUCCESS:
                    if ( maple ) {
                        mapleNumberOfSuccessfulNumericTests++;
                        mapleNumberOfStartedNumericTests++;
                    }
                    else {
                        mathematicaNumberOfSuccessfulNumericTests++;
                        mathematicaNumberOfStartedNumericTests++;
                    }
                    break;
                case FAILURE:
                    if ( maple ) {
                        mapleNumberOfFailedNumericTests++;
                        mapleNumberOfStartedNumericTests++;
                    }
                    else {
                        mathematicaNumberOfFailedNumericTests++;
                        mathematicaNumberOfStartedNumericTests++;
                    }
                    break;
                case ERROR:
                    if ( maple ) {
                        mapleNumberOfStartedNumericTests++;
                        mapleNumberOfErrorNumericTests++;
                    }
                    else {
                        mathematicaNumberOfErrorNumericTests++;
                        mathematicaNumberOfStartedNumericTests++;
                    }
                    break;
            }
        }
    }

    @JsonIgnore
    public static Statistics analyze(Collection<SemanticEnhancedDocument> sed) {
        LOG.info("Start analyzing all documents");
        Instant start = Instant.now();
        Statistics stats = new Statistics();
        sed.forEach(stats::addDocument);
        LOG.info("Finished analyzing all documents [" + Duration.between(start, Instant.now()).toString() + "]");
        return stats;
    }

    @JsonIgnore
    public static Statistics analyze(Path path) throws IOException {
        LOG.info("Loading semantic enhanced documents ...");
        Instant start = Instant.now();
        List<SemanticEnhancedDocument> docs = SemanticEnhancedDocument.deserialize(path);
        LOG.info("Finished document loading [" + Duration.between(start, Instant.now()).toString() + "]");
        return analyze(docs);
    }

    @JsonIgnore
    public static void main(String[] args) throws IOException {
        Statistics stats = analyze(Paths.get("/mnt/share/data/wikipedia/Results/pagesComputed"));

        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        String statString = mapper.writeValueAsString(stats);
        System.out.println(statString);
    }
}
