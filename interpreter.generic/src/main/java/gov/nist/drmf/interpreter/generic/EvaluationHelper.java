package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.meta.ListExtender;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
import gov.nist.drmf.interpreter.pom.generic.GenericFunctionAnnotator;
import gov.nist.drmf.interpreter.pom.generic.GenericReplacementTool;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.extensions.MatchablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.extensions.PomMatcherBuilder;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class EvaluationHelper {
    private static final Logger LOG = LogManager.getLogger(EvaluationHelper.class.getName());

    private static final Path goldPath = Paths.get("./misc/Results/Wikipedia/gold-data.json");

    private static final ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
    private static final ObjectWriter writer = SemanticEnhancedDocument.getWriter();

    private final Path path;

    private List<SemanticEnhancedDocument> goldDocs;
    private final Map<String, Map<String, Double>> goldDefs;
    private List<MOIPresentations> goldMois;
    private Map<String, MOIPresentations> goldMoiMap;
    private Map<String, String> moiIdMapIdMap;

    private int[] totalRelCounter;

    public EvaluationHelper(Path path) throws IOException {
        this.path = path;

        goldDocs = SemanticEnhancedDocument.deserialize(goldPath);
        totalRelCounter = new int[]{0, 0, 0};
        this.goldMois = new LinkedList<>();
        this.goldMoiMap = new HashMap<>();
        this.moiIdMapIdMap = new HashMap<>();

        goldDefs = new HashMap<>();
        for (SemanticEnhancedDocument sed : goldDocs) {
            for (MOIPresentations moi : sed.getFormulae()) {
                goldMois.add(moi);
                String key = GenericLatexSemanticEnhancer.makeKey(sed, moi);
                goldMoiMap.put(key, moi);
                moiIdMapIdMap.put(moi.getId(), key);
                Map<String, Double> scores = goldDefs.computeIfAbsent(key, k -> new HashMap<>());
                for (FormulaDefinition fd : moi.getDefiniens()) {
                    scores.put(fd.getDefinition(), fd.getScore());
                    if (fd.getScore() == 0)
                        totalRelCounter[0]++;
                    else if (fd.getScore() == 1) {
                        totalRelCounter[1]++;
                    } else if (fd.getScore() == 2) {
                        totalRelCounter[2]++;
                    }
                }
            }
        }
    }

    public String getString(SemanticEnhancedDocument[] docs) throws JsonProcessingException {
        return writer.writeValueAsString(docs);
    }

    public SemanticEnhancedDocument[] loadData() throws IOException {
        return mapper.readValue(path.toFile(), SemanticEnhancedDocument[].class);
    }

    public SemanticEnhancedDocument[] pickEquationsRandomlyAndJacobi(SemanticEnhancedDocument[] docs) {
        long oldFormulaeSize = Arrays.stream(docs).filter(Objects::nonNull).map(SemanticEnhancedDocument::getFormulae)
                .mapToLong(Collection::size).sum();

        Arrays.stream(docs)
                .filter(Objects::nonNull)
                .map(this::removeNonEquations)
                .filter(d -> !d.getFormulae().isEmpty())
                .forEach(this::pickRemainingRandomly);

        long newFormulaeSize = Arrays.stream(docs).filter(Objects::nonNull).map(SemanticEnhancedDocument::getFormulae)
                .mapToLong(Collection::size).sum();

        LOG.warn("Before removing and selecting the total number of formulae was: " + oldFormulaeSize);
        LOG.warn("After removing and selecting " + newFormulaeSize + " formulae remain");
        return docs;
    }

    private SemanticEnhancedDocument removeNonEquations(SemanticEnhancedDocument doc) {
        if (doc.getTitle().equals("Jacobi polynomials")) {
            LOG.debug("Skip removing equations from jacobi polynomials article");
            return doc;
        }
        List<MOIPresentations> formulae = doc.getFormulae();
        LOG.debug("Removing non equations from " + doc.getTitle() + ". Before had " + formulae.size() + " formulae");
        formulae.removeIf(this::remove);
        LOG.debug("After removing non-equations " + formulae.size() + " formulae remain");
        return doc;
    }

    private boolean remove(MOIPresentations moi) {
        String latex = moi.getGenericLatex();
        return latex.matches(".*(?:color|text).*") || !latex.contains("=");
    }

    private void pickRemainingRandomly(SemanticEnhancedDocument doc) {
        if (doc.getTitle().equals("Jacobi polynomials")) {
            LOG.debug("Skip pick randomly from jacobi polynomials article");
            return;
        }
        LOG.debug("Pick one equation randomly from " + doc.getTitle());
        List<MOIPresentations> formulae = doc.getFormulae();
        Random randomizer = new Random();
        int pickId = randomizer.nextInt(formulae.size());
        LOG.debug("Pick " + pickId + "/" + formulae.size());
        MOIPresentations pick = formulae.get(pickId);
        LOG.debug("Picked: " + pick.getId() + " : " + pick.getGenericLatex());
        formulae.clear();
        formulae.add(pick);
    }

    public static void buildGoldenDataset() throws IOException {
        Path p = Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-TRANSLATED.json");
        EvaluationHelper helper = new EvaluationHelper(p);
        SemanticEnhancedDocument[] docs = helper.loadData();
        docs = helper.pickEquationsRandomlyAndJacobi(docs);

//        String serializedDoc = helper.getString(docs);
//        Files.writeString( Paths.get("/mnt/share/data/wikipedia/Results/gold-data-otherSet.json"), serializedDoc );
    }

    public static void statisticsResults() throws IOException {
        Path p = Paths.get("/mnt/share/data/wikipedia/Results/dlmf-template-results-26-11-2020-generated-12-01-2021-TRANSLATED.json");
        EvaluationHelper helper = new EvaluationHelper(p);
        SemanticEnhancedDocument[] docs = helper.loadData();
    }

    public void compareMlp(Collection<SemanticEnhancedDocument> docs, int threshold, int topHits, int depth) throws IOException {
        double truePos = 0;
        double falsePos = 0;

        for (SemanticEnhancedDocument sed : docs) {
            Map<String, MOIPresentations> moiMap = sed.getMoiMapping(m -> sed.getTitle() + "-" + m);

            for (MOIPresentations moi : sed.getFormulae()) {
                String key = GenericLatexSemanticEnhancer.makeKey(sed, moi);
                if (!goldDefs.containsKey(key)) continue;

                List<FormulaDefinition> fds = moi.getDefiniens();
                if (topHits > 0)
                    fds = fds.subList(0, Math.min(topHits, fds.size()));

                if (depth > 0) {
                    for (String in : moi.getIngoingNodes()) {
                        String inKey = sed.getTitle() + "-" + MathTag.getID(in);
                        MOIPresentations inMoi = moiMap.get(inKey);
                        List<FormulaDefinition> inDefs = inMoi.getDefiniens();

                        if (topHits > 0) {
                            inDefs.sort(Comparator.comparingDouble(FormulaDefinition::getScore));
                            Collections.reverse(inDefs);
                            inDefs = inDefs.subList(0, Math.min(topHits, inDefs.size()));
                        }

                        Set<String> currDefs = fds.stream().map(FormulaDefinition::getDefinition).collect(Collectors.toSet());
                        ListExtender.addAll(fds, inDefs, m -> !currDefs.contains(m.getDefinition()));
                    }
                }

                fds.sort(Comparator.comparingDouble(FormulaDefinition::getScore));
                Collections.reverse(fds);

                Map<String, Double> goldFds = goldDefs.get(key);

//                int max = topHits < 0 ? fds.size() : topHits;
                Set<String> alreadyVisited = new HashSet<>();
                for (FormulaDefinition fd : fds) {
                    String def = fd.getDefinition();
                    if (alreadyVisited.contains(def)) continue;
                    alreadyVisited.add(def);
                    if (goldFds.containsKey(def) && goldFds.get(def) >= threshold)
                        truePos++;
                    else falsePos++;
                }
            }
        }

        int relCounter = threshold == 1 ? totalRelCounter[1] + totalRelCounter[2] : totalRelCounter[2];

        double prec = truePos / (truePos + falsePos);
        double rec = truePos / relCounter;
        double f1 = 2 * ((prec * rec) / (prec + rec));
        String out = String.format("Settings: Rel-Threshold: %d, Top-Hits: %d, Depth: %d\n" +
                        "%4s\t%4s\t%4s\t|\t%6s\t%6s\t%6s\n" +
                        "%4d\t%4d\t%4d\t|\t %.3f\t %.3f\t %.3f\n",
                threshold, topHits, depth,
                "TP", "FP", "REL", "PREC", "REC", "F1",
                (int) truePos, (int) falsePos, relCounter, prec, rec, f1
        );
        System.out.println(out);
    }

    public static void compareSemanticLatex(Collection<SemanticEnhancedDocument> docs) throws IOException, ParseException {
        List<SemanticEnhancedDocument> goldDocs = SemanticEnhancedDocument
                .deserialize(Paths.get("/mnt/share/data/wikipedia/Results/gold/gold-data-ManualAnnotationAndre.json"));

        int noSemantic = 0;
        Map<String, MatchablePomTaggedExpression> goldMoi = new HashMap<>();
        Map<String, String> goldMoiStr = new HashMap<>();
        for (SemanticEnhancedDocument sed : goldDocs) {
            for (MOIPresentations moi : sed.getFormulae()) {
                String key = GenericLatexSemanticEnhancer.makeKey(sed, moi);
                if (moi.getSemanticLatex() != null && !moi.getSemanticLatex().isBlank()) {
                    MatchablePomTaggedExpression mpom = null;
                    try {
                        mpom = PomMatcherBuilder.compile(moi.getSemanticLatex());
                    } catch (ParseException e) {
                        LOG.error("Unable to parse " + moi.getSemanticLatex() + " - KEY: " + key, e);
                        throw e;
                    }
                    goldMoi.put(key, mpom);
                    goldMoiStr.put(key, moi.getSemanticLatex());
                } else noSemantic++;
            }
        }

        int goldSize = goldMoi.size();
        int nonMatching = 0;
        int matching = 0;

        for (SemanticEnhancedDocument sed : docs) {
            for (MOIPresentations moi : sed.getFormulae()) {
                String key = GenericLatexSemanticEnhancer.makeKey(sed, moi);
                if (!goldMoi.containsKey(key)) continue;

                MatchablePomTaggedExpression goldM = goldMoi.get(key);
                if (moi.getSemanticLatex() == null || !goldM.match(moi.getSemanticLatex())) {
                    nonMatching++;
                } else matching++;
            }
        }

        String out = String.format("No Semantic: %d, Gold Size: %d\n" +
                        "Matching: %d\nNon-Matching: %d\n",
                noSemantic, goldSize, matching, nonMatching
        );
        System.out.println(out);
    }

    public static SemanticResults compareSemanticLatex(Collection<SemanticEnhancedDocument> docs,
                                                       GenericLacastConfig config, boolean preProcessing) throws IOException, ParseException {
        List<SemanticEnhancedDocument> goldDocs = SemanticEnhancedDocument
                .deserialize(Paths.get("./misc/Results/Wikipedia/gold-data.json"));

        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer(config);
        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

        int noSemantic = 0;
        Map<String, MatchablePomTaggedExpression> goldMoi = new HashMap<>();
        Map<String, String> goldMoiStr = new HashMap<>();
        for (SemanticEnhancedDocument sed : goldDocs) {
            for (MOIPresentations moi : sed.getFormulae()) {
                String key = GenericLatexSemanticEnhancer.makeKey(sed, moi);
                if (moi.getSemanticLatex() != null && !moi.getSemanticLatex().isBlank()) {
                    MatchablePomTaggedExpression mpom = null;
                    try {
                        mpom = PomMatcherBuilder.compile(moi.getSemanticLatex());
                    } catch (ParseException e) {
                        LOG.error("Unable to parse " + moi.getSemanticLatex() + " - KEY: " + key, e);
                        throw e;
                    }
                    goldMoi.put(key, mpom);
                    goldMoiStr.put(key, moi.getSemanticLatex());
                } else noSemantic++;
            }
        }

        int goldSize = goldMoi.size();
        int nonMatching = 0;
        int matching = 0;

        List<SemanticEnhancedDocument> newTranslatedSeds = new LinkedList<>();

        List<String> matches = new LinkedList<>();

        Set<String> remaining = new HashSet<>(goldMoiStr.keySet());

        for (SemanticEnhancedDocument sed : docs) {
            for (MOIPresentations moi : sed.getFormulae()) {
                String key = GenericLatexSemanticEnhancer.makeKey(sed, moi);
                if (!goldMoi.containsKey(key)) {
                    continue;
                }

                remaining.remove(key);

                String semanticlatex = moi.getGenericLatex();
                if (config.getMaxDepth() >= 0) {
                    MOIPresentations semanticMoi = enhancer.appendTranslationToMoi(sed, moi);
                    semanticlatex = semanticMoi.getSemanticLatex();
                    SemanticEnhancedDocument newDoc = new SemanticEnhancedDocument(sed.getTitle());
                    newDoc.getFormulae().add(semanticMoi);
                    newTranslatedSeds.add(newDoc);
                } else if (preProcessing) {
                    PrintablePomTaggedExpression pte = mlp.parse(semanticlatex);
                    GenericReplacementTool grt = new GenericReplacementTool();
                    pte = grt.getSemanticallyEnhancedExpression(pte);
                    MatchablePomTaggedExpression goldM = goldMoi.get(key);
                    if (semanticlatex == null || !goldM.match(pte)) {
                        nonMatching++;
                    } else {
                        matching++;
                        matches.add(key + ": " + moi.getGenericLatex());
                    }
                    continue;
                }

                MatchablePomTaggedExpression goldM = goldMoi.get(key);
                if (semanticlatex == null || !goldM.match(semanticlatex)) {
                    nonMatching++;
                } else {
                    matching++;
                    matches.add(key + ": " + moi.getGenericLatex());
                }
            }
        }

        System.out.println("NOT TESTED REMAINING IDS:");
        System.out.println(remaining);

        System.out.println("Matches: ");
        Collections.sort(matches);
        for (String s : matches) System.out.println(s);

        SemanticResults res = new SemanticResults(config, noSemantic, goldSize, nonMatching, matching);
        res.seds = newTranslatedSeds;
        return res;
    }

    public static void appendDepthDefinitions() throws IOException {
        List<SemanticEnhancedDocument> gold = SemanticEnhancedDocument.deserialize(goldPath);
        List<SemanticEnhancedDocument> docs = SemanticEnhancedDocument.deserialize(Paths.get("/mnt/share/data/wikipedia/Results/pages/"));

        Map<String, MOIPresentations> allMoi = new HashMap<>();
        for (SemanticEnhancedDocument doc : docs) {
            allMoi.putAll(doc.getMoiMapping(m -> doc.getTitle() + "-" + m));
        }

        for (SemanticEnhancedDocument goldSed : gold) {
            for (MOIPresentations goldMoi : goldSed.getFormulae()) {
                for (String goldIngoing : goldMoi.getIngoingNodes()) {
                    String id = MathTag.getID(goldIngoing);
                    String key = goldSed.getTitle() + "-" + id;
                    MOIPresentations moipres = allMoi.get(key);
                    if (moipres == null) {
                        LOG.warn("Unable to find: " + key + "; Formulae: " + goldIngoing);
                    } else {
                        List<String> goldDefs = goldMoi.getDefiniens().stream().map(FormulaDefinition::getDefinition).collect(Collectors.toList());
                        ListExtender.addAll(goldMoi.getDefiniens(), moipres.getDefiniens(), (m) -> !goldDefs.contains(m.getDefinition()));
                    }
                }
            }
        }

        LOG.info("Finish. Write new version: ");
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        String newGold = mapper.writer(prettyPrinter).writeValueAsString(gold);
        Files.writeString(goldPath, newGold);
    }

    public void compareMathematicaTranslation(Collection<SemanticEnhancedDocument> translatedDocs) throws InitTranslatorException {
        // 1) enter gold in mathematica and get FullForm
        // 2) translate LaCASt to mathematica and enter to get fullform
        // 3) enter generic latex via ToExpression["tex", TeXForm] and get FullForm from this
        // 4) compare 3-1 (baseline) and 2-1 (lacast)

        SemanticLatexTranslator slt = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        MathematicaInterface mi = MathematicaInterface.getInstance();
        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

        Map<String, MOIPresentations> moiMap = new HashMap<>();
        for (SemanticEnhancedDocument sed : translatedDocs) {
            moiMap.putAll(sed.getMoiMapping(m -> sed.getTitle() + "-" + m));
        }

        int transErrorImport = 0;
        int transErrorLacastStraight = 0;
        int transErrorLacastRules = 0;
        int transErrorLacastFull = 0;

        int transSuccessImport = 0;
        int transSuccessLacastStraight = 0;
        int transSuccessLacastRules = 0;
        int transSuccessLacastFull = 0;

        int matchImport = 0;
        int matchLacastStraight = 0;
        int matchLacastRules = 0;
        int matchLacastFull = 0;

        int successfulGoldEntries = 0;
        int goldEntriesInTotal = 0; // should be equal, otherwise something went wrong

        for (MOIPresentations goldMoiEntry : goldMois) {
//        for (Map.Entry<String, MOIPresentations> goldEntry : goldMoiMap.entrySet()) {
            String key = moiIdMapIdMap.get(goldMoiEntry.getId());
            MOIPresentations goldMoi = goldMoiMap.get( key );
//            if (goldMoi.getCasRepresentations() == null || goldMoi.getCasResults("Mathematica") == null) continue;

//            if ( !goldMoi.getId().equals("FORMULA_06f9b7b1d3f141742ad1c582b55056ba") ) continue;

            goldEntriesInTotal++;
            String goldMathematica = null;
            if (goldMoi.getCasRepresentations() != null && goldMoi.getCasResults("Mathematica") != null)
                goldMathematica = goldMoi.getCasResults("Mathematica").getCasRepresentation();
            else {
                LOG.warn("No gold translation defined for: " + goldMoi.getId() + " : " + goldMoi.getGenericLatex());
            }

            MOIPresentations moi = moiMap.get(key);
            String genericLatex = moi.getGenericLatex();
            String semanticLatex = moi.getSemanticLatex();

            String goldFullForm = null;

            try {
                if ( goldMathematica != null ) {
                    LOG.debug("Enter gold entry: " + goldMathematica);
                    clearCache(mi);
                    goldFullForm = mi.evaluate(goldMathematica);
                    successfulGoldEntries++;
                    LOG.debug("Successfully full-formed gold entry " + goldMoi.getId() + ": " + goldFullForm);
                }
            } catch (MathLinkException e) {
                LOG.error("Unable to enter gold translation to mathematica. Entered: " + goldMathematica + " from " + goldMoi.getId(), e);
            }

            try { // lacast straight
                LOG.debug("Translate straight: " + genericLatex);
                String lacastMath = slt.translate(genericLatex);
                LOG.debug("Enter lacast straight trans: " + lacastMath);
                clearCache(mi);
                String lacastFullForm = mi.evaluate(lacastMath);
                transSuccessLacastStraight++;

                LOG.debug("Successfully entered lacast straight trans: " + lacastFullForm);
                if (goldFullForm != null && goldFullForm.equals(lacastFullForm)) {
                    LOG.info("Lacast straight trans matches gold entry: " + goldFullForm);
                    matchLacastStraight++;
                } else {
                    LOG.warn("Lacast straight trans did not match gold:\nLaCASt: " + lacastFullForm + "\n  Gold: " + goldFullForm);
                }
            } catch (Exception e) {
                transErrorLacastStraight++;
                LOG.warn("Unable to enter/translate lacast straight translation", e);
            }

            try { // lacast pre-process
                LOG.debug("Translate pre-processed: " + genericLatex);
                PrintablePomTaggedExpression pte = mlp.parse(genericLatex);
                GenericReplacementTool grt = new GenericReplacementTool();
                pte = grt.getSemanticallyEnhancedExpression(pte);
                TranslatedExpression lacastMathE = slt.translate(pte);
                String lacastMath = lacastMathE.getTranslatedExpression();
                LOG.debug("Enter lacast pre-processed trans: " + lacastMath);
                clearCache(mi);
                String lacastFullForm = mi.evaluate(lacastMath);
                transSuccessLacastRules++;

                LOG.debug("Successfully entered lacast pre-processed trans: " + lacastFullForm);
                if (goldFullForm != null && goldFullForm.equals(lacastFullForm)) {
                    LOG.info("Lacast pre-processed matches gold entry: " + goldFullForm);
                    matchLacastRules++;
                } else {
                    LOG.warn("Lacast pre-processed did not match gold:\nLaCASt: " + lacastFullForm + "\n  Gold: " + goldFullForm);
                }
            } catch (Exception e) {
                transErrorLacastRules++;
                LOG.warn("Unable to enter/translate lacast pre-processed translation", e);
            }

            try { // lacast full
                LOG.debug("Translate full: " + semanticLatex);
                TranslationInformation lacastMathI = slt.translateToObjectFeatured(semanticLatex, new GenericFunctionAnnotator());
                String lacastMath = lacastMathI.getTranslatedExpression();
                LOG.debug("Enter lacast trans: " + lacastMath);
                clearCache(mi);
                String lacastFullForm = mi.evaluate(lacastMath);
                transSuccessLacastFull++;

                LOG.debug("Successfully entered lacast trans: " + lacastFullForm);
                if (goldFullForm != null && goldFullForm.equals(lacastFullForm)) {
                    LOG.info("Lacast full form matches gold entry: " + goldFullForm);
                    matchLacastFull++;
                } else {
                    LOG.warn("Lacast did not match gold:\nLaCASt: " + lacastFullForm + "\n  Gold: " + goldFullForm);
                }
            } catch (Exception e) {
                transErrorLacastFull++;
                LOG.warn("Unable to enter/translate lacast translation", e);
            }

            try { // mathematica import
                genericLatex = genericLatex.replace("\\", "\\\\");
                LOG.debug("Try to use Mathematica's import function: " + genericLatex);
                clearCache(mi);
                String mathImport = mi.evaluate("ToExpression[\"" + genericLatex + "\", TeXForm]");
                if ("$Failed".equals(mathImport)) {
                    transErrorImport++;
                } else transSuccessImport++;
                LOG.debug("Successfully entered mathematicas import");
                if (goldFullForm != null && goldFullForm.equals(mathImport)) {
                    LOG.info("Import full form matches gold entry: " + goldFullForm);
                    matchImport++;
                } else {
                    LOG.warn("Import did not match gold:\nImport: " + mathImport + "\n  Gold: " + goldFullForm);
                }
            } catch (Exception e) {
                LOG.warn("Unable to import tex", e);
            }

        }

        String resString = String.format(
                "\\textbf{Math\\_import}\t\t& $%3.2f$ & $%3.2f$ & $%3.2f$ & $%3.2f$ \\\\\n" +
                        "\\textbf{\\LaCASt\\_straight}\t& $%3.2f$ & $%3.2f$ & $%3.2f$ & $%3.2f$ \\\\\n" +
                        "\\textbf{\\LaCASt\\_rules}\t& $%3.2f$ & $%3.2f$ & $%3.2f$ & $%3.2f$ \\\\\n" +
                        "\\textbf{\\LaCASt\\_full}\t& $%3.2f$ & $%3.2f$ & $%3.2f$ & $%3.2f$ \\\\\n" +
                        "\\textbf{Human}\t\t\t& - & - & $%3.2f$ & $%3.2f$ \\\\\n",
                transErrorImport / (double) 95, transSuccessImport / (double) 95, matchImport / (double) 95, (transSuccessImport - matchImport) / (double) 95,
                transErrorLacastStraight / (double) 95, transSuccessLacastStraight / (double) 95, matchLacastStraight / (double) 95, (transSuccessLacastStraight - matchLacastStraight) / (double) 95,
                transErrorLacastRules / (double) 95, transSuccessLacastRules / (double) 95, matchLacastRules / (double) 95, (transSuccessLacastRules - matchLacastRules) / (double) 95,
                transErrorLacastFull / (double) 95, transSuccessLacastFull / (double) 95, matchLacastFull / (double) 95, (transSuccessLacastFull - matchLacastFull) / (double) 95,
                successfulGoldEntries / (double) 95, 1 - (successfulGoldEntries / (double) 95)
        );

        System.out.println(resString);
        System.out.println("Number of total entries with semantic: " + goldEntriesInTotal);
    }

    private static void clearCache(MathematicaInterface mi) {
        try {
            mi.evaluate("ClearAll[\"Global`*\"]");
        } catch (MathLinkException e) {
            LOG.error("Unable to clear variable cache in mathematica.");
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, ParseException, InitTranslatorException {
        List<SemanticEnhancedDocument> generatedDocs = SemanticEnhancedDocument.deserialize(Paths.get("./misc/Results/Wikipedia/gold-data-TRANSLATED.json"));
        EvaluationHelper helper = new EvaluationHelper(null);
        helper.compareMathematicaTranslation(generatedDocs);

//        List<SemanticEnhancedDocument> docs = SemanticEnhancedDocument.deserialize(Paths.get("./misc/Results/Wikipedia/gold-data-TRANSLATED.json"));

//        List<SemanticEnhancedDocument> gold = SemanticEnhancedDocument.deserialize(goldPath);
//
//        List<SemanticEnhancedDocument> output = new LinkedList<>();
//
//        Map<String, MOIPresentations> allMoi = new HashMap<>();
//        for (SemanticEnhancedDocument doc : docs) {
//            allMoi.putAll(doc.getMoiMapping(m -> doc.getTitle() + "-" + m));
//        }
//
//        for (SemanticEnhancedDocument goldSed : gold) {
//            for (MOIPresentations goldMoi : goldSed.getFormulae()) {
////                for (String goldIngoing : goldMoi.getIngoingNodes()) {
//                    String key = goldSed.getTitle() + "-" + goldMoi.getId();
//                    MOIPresentations moipres = allMoi.get(key);
//
//                    List<MOIPresentations> form = List.of(moipres);
//                    SemanticEnhancedDocument sed = new SemanticEnhancedDocument(goldSed.getTitle(), form);
//                    output.add(sed);
////                }
//            }
//        }
//
//        String update = EvaluationHelper.writer.writeValueAsString(output);
//        Files.writeString(Paths.get("./misc/Results/Wikipedia/gold-data-TRANSLATED-ORDERED.json"), update);

//
//        // straight
//        GenericLacastConfig config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(3);
//        config.setMaxMacros(20);
//        config.setMaxDepth(1);
//        SemanticResults sed0 = compareSemanticLatex(docs, config, false);
//
//        // only generic rules
//        config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(3);
//        config.setMaxMacros(30);
//        config.setMaxDepth(1);
//        SemanticResults sed1 = compareSemanticLatex(docs, config, true);

        // full
//        config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(20);
//        config.setMaxMacros(3);
//        config.setMaxDepth(0);
//        SemanticResults sed2 = compareSemanticLatex(docs, config, false);
//
//        config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(3);
//        config.setMaxMacros(6);
//        config.setMaxDepth(0);
//        SemanticResults sed3 = compareSemanticLatex(docs, config, false);
//
//        config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(3);
//        config.setMaxMacros(15);
//        config.setMaxDepth(1);
//        SemanticResults sed4 = compareSemanticLatex(docs, config, false);
//
//        config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(5);
//        config.setMaxMacros(5);
//        config.setMaxDepth(1);
//        SemanticResults sed5 = compareSemanticLatex(docs, config, false);
//
//        config = GenericLacastConfig.getDefaultConfig();
//        config.setMaxRelations(20);
//        config.setMaxMacros(20);
//        config.setMaxDepth(1);
//        SemanticResults sed6 = compareSemanticLatex(docs, config, false);

//        System.out.println(sed0.toString());
//        System.out.println(sed1.toString());
//        System.out.println(sed2.toString());
//        System.out.println(sed3.toString());
//        System.out.println(sed4.toString());
//        System.out.println(sed5.toString());
//        System.out.println(sed6.toString());

//
//        String update = EvaluationHelper.writer.writeValueAsString(sed0.seds);
//        Files.writeString(Paths.get("./misc/Results/Wikipedia/gold-data-TRANSLATED.json"), update);
//
//        System.out.println(sed1.toString());

//
//        System.out.println(sed1.toString());

//        EvaluationHelper helper = new EvaluationHelper(null);
//        helper.compareMlp(docs, 2, 1, 0);
//        helper.compareMlp(docs, 2, 3, 0);
//        helper.compareMlp(docs, 2, 6, 0);
//        helper.compareMlp(docs, 2, 15, 0);
//
//        helper.compareMlp(docs, 2, 1, 1);
//        helper.compareMlp(docs, 2, 3, 1);
//        helper.compareMlp(docs, 2, 6, 1);
//
//        helper.compareMlp(docs, 2, 1, 2);
//        helper.compareMlp(docs, 2, 3, 2);
    }

    private static class SemanticResults {
        private int noSemantic, goldSize, nonMatching, matching;

        private String configString;

        private List<SemanticEnhancedDocument> seds;

        public SemanticResults(GenericLacastConfig config, int noSemantic, int goldSize, int nonMatching, int matching) {
            configString = String.format("Max Relations: %d, Max Macros: %d, Max Depth: %d",
                    config.getMaxRelations(), config.getMaxMacros(), config.getMaxDepth()
            );
            this.nonMatching = nonMatching;
            this.noSemantic = noSemantic;
            this.goldSize = goldSize;
            this.matching = matching;
        }

        @Override
        public String toString() {
            return String.format("Config: %s\nNo Semantic: %d, Gold Size: %d\n" +
                            "Matching: %d (%4.2f)\nNon-Matching: %d (%4.2f)\n",
                    configString,
                    noSemantic, goldSize,
                    matching, (matching / (double) goldSize),
                    nonMatching, (nonMatching / (double) goldSize)
            );
        }
    }
}
