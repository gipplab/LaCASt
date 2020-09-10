package gov.nist.drmf.interpreter.evaluation.core;

import gov.nist.drmf.interpreter.common.cas.IAbortEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.interfaces.IPackageWrapper;
import gov.nist.drmf.interpreter.evaluation.common.*;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractEvaluator<T> {
    private static final Logger LOG = LogManager.getLogger(AbstractEvaluator.class.getName());

    public final static String NL = System.lineSeparator();

    private static double timeoutMS = 30_000;

    private IConstraintTranslator forwardTranslator;
    private IComputerAlgebraSystemEngine<T> engine;

    private HashMap<String, Integer> missingMacrosLib;

    private SymbolDefinedLibrary symbolDefinitionLibrary;

    private IPackageWrapper packageWrapper;
    private Set<String> reqPackageMemory;

    public static int DEFAULT_OUTPUT_LENGTH = 150;

    private boolean rememberPackages = false;

    public static final Pattern filterCases = Pattern.compile(
            "\\\\([Bb]igO|littleo|[fdc]Diff|asymp|sim|[lc]?dots)(?:[^a-zA-Z]|$)|" +
                    "\\{(cases|array|[bBvp]matrix|Matrix|Lattice)}|" +
                    "([fg](?:\\\\left)?\\(.*?(?:\\)|\\\\right\\)))"
    );

    public AbstractEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine
    ) {
        this.forwardTranslator = forwardTranslator;
        this.packageWrapper = forwardTranslator.getPackageWrapper();
        this.reqPackageMemory = new TreeSet<>();
        this.engine = engine;
        this.missingMacrosLib = new HashMap<>();
        this.symbolDefinitionLibrary = new SymbolDefinedLibrary();
    }

    public void setTimeoutSeconds(double timeoutSeconds) {
        AbstractEvaluator.timeoutMS = 1_000 * timeoutSeconds;
    }

    public double getTimeoutSeconds() {
        return AbstractEvaluator.timeoutMS;
    }

    public void setGlobalAssumptions(String... assumptions) throws ComputerAlgebraSystemEngineException {
        engine.setGlobalAssumptions(assumptions);
    }

    public void startRememberPackages() {
        reqPackageMemory.clear();
        this.rememberPackages = true;
    }

    public String forwardTranslate( String in, String label ) throws TranslationException {
        String translation = forwardTranslator.translate(in, label);
        if ( rememberPackages ) reqPackageMemory.addAll( forwardTranslator.getRequiredPackages() );
        return translation;
    }

    public void stopRememberPackages() {
        this.rememberPackages = false;
    }

    public T enterEngineCommand(String cmd) throws ComputerAlgebraSystemEngineException {
        return engine.enterCommand(cmd);
    }

    public Set<String> getRequiredPackages() {
        return reqPackageMemory;
    }

    public String getCASListRepresentation( List<String> list ) {
        return engine.buildList(list);
    }

    public IConstraintTranslator getThisConstraintTranslator() {
        return forwardTranslator;
    }

    public void addMissingMacro(String macro) {
        if ( missingMacrosLib.containsKey(macro) ) {
            missingMacrosLib.put(macro, missingMacrosLib.get(macro)+1);
        } else {
            missingMacrosLib.put(macro, 1);
        }
    }

    public List<String> getOrderedMissingMacros() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(
                missingMacrosLib.entrySet()
        );

        Collections.sort(entries, Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(entries);

        LinkedList<String> output = new LinkedList<>();
        for ( Map.Entry<String, Integer> e : entries ) {
            output.addLast(e.getKey() + ", " + e.getValue());
        }

        return output;
    }

    public String shortenOutput(String msg) {
        return msg;
        // TODO maybe flag what we should do
//        if ( msg.length() > DEFAULT_OUTPUT_LENGTH ) {
//            return msg.substring(0, DEFAULT_OUTPUT_LENGTH) + "...";
//        } else return msg;
    }

    public void forceGC() throws ComputerAlgebraSystemEngineException {
        this.engine.forceGC();
    }

    public abstract void performSingleTest(Case testCase);

    public void performAllTests(LinkedList<Case> testCases) {
//        HashSet<Integer> m = new HashSet<>();
//        for ( Integer i : NumericalEvaluator.POT_DIFF ) m.add(i);

        for ( Case test : testCases ) {
//            if ( m.contains(test.getLine()) ) {
                test.replaceSymbolsUsed(symbolDefinitionLibrary);
                performSingleTest(test);
//            }
        }
    }

    public abstract LinkedList<Case> loadTestCases();

    public LinkedList<Case> loadTestCases(
            int[] subset,
            Set<ID> skipLines,
            Path dataset,
            HashMap<Integer, String> labelLib,
            HashMap<Integer, String> skippedLinesInfo
    ) {
        return loadTestCases(
                subset,
                skipLines,
                dataset,
                labelLib,
                skippedLinesInfo,
                false
        );
    }

    public LinkedList<Case> loadTestCases(
            int[] subset,
            Set<ID> skipLines,
            Path dataset,
            HashMap<Integer, String> labelLib,
            HashMap<Integer, String> skippedLinesInfo,
            boolean reverseSkipLines
    ) {
        HashMap<Integer, Set<Integer>> skipLineIDs = new HashMap<>();
        skipLines.forEach(s -> {
            Set<Integer> subIds = skipLineIDs.get(s.id);
            if ( subIds == null ) {
                Set<Integer> subs = new HashSet<>();
                subs.add(s.subID);
                skipLineIDs.put(s.id, subs);
            } else {
                subIds.add(s.subID);
            }
        } );
        LinkedList<Case> testCases = new LinkedList<>();
        int[] currLine = new int[] {0};

        try (BufferedReader br = Files.newBufferedReader(dataset)) {
            Stream<String> lines = br.lines();

            int start = subset[0];
            int limit = subset[1];

            lines.sequential()
                    .peek(l -> {
                        currLine[0]++;
                        Matcher m = CaseAnalyzer.URL_PATTERN.matcher(l);
                        if ( m.find() )
                            labelLib.put(currLine[0], m.group(1));
                    }) // line counter
                    .filter(l -> start <= currLine[0] && currLine[0] < limit) // filter by limits
                    .filter(l -> {
                        boolean skip = skipLineIDs.containsKey(currLine[0]);
                        if ( reverseSkipLines ) skip = !skip;
                        if ( skip ){
                            skippedLinesInfo.put(currLine[0], "Skipped - user defined");
                            Status.SKIPPED.add();
                            return false;
                        } else return true;
                    }) // skip entries if wanted
                    .flatMap(l -> {
                        if ( l.contains("comments{Warning") && !l.contains("symbolDefined") ) {
                            skippedLinesInfo.put(currLine[0], "Skipped - no semantic math");
                            Status.SKIPPED.add();
                            return null;
                        }

                        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(l, currLine[0], symbolDefinitionLibrary);
                        if ( cc == null || cc.isEmpty() ) {
                            if ( l.contains("symbolDefined") ) {
                                LOG.info(currLine[0] + ": Ignored, because it's a definition.");
                                skippedLinesInfo.put(currLine[0], "Definition - Line is a non-semantic definition.");
                                Status.DEFINITIONS.add();
                                return null;
                            } else {
                                LOG.warn(currLine[0] + ": unable to extract test case.");
                                skippedLinesInfo.put(currLine[0], "Skipped - Unable to analyze test case: Null");
                                Status.SKIPPED.add();
                                return null;
                            }
                        }

                        Set<Integer> subIds = skipLineIDs.get(currLine[0]);
                        LinkedList<Case> testCC = new LinkedList<>();
                        int counter = 0;
                        String reason = "";
                        for ( Case ic : cc ) {
                            String test = ic.getLHS() + " " + ic.getRelation().getSymbol() + " " + ic.getRHS();
                            Matcher m = filterCases.matcher(test);
                            if ( !m.find() ) {
                                String conStr = ic.getRawConstraint();
                                if ( conStr != null && !conStr.isEmpty() ) {
                                    Matcher cm = filterCases.matcher(conStr);
                                    if ( cm.find() ) ic.removeConstraint();
                                }

                                boolean addBoolean = subIds == null || !subIds.contains(counter);
                                addBoolean = reverseSkipLines != addBoolean;

                                if ( addBoolean ) {
                                    testCC.add(ic);
                                    counter++;
                                } else {
                                    testCC.add(new DummyCase(ic.getMetaData()));
                                    counter++;
                                }
                            } else {
                                if ( !reason.isEmpty() ) reason += ", ";

                                if ( m.group(1) != null ) reason += m.group(1);
                                else if ( m.group(2) != null ) reason += m.group(2);
                                else reason += "Generic function " + m.group(3);
                                LOG.warn("Ignore " + currLine[0] + " because " + reason + "; Test case: " + test);
                            }
                        }

                        if (!testCC.isEmpty()) {
                            Case c = testCC.get(0);
                            labelLib.put(c.getLine(), c.getDlmf());
                            return testCC.stream();
                        } else {
                            skippedLinesInfo.put(currLine[0], "Skipped - Invalid test case: " + reason);
                            Status.SKIPPED.add();
                            return null;
                        }
                    })
                    .filter( Objects::nonNull )
                    .forEach(testCases::add);
            return testCases;
        } catch (IOException ioe) {
            LOG.fatal("Cannot load dataset!", ioe);
            return null;
        }
    }

    protected static Thread getAbortionThread(IAbortEvaluator evaluator) {
        return getAbortionThread(evaluator, timeoutMS);
    }

    protected static Thread getAbortionThread(IAbortEvaluator evaluator, double timeout) {
        return new Thread(() -> {
            boolean interrupted = false;
            LOG.debug("Start waiting for abortion.");
            try {
                Thread.sleep((int)timeout);
            } catch ( InterruptedException ie ) {
                LOG.debug("Interrupted, no abortion necessary.");
                interrupted = true;
            }

            if ( !interrupted ) {
                try {
                    LOG.warn("Abort current evaluation!");
                    evaluator.abort();
                } catch ( ComputerAlgebraSystemEngineException casee ) {
                    LOG.error("Cannot abort computation.");
                }
            }
        });
    }

    public abstract EvaluationConfig getConfig();
    public abstract HashMap<Integer, String> getLabelLibrary();
    public abstract LinkedList<String>[] getLineResults();

    private void writeMissingMacros() throws IOException {
        Path out = getConfig().getMissingMacrosOutputPath();
        Files.write(out, getOrderedMissingMacros());
    }

    public void writeResults() throws IOException {
        if ( getConfig().getMissingMacrosOutputPath() != null ) {
            writeMissingMacros();
        }

        String results = getResults(
                this.getConfig(),
                this.getLabelLibrary(),
                this.getLineResults()
        );
        Files.write(
                this.getConfig().getOutputPath(),
                results.getBytes()
        );
    }

    public int[] getResultInterval() {
        return getConfig().getSubSetInterval();
    }

    public String getOverviewString() {
        return Status.buildString();
    }

    protected String getResults(
            EvaluationConfig config,
            HashMap<Integer, String> labelLib,
            LinkedList<String>[] lineResults
    ){
        StringBuffer sb = new StringBuffer();

        sb.append("Overall: ");
        sb.append(getOverviewString());
        sb.append(" for test expression: ");
        sb.append(config.getTestExpression());
        sb.append(NL);

        return buildResults(
                sb,
                labelLib,
                config.showDLMFLinks(),
                getResultInterval(),
                lineResults
        );
    }

    protected String buildResults(
            StringBuffer sb,
            HashMap<Integer, String> labelLib,
            boolean showDLMF,
            int[] limits,
            LinkedList<String>[] lineResults){
        int start = limits[0];
        int limit = limits[1];

        for ( int i = start; i < lineResults.length && i < limit; i++ ){
            if ( lineResults[i] == null || lineResults[i].isEmpty() ){
//                sb.append(i).append(": Skipped (is null)").append(NL);
//                return sb.toString();
                continue;
            }

            LinkedList<String> tmp = lineResults[i];
            LinkedList<String> lineResult = new LinkedList<>();
            for ( String s : tmp ) {
                if (    !s.contains("Ignore") &&
                        !s.contains("Skipped - (user defined)") &&
                        !s.contains("Skipped - Line is a definition")
                ) lineResult.add(s);
            }

            if ( lineResult.isEmpty() ) continue;

            sb.append(i);
            boolean first = true;
            Character c = 'a';

            for ( String s : lineResult ) {
                if ( !first ) {
                    sb.append(i+"-"+c);
                    c++;
                } else first = false;

                String dlmf = labelLib.get(i);
                if ( dlmf != null && showDLMF ){
                    sb.append(" [").append(dlmf).append("]: ");
                } else sb.append(": ");

                sb.append(s);
                sb.append(NL);
            }


        }
        return sb.toString();
    }

    public class ID {
        public Integer id;
        public Integer subID;

        public ID(Integer id) {
            this.id = id;
            this.subID = null;
        }

        public ID(String id) {
            String[] ids = id.split("-");
            this.id = Integer.parseInt(ids[0]);
            if ( ids.length > 1 ) {
                char a = ids[1].charAt(0);
                this.subID = a - 'a' + 1;
            } else this.subID = 0;
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof AbstractEvaluator.ID ) {
                AbstractEvaluator.ID ref = (AbstractEvaluator.ID) obj;
                if ( this.id.equals(ref.id) ){
                    if ( this.subID != null && ref.subID != null )
                        return this.subID.equals(ref.subID);
                }
            }
            return false;
        }
    }

    public class DummyCase extends Case {
        public DummyCase(CaseMetaData metaData) {
            super(null, null, null, metaData);
        }
    }
}
