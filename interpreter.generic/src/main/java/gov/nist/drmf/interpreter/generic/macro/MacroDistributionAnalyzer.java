package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
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
public class MacroDistributionAnalyzer {
    private static final Logger LOG = LogManager.getLogger(MacroDistributionAnalyzer.class.getName());

    private Map<String, MacroCounter> dist;

    private static MacroDistributionAnalyzer instance;

    public static MacroDistributionAnalyzer getStandardInstance() {
        if ( instance == null ) {
            instance = new MacroDistributionAnalyzer();
            try {
                instance.load( GlobalPaths.PATH_SEMANTIC_MACROS_DISTRIBUTIONS );
            } catch (IOException e) {
                LOG.error("Unable to load standard distribution of dlmf macros. All values set to 0.", e);
            }
        }
        return instance;
    }

    public MacroDistributionAnalyzer() {
        this.dist = new HashMap<>();
    }

    public void load(Path p) throws IOException {
        LOG.info("Load standard reference for DLMF macro distributions.");
        String serializedData = Files.readString(p);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, MacroCounter>> typeRef =
                new TypeReference<HashMap<String, MacroCounter>>() {};
        dist = mapper.readValue(serializedData, typeRef);
        LOG.debug("Successfully initiated standard distributions of DLMF macros.");
    }

    public void analyze(Path path) throws IOException {
        final SemanticMLPWrapper mlpWrapper = SemanticMLPWrapper.getStandardInstance();

        LOG.info("Start analyzing lines in database.");
        int[] lines = new int[]{0};

        Files.lines(path)
                .sequential()
                .peek( l -> {
                    lines[0]++;
                    LOG.debug("Analyzing line " + lines[0]);
                })
                .filter( l -> !l.contains("Warning") )
                .map( l -> l.split("\\\\url")[0] )
                .map( l -> {
                    try {
                        return mlpWrapper.parse(l);
                    } catch (ParseException e) {
                        LOG.debug("Unable to parse line: " + l);
                        return null;
                    }
                })
                .filter( Objects::nonNull )
                .forEach( this::analyze );

        LOG.info("Finished analyzing all macros. Encountered " + this.dist.size() + " macros.");
    }

    public void analyze(PrintablePomTaggedExpression pte) {
        LinkedList<PrintablePomTaggedExpression> remainingElementsList = new LinkedList<>();
        remainingElementsList.add(pte);
        analyzeInternally(remainingElementsList);
    }

    private void analyzeInternally(List<PrintablePomTaggedExpression> remainingPte) {
        while ( !remainingPte.isEmpty() ) {
            PrintablePomTaggedExpression next = remainingPte.remove(0);
            if ( MathTermUtility.isDLMFMacro(next.getRoot()) ) analyzeDlmfMacro(next);
            remainingPte.addAll( next.getPrintableComponents() );
        }
    }

    private void analyzeDlmfMacro(PrintablePomTaggedExpression macro) {
        MathTerm macroTerm = macro.getRoot();
        LOG.trace("Encountered a DLMF macro " + macroTerm.getTermText());
        MacroCounter counter = getMacroCounter(macroTerm.getTermText());
        // always first, increment the macro counter, no matter what happens next
        counter.incrementMacroCounter();

        FeatureSet featureSet = macroTerm.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if ( featureSet == null ) return;

        try {
            int numOfParams = Integer.parseInt(DLMFFeatureValues.NUMBER_OF_PARAMETERS.getFeatureValue(featureSet, null));
            analyzeFollowingNodesOfMacro( counter, macro.getNextSibling(), numOfParams );
        } catch (NullPointerException | NumberFormatException e) {} // simply ignore that
    }

    private void analyzeFollowingNodesOfMacro(
            MacroCounter counter,
            PomTaggedExpression sibling,
            int numOfParams
    ) {
        if ( sibling == null ) return;

        Brackets bracket = Brackets.getBracket(sibling);
        if ( Brackets.left_brackets.equals(bracket) ) {
            sibling = untilClosingBracket(counter, sibling);
        }

        for ( int i = 0; i < numOfParams; i++ ) {
            if ( sibling == null ) {
                LOG.warn("Reached the end of siblings before we passed all number of parameters. Something went wrong");
                return;
            }
            sibling = sibling.getNextSibling();
        }

        // skip carets before ats
        while ( sibling != null && MathTermUtility.equals( sibling.getRoot(), MathTermTags.caret )) {
            sibling = sibling.getNextSibling();
        }

        // now we should reached the @ symbols... lets count them
        int atCounter = 0;
        while ( sibling != null && MathTermUtility.isAt(sibling.getRoot()) ) {
            atCounter++;
            sibling = sibling.getNextSibling();
        }
        counter.incrementAtCounter(atCounter);
    }

    private PomTaggedExpression untilClosingBracket(MacroCounter counter, PomTaggedExpression sibling) {
        int open = 1;
        boolean containedOptionalElement = false;
        while ( open > 0 ) {
            sibling = sibling.getNextSibling();
            if ( sibling == null ) {
                LOG.warn("Unable to handle optional argument. Suddenly reached the end without ever closing the optional argument.");
                return null;
            }
            Brackets bracket = Brackets.getBracket(sibling);
            if ( Brackets.left_brackets.equals(bracket) ) open++;
            else if ( Brackets.right_brackets.equals(bracket) ) open--;
            else containedOptionalElement = true;
        }

        // slightly modified version. Quite often we see empty optional arguments. For example \FerresP[]{n}@{x}
        // this is equivalent to removing the optional brackets at all: \FerrersP{n}@{x}. Hence we count them only
        // if the optional argument was not empty.
        if ( containedOptionalElement )
            counter.incrementOptionalArgumentCounter();

        return sibling.getNextSibling();
    }

    public synchronized MacroCounter getMacroCounter(String macro) {
        return dist.computeIfAbsent( macro, MacroCounter::new );
    }

    public static void main(String[] args) throws IOException {
        MacroDistributionAnalyzer analyzer = new MacroDistributionAnalyzer();
        Path data = Paths.get("/mnt/share/data/Howard/together.txt");
        analyzer.analyze(data);

        LOG.info("Writing results to the output path.");
        Path out = GlobalPaths.PATH_SEMANTIC_MACROS_DISTRIBUTIONS;
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String serialized = mapper.writeValueAsString(analyzer.dist);
        Files.writeString(out, serialized);
    }
}
