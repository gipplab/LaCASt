package gov.nist.drmf.interpreter.generic.mlp;

/**
 * @author Andre Greiner-Petter
 */
public class Statistics {

    private int numberOfDocs;

    private int numberOfFormulae;

    private int docsWithoutFormulae;

    private int numberOfExistingSemanticTranslations;

    private int numberOfTranslationsWithConfidenceScoreOverZero;

    private int numberOfSemanticGenericDifferenceTranslations;

    private int numberOfTranslationsToAtLeastOneCAS;

    private int numberOfTranslationsToMaple;

    private int numberOfTranslationsToMathematica;

    private int mapleNumberOfSuccessfulSymbolicTests;
    private int mapleNumberOfSuccessfulNumericTests;

    private int mathematicaNumberOfSuccessfulSymbolicTests;
    private int mathematicaNumberOfSuccessfulNumericTests;

    public Statistics() {}

}
