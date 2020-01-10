package gov.nist.drmf.interpreter.evaluation.diff;

import com.maplesoft.externalcall.MapleException;
import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.MathematicaTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.Util;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalDifferencesAnalyzer {
    private static final Logger LOG = LogManager.getLogger(NumericalDifferencesAnalyzer.class.getName());

    private HashMap<String, LinkedList<SingleNumericEntity>> resultsMapMathematica;
    private HashMap<String, LinkedList<SingleNumericEntity>> resultsMapMaple;

    private static final Pattern mathLinePattern = Pattern.compile(
            "^(\\d+(?:-[a-z])?): (\\{\\{.*}})$"
    );

    private static final Pattern mathEntityPattern = Pattern.compile(
            "[ {]\\{(.*?), \\{(.*?)}}[,}]"
    );

    private static final Pattern mapleLinePattern = Pattern.compile(
            "^(\\d+(?:-[a-z])?): (\\[\\[.*]])$"
    );

    private static final Pattern mapleEntityPattern = Pattern.compile(
            "[ \\[]\\[(.*?), \\[(.*?)]][,\\]]"
    );

    private static final Pattern procedureNamePattern = Pattern.compile(
            "^([A-Za-z]+)\\[.*", Pattern.DOTALL
    );

    private IComputerAlgebraSystemEngine<Expr> mathematica;
    private ITranslator forwardTranslator;
    private ITranslator backwardTranslator;

    private String procedureName;

    public NumericalDifferencesAnalyzer() throws IOException, MapleException, ComputerAlgebraSystemEngineException {
        MathematicaTranslator m = new MathematicaTranslator();
        m.init();
        mathematica = m;

        SemanticLatexTranslator dlmfInterface = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        forwardTranslator = dlmfInterface;

        MapleInterface.init();
        MapleInterface mi = MapleInterface.getUniqueMapleInterface();
        backwardTranslator = mi;

        String procedure = Util.getProcedure(GlobalPaths.PATH_MATHEMATICA_DIFFERENCE_PROCEDURES);
        Matcher matcher = procedureNamePattern.matcher(procedure);
        if ( matcher.matches() ) {
            this.procedureName = matcher.group(1);
            mathematica.enterCommand(procedure);
        } else {
            throw new RuntimeException("Cannot extract procedure name from " +
                    GlobalPaths.PATH_MATHEMATICA_DIFFERENCE_PROCEDURES);
        }
    }

    public void init(Path mathematicaNumResultsPath, Path mapleNumResultsPath) throws IOException {
        resultsMapMathematica = new HashMap<>();
        resultsMapMaple = new HashMap<>();

        Files.walk(mathematicaNumResultsPath)
                .filter( p -> Files.isRegularFile(p) )
                .forEach( p -> fillMap(resultsMapMathematica, p, mathLinePattern, mathEntityPattern, true));

        Files.walk(mapleNumResultsPath)
                .filter( p -> Files.isRegularFile(p) )
                .forEach( p -> fillMap(resultsMapMaple, p, mapleLinePattern, mapleEntityPattern, false));
    }

    private static void fillMap(
            HashMap<String, LinkedList<SingleNumericEntity>> map,
            Path p,
            Pattern linePattern,
            Pattern entityPattern,
            boolean isMathematica
    ) {
        try {
            Files.lines(p)
                    .filter(l -> l.matches("^(\\d+(?:-[a-z])?): [{\\[].*") && !l.contains("Error"))
                    .forEach(l -> {
                        Matcher m = linePattern.matcher(l);
                        if (m.matches()) {
                            String id = m.group(1); // id
                            LinkedList<SingleNumericEntity> els = new LinkedList<>();
                            String values = m.group(2);
                            Matcher valM = entityPattern.matcher(values);
                            while (valM.find()) {
                                SingleNumericEntity sne = new SingleNumericEntity(valM.group(1), valM.group(2), isMathematica);
                                els.add(sne);
                            }
                            map.put(id, els);
                        }
                    });
        } catch (IOException ioe) {
            LOG.error("Cannot read lines from file: " + p);
        }
    }

    public void compareAll(Path output) {
        TreeMap<String, String> results = new TreeMap<>();
        int lengthCounter = 0;
        int sameCounter = 0;
        int errorCounter = 0;

        for ( String i : resultsMapMathematica.keySet() ) {
            LinkedList<SingleNumericEntity> mathSet = resultsMapMathematica.get(i);
            LinkedList<SingleNumericEntity> mapleSet = resultsMapMaple.get(i);
            if ( mapleSet == null || mapleSet.isEmpty() ) {
                LOG.debug("Skip " + i + " (not available in Maple)");
                continue;
            }

            if ( mathSet.size() != mapleSet.size() ) {
                LOG.warn("Lists have different lengths for " + i);
                lengthCounter++;
                continue;
            }

            // first, build same order list!
            LinkedList<SingleNumericEntity> orderedMapleSet = new LinkedList<>();
            for ( SingleNumericEntity mathematicaEntry : mathSet ) {
                for ( int j = 0; j < mapleSet.size(); j++ ) {
                    SingleNumericEntity mapleEntry = mapleSet.get(j);
                    try {
                        if ( mathematicaEntry.match(mapleEntry, forwardTranslator, backwardTranslator, mathematica) ) {
                            mapleSet.remove(j);
                            orderedMapleSet.addLast(mapleEntry);
                        }
                    } catch (ComputerAlgebraSystemEngineException | TranslationException e) {
                        LOG.error("Cannot match entries: \n" + mathematicaEntry + "\n" + mapleEntry, e);
                    }
                }
            }

            String mathSetDef = SingleNumericEntity.buildMathematicaValuesList(mathSet);
            String mapleSetDef = SingleNumericEntity.buildMathematicaValuesList(orderedMapleSet);
            String testValueList = SingleNumericEntity.buildMathematicaValuesList(orderedMapleSet, false);

            try {
                String cmd = String.format(
                        "%s[%s, %s, %s, %s]",
                        procedureName,
                        mathSetDef,
                        mapleSetDef,
                        testValueList,
                        "10^-5"
                );
                Expr res = mathematica.enterCommand(cmd);
//                Expr res = mathematica.enterCommand("Select[Flatten[" + mathSetDef + " - " + mapleSetDef + "], Abs[#] > 1*10^(-5) &]");
                if ( res.toString().matches("\\{}") ) {
                    LOG.info("Same set in line " + i);
                    sameCounter++;
                } else {
                    LOG.warn("Not same in line " + i + "\n" + res.toString() + "\n" + mathSetDef + "\n" + mapleSetDef);
                    results.put(i, res.toString());
                }
            } catch (ComputerAlgebraSystemEngineException e) {
                errorCounter++;
                LOG.error("Cannot compare " + i + "\n" + mathSetDef + "\n" + mapleSetDef);
            }
        }

        LOG.info("Finished all.");
        LOG.info("Same Sets: " + sameCounter);
        LOG.info("Different Length: " + lengthCounter);
        LOG.warn("Different Sets: " + results.size());
        if ( errorCounter != 0 ) LOG.error("Errors: " + errorCounter);

        LinkedList<String> list = new LinkedList<>();
        Pattern numPat = Pattern.compile("(\\d+)-?([a-z])?");

        for ( String i : results.keySet() ) {
            list.add(i);
        }

        Collections.sort(list, (l, r) -> {
            Matcher lm = numPat.matcher(l);
            Matcher rm = numPat.matcher(r);

            lm.matches();
            rm.matches();

            int li = Integer.parseInt(lm.group(1));
            int ri = Integer.parseInt(rm.group(1));
            if ( li == ri ) {
                if ( lm.group(2) != null && rm.group(2) != null ) {
                    return lm.group(2).charAt(0) - rm.group(2).charAt(0);
                } else if ( lm.group(2) != null ) {
                    return 1;
                } else return -1;
            } else return li - ri;
        });

        StringBuilder sb = new StringBuilder();
        for ( String i : list ) {
            sb.append(i).append(": ").append(results.get(i)).append("\n");
        }

        try {
            Files.write(output, sb.toString().getBytes());
            LOG.info("Done!");
        } catch (IOException e) {
            LOG.fatal("Cannot write results file!");
        }
    }

    public static void main(String[] args) throws IOException, MapleException, ComputerAlgebraSystemEngineException {
        NumericalDifferencesAnalyzer nda = new NumericalDifferencesAnalyzer();
        nda.init(
                Paths.get("/home/andreg-p/Howard/Results/MathNumeric/"),
//                Paths.get("libs/Results/MathematicaNumericPotentialConflicts/"),
                Paths.get("/home/andreg-p/Howard/Results/MapleNumeric/")
//                Paths.get("libs/Results/MapleNumericPotentialConflicts/")
        );
        nda.compareAll(Paths.get("libs/Results/NEW-difference-results.txt"));
    }
}