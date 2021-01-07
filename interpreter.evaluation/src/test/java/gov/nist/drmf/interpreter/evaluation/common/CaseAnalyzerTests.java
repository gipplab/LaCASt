package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.latex.Relations;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.pom.common.CaseMetaData;
import gov.nist.drmf.interpreter.pom.common.SymbolTag;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.core.api.DLMFTranslator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class CaseAnalyzerTests {
    private static DLMFTranslator dlmfTrans;

    @BeforeAll
    public static void setup() throws InitTranslatorException {
        dlmfTrans = new DLMFTranslator(Keys.KEY_MAPLE);
    }

    @Test
    public void basicTest() {
        String line = "\\Ln@@{z} = \\int_1^z \\frac{\\diff{t}}{t} \\constraint{z\\neq 0} \\url{http://dlmf.nist.gov/1.2.E1}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("\\Ln@@{z}", c.getLHS());
        assertEquals("\\int_1^z \\frac{\\diff{t}}{t}", c.getRHS());
        assertEquals(Relations.EQUAL, c.getRelation());

        assertEquals("[z <> 0]", c.getConstraints(dlmfTrans, null).toString());
        assertEquals("http://dlmf.nist.gov/1.2.E1", c.getMetaData().getLabel().getTex());
    }

    @Test
    public void basicKeyphraseTest() {
        String line = "\\sum_{k=0}^n k > 1 \\source{(8.04), p.~414}{Olver:1997:ASF} \\keyphrase{z} \\keyphrase{y} \\url{http://dlmf.nist.gov/1.2.E1}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("\\sum_{k=0}^n k", c.getLHS());
        assertEquals("1", c.getRHS());
        assertEquals(Relations.GREATER_THAN, c.getRelation());

        assertTrue(c.getConstraints(dlmfTrans, null).isEmpty());
        assertEquals("http://dlmf.nist.gov/1.2.E1", c.getMetaData().getLabel().getTex());
    }

    @Test
    public void caseGeneralMethodsTest() {
        String line = "\\Ln@@{z} = \\int_1^z \\frac{\\diff{t}}{t} \\constraint{z\\neq 0} \\url{http://dlmf.nist.gov/1.2.E1}";

        int randomLineNumber = (int)(Math.random()*100);
        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, randomLineNumber, new SymbolDefinedLibrary());
        assertNotNull(cc);
        assertEquals(1, cc.size());

        Case c = cc.get(0);
        assertEquals("1.2.E1", c.getEquationLabel());
        assertEquals("http://dlmf.nist.gov/1.2.E1", c.getDlmf());
        assertEquals(randomLineNumber, c.getLine());
        assertTrue(c.isEquation());

        String infoStr = c.toString();
        assertTrue(infoStr.contains(Integer.toString(randomLineNumber)));
        assertTrue(infoStr.contains("\\Ln@@{z}"));
        assertTrue(infoStr.contains("\\int_1^z \\frac{\\diff{t}}{t}"));
    }

    @Test
    public void generalConstraintTest() {
        // fictive constraint, that will never match any blueprint
        // ensure the constraints does not exist:
        boolean previousState = CaseAnalyzer.ACTIVE_BLUEPRINTS;
        CaseAnalyzer.ACTIVE_BLUEPRINTS = true;
        String line = "\\Ln@@{z} = z+1 \\constraint{z = 1, 2, 3, \\dots} \\constraint{z\\neq 2, 3, 5, 7, 9, ...} \\url{http://dlmf.nist.gov/111.1.1}";
        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);

        assertEquals(0, c.getConstraintObject().getTexConstraints().length);
        assertEquals(1, c.getConstraintObject().getSpecialConstraintVariables().length);
        assertEquals("z", c.getConstraintObject().getSpecialConstraintVariables()[0]);
        assertEquals(1, c.getConstraintObject().getSpecialConstraintValues().length);
        assertEquals("3", c.getConstraintObject().getSpecialConstraintValues()[0]);

        List<String> constraints = c.getConstraints(dlmfTrans, null);
        assertEquals(0, constraints.size());

        // the new version ignores invalid / impossible to parse constraints since they crash the CAS
//        assertEquals(1, constraints.size());
//        assertEquals("z <> 2 , 3 , 5 , 7 , 9 ,", constraints.get(0));

        CaseAnalyzer.ACTIVE_BLUEPRINTS = previousState;
    }

    @Test
    public void constraintMatchBlueprintTest() {
        // let's test our classic blueprint from line 1:
        // var \in \Integers ==> 3
        boolean previousState = CaseAnalyzer.ACTIVE_BLUEPRINTS;
        CaseAnalyzer.ACTIVE_BLUEPRINTS = true;
        String line = "z/2 = z+1 \\constraint{z \\in \\Integers} \\url{http://dlmf.nist.gov/111.1.1}";
        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);

        List<String> vars = c.getConstraintVariables(dlmfTrans, c.getEquationLabel());
        List<String> vals = c.getConstraintValues();

        // z in Integers matches one of our constraints
        assertEquals(1, vars.size());
        assertEquals(1, vals.size());
        assertEquals("z", vars.get(0));
        assertEquals("3", vals.get(0));

        CaseAnalyzer.ACTIVE_BLUEPRINTS = previousState;
    }

    @Test
    public void multiTest() {
        String line = "\\sum_{k=0}^n k > 1 > 0 \\url{tmp} \\url{http://dlmf.nist.gov/1.2.E1}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("\\sum_{k=0}^n k", c.getLHS());
        assertEquals("1", c.getRHS());
        assertEquals(Relations.GREATER_THAN, c.getRelation());

        assertTrue(c.getConstraints(dlmfTrans, null).isEmpty());
        assertEquals("http://dlmf.nist.gov/1.2.E1", c.getMetaData().getLabel().getTex());

        c = cc.get(1);
        assertEquals("1", c.getLHS());
        assertEquals("0", c.getRHS());
        assertEquals(Relations.GREATER_THAN, c.getRelation());
    }

    @Test
    public void genlogTest() {
        String line = "1 \\ge\\cpi + \\genlog{1}@{1} \\geq\\pi \\url{http://dlmf.nist.gov/1.2.E1}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("1", c.getLHS());
        assertEquals("\\cpi + \\genlog{1}@{1}", c.getRHS());
        assertEquals(Relations.GREATER_EQ_THAN, c.getRelation());

        assertTrue(c.getConstraints(dlmfTrans, null).isEmpty());
        assertEquals("http://dlmf.nist.gov/1.2.E1", c.getMetaData().getLabel().getTex());

        c = cc.get(1);
        assertEquals("\\cpi + \\genlog{1}@{1}", c.getLHS());
        assertEquals("\\pi", c.getRHS());
        assertEquals(Relations.GREATER_EQ_THAN, c.getRelation());
    }

    @Test
    public void pmTest() {
        String line = "\\pm 1 = - \\mp 1 \\url{http://dlmf.nist.gov/1.2.E1}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("+ 1", c.getLHS());
        assertEquals("- - 1", c.getRHS());
        assertEquals(Relations.EQUAL, c.getRelation());

        c = cc.get(1);
        assertEquals("- 1", c.getLHS());
        assertEquals("- + 1", c.getRHS());
        assertEquals(Relations.EQUAL, c.getRelation());
    }

    @Test
    public void errorTest() {
        String line = "\\sqrt{z^2} = \\begin{cases} z, & \\realpart@@{z} \\geq 0, -z, & \\realpart@@{z} \\leq 0. \\end{cases} \\label{eq:EF.PVEX}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("\\sqrt{z^2}", c.getLHS());
        assertEquals(Relations.EQUAL, c.getRelation());
    }

    @Test
    public void equal0Test() {
        String line = "\\AiryAi@{z}+\\expe^{-2\\cpi\\iunit/3} \\AiryAi@{z\\expe^{-2\\cpi\\iunit/3}}+" +
                "\\expe^{2\\cpi\\iunit/3}\\AiryAi@{z\\expe^{2\\cpi\\iunit/3}}=0, " +
                "\\source{(8.03), p.~414}{Olver:1997:ASF} \\url{http://dlmf.nist.gov/1.2.E1}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        Case c = cc.get(0);
        assertEquals("\\AiryAi@{z}+\\expe^{-2\\cpi\\iunit/3} \\AiryAi@{z\\expe^{-2\\cpi\\iunit/3}}+\\expe^{2\\cpi\\iunit/3}\\AiryAi@{z\\expe^{2\\cpi\\iunit/3}}", c.getLHS());
        assertEquals("0", c.getRHS());
        assertEquals(Relations.EQUAL, c.getRelation());

        assertTrue(c.getConstraints(dlmfTrans, null).isEmpty());
        assertEquals("http://dlmf.nist.gov/1.2.E1", c.getMetaData().getLabel().getTex());
    }

    @Test
    public void jacobiThetaQTest() {
        String line = "\\Jacobiellsnk@{z}{k}=" +
                "\\frac{\\Jacobithetaq{3}@{0}{q}}{\\Jacobithetaq{2}@{0}{q}}\\frac{\\Jacobithetaq{1}@{\\zeta}{q}}{\\Jacobithetaq{4}@{\\zeta}{q}}=" +
                "\\frac{1}{\\Jacobiellnsk@{z}{k}}, " +
                "\\url{http://dlmf.nist.gov/22.2.E4} " +
                "\\symbolDefined[\\Jacobiellnsk@{\\NVar{z}}{\\NVar{k}}]{C22.S2.E4.m3bdec} " +
                "\\symbolDefined[\\Jacobiellsnk@{\\NVar{z}}{\\NVar{k}}]{C22.S2.E4.m2bdec} " +
                "\\symbolUsed[\\Jacobithetaq{\\NVar{j}}@{\\NVar{z}}{\\NVar{q}}]{C20.S2.i.m2badec} " +
                "\\symbolUsed[q]{C22.S2.E1.m2bbdec} \\symbolUsed[z]{C22.S1.XMD3.m1badec} " +
                "\\symbolUsed[k]{C22.S1.XMD4.m1bcdec} \\symbolUsed[\\zeta]{C22.S2.XMD2.m1badec}";

        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(line, 1, new SymbolDefinedLibrary());
        assertNotNull(cc);
        assertEquals(2, cc.size());

        Case c = cc.get(0);
        assertEquals("\\Jacobiellsnk@{z}{k}", c.getLHS());
        assertEquals("\\frac{\\Jacobithetaq{3}@{0}{q}}{\\Jacobithetaq{2}@{0}{q}}\\frac{\\Jacobithetaq{1}@{\\zeta}{q}}{\\Jacobithetaq{4}@{\\zeta}{q}}", c.getRHS());
        assertEquals(Relations.EQUAL, c.getRelation());
    }

    @Resource("test-cases.txt")
    public void fileTest(String testCases) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();

        int[] lineCounter = new int[]{0};
        Arrays.stream(testCases.split("\n"))
                .peek( l -> lineCounter[0]++ )
                .forEach(l -> {
                    CaseAnalyzer.analyzeLine(l, lineCounter[0], lib);
                });

        SymbolTag def = lib.getSymbolDefinition("C1.S2.XMD3.m1badec");
        assertNotNull(def);
        assertEquals("B_{j}", def.getSymbol());
        assertEquals("\\frac{f^{(n-j)}(\\alpha_{1})}{(n-j)!}", def.getDefinition());

        def = lib.getSymbolDefinition("C1.S2.XMD2.m1badec");
        assertNotNull(def);
        assertEquals("A_{j}", def.getSymbol());
        assertEquals("\\frac{(\\alpha_{j})}{\\prod\\limits_{k\\not=j}(\\alpha_{j}-\\alpha_{k})}", def.getDefinition());

        LinkedList<SymbolTag> used = new LinkedList<>();
        used.add(new SymbolTag("C1.S2.XMD2.m1bdec", "A_{j}"));
        CaseMetaData meta = new CaseMetaData(1, null, null, used);
        Case c = new Case("1 + A_{j}", "2", Relations.EQUAL, meta);
        c = c.replaceSymbolsUsed(lib);
        assertEquals("1 + (\\frac{(\\alpha_{j})}{\\prod\\limits_{k\\not=j}(\\alpha_{j}-\\alpha_{k})})", c.getLHS());
        assertEquals("2", c.getRHS());

        used.removeFirst();
        used.add(new SymbolTag("DREAM.C1.S2.XMD3.m1bbdec", "B_{j}"));
        c = new Case("1 + B_{j}", "2", Relations.EQUAL, meta);
        c = c.replaceSymbolsUsed(lib);
        assertEquals("1 + B_{j}", c.getLHS());
    }

    @Resource("zetaSubstitutionTests.txt")
    void zetaSubstitutionTest(String testStrings) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        int[] lineCounter = new int[]{0};
        List<LinkedList<Case>> testCases = Arrays.stream(testStrings.split("\n"))
                .peek( l -> lineCounter[0]++ )
                .map(l -> CaseAnalyzer.analyzeLine(l, lineCounter[0], lib))
                .collect(Collectors.toList());

        // expecting two lines analyzed
        assertEquals(2, testCases.size());

        // the first line is a definition, hence it does ont contain any test cases
        assertNull(testCases.get(0));

        // the second line is a multi-equation expression, so we presume multiple expressions
        LinkedList<Case> airyTests = testCases.get(1);
        // the actual length depends on our approach, to handle multi-equations,
        // hence we should not test for the exact number of test cases here, just bigger than 1 is ok
        assertTrue(airyTests.size() > 1);

        // nonetheless, the first test case should always be the first equation, hence we can at least check this
        Case airyFirstTest = airyTests.getFirst();
        assertTrue(airyFirstTest.isEquation());
        assertEquals(Relations.EQUAL, airyFirstTest.getRelation());
        assertEquals("\\AiryAi@{z}", airyFirstTest.getLHS());
        // note that \pm should be replaced by plus in the first test case
        assertEquals("\\pi^{-1}\\sqrt{z/3}\\modBesselK{+ 1/3}@{\\zeta}", airyFirstTest.getRHS());

        // check if usedSymbol works correctly
        assertEquals(1, lib.library.keySet().size()); // we should have one key in the library (for \zeta)
        Case actualAiryAiTest = airyFirstTest.replaceSymbolsUsed(lib);
        assertTrue(actualAiryAiTest.isEquation());
        assertEquals(Relations.EQUAL, actualAiryAiTest.getRelation());
        assertEquals("\\AiryAi@{z}", actualAiryAiTest.getLHS());
        // note that only \zeta has changed if everything worked properly
        assertEquals("\\cpi^{-1}\\sqrt{z/3}\\modBesselK{+ 1/3}@{{\\frac{2}{3} z^{\\frac{3}{2}}}}", actualAiryAiTest.getRHS());
    }

    @Resource("wrongSubstitutionTests.txt")
    void wrongSubstitutionTest(String testStrings) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        int[] lineCounter = new int[]{0};
        List<LinkedList<Case>> testCases = Arrays.stream(testStrings.split("\n"))
                .peek( l -> lineCounter[0]++ )
                .map(l -> CaseAnalyzer.analyzeLine(l, lineCounter[0], lib))
                .collect(Collectors.toList());

        assertEquals(3, testCases.size());

        // the second line is a multi-equation expression, so we presume multiple expressions
        Case eulerTest = testCases.get(1).getFirst();
        assertEquals("-\\EulerConstant", eulerTest.getRHS());

        Case actualAiryAiTest = eulerTest.replaceSymbolsUsed(lib);
        assertEquals("-\\EulerConstant", actualAiryAiTest.getRHS());
    }

    @Resource("recursiveDefinitionTests.txt")
    void recursiveSubstitutionTest(String testStrings) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        List<LinkedList<Case>> testCases = loadTestCases(testStrings, lib);

        assertEquals(5, testCases.size());
        assertEquals(3, lib.library.keySet().size());

        Case trickyParaWCase = testCases.get(0).get(0);

        // that's real hard stuff... let's check if everything is loaded properly
        assertEquals("\\paraW@{a}{x}", trickyParaWCase.getLHS());
        assertEquals("\\sqrt{k/2}\\,e^{\\frac{1}{4}\\pi a}\\left(e^{i\\rho}\\paraU@{ia}{xe^{-\\pi i/4}}+e^{-i\\rho}\\paraU@{-ia}{xe^{\\pi i/4}}\\right)", trickyParaWCase.getRHS());
        assertEquals(Relations.EQUAL, trickyParaWCase.getRelation());
        assertEquals("12.14.E4", trickyParaWCase.getEquationLabel());

        // let the magic happen...
        trickyParaWCase = trickyParaWCase.replaceSymbolsUsed(lib);
        assertEquals("\\paraW@{a}{x}", trickyParaWCase.getLHS());
        assertEquals(
                "\\sqrt{(\\sqrt{1+\\expe ^{2\\cpi a}}-\\expe ^{\\cpi a}) / 2} " +
                "\\expe^{\\frac{1}{4}\\cpi a}" +
                " (" +
                    "\\expe^{\\iunit (\\tfrac{1}{8} \\cpi + \\tfrac{1}{2} (\\phase@@{\\EulerGamma@{\\tfrac{1}{2}+\\iunit a}}))} " +
                    "\\paraU@{\\iunit a}{" +
                        "x\\expe ^{-\\cpi \\iunit /4}" +
                    "} + " +
                    "\\expe^{- \\iunit (\\tfrac{1}{8} \\cpi + \\tfrac{1}{2} (\\phase@@{\\EulerGamma@{\\tfrac{1}{2}+\\iunit a}}))} " +
                    "\\paraU@{-\\iunit a}{" +
                        "x\\expe ^{\\cpi \\iunit /4}" +
                    "}" +
                ")", trickyParaWCase.getRHS());
    }

    @Resource("jacobiQTests.txt")
    void jacobiSubstitutionTest(String testStrings){
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        List<LinkedList<Case>> testCases = loadTestCases(testStrings, lib);

        Case jacobiCase = testCases.get(1).get(0);
        assertEquals("\\Jacobiellsnk@{z}{k}", jacobiCase.getLHS());
        assertEquals("\\frac{\\Jacobithetaq{3}@@{0}{q}}{\\Jacobithetaq{2}@{0}{q}}\\frac{\\Jacobithetaq{1}@{\\zeta}{q}}{\\Jacobithetaq{4}@{\\zeta}{q}}", jacobiCase.getRHS());

        jacobiCase = jacobiCase.replaceSymbolsUsed(lib);
        assertEquals("\\Jacobiellsnk@{z}{k}", jacobiCase.getLHS());
        assertEquals(
                "\\frac" +
                    "{\\Jacobithetaq{3}@{0}{(\\exp@{-\\cpi\\ccompellintKk@{k}/\\compellintKk@{k}})}}" +
                    "{\\Jacobithetaq{2}@{0}{(\\exp@{-\\cpi\\ccompellintKk@{k}/\\compellintKk@{k}})}} " +
                "\\frac" +
                    "{\\Jacobithetaq{1}@{\\zeta}{(\\exp@{-\\cpi\\ccompellintKk@{k}/\\compellintKk@{k}})}}" +
                    "{\\Jacobithetaq{4}@{\\zeta}{(\\exp@{-\\cpi\\ccompellintKk@{k}/\\compellintKk@{k}})}}",
                jacobiCase.getRHS());
    }

    @Resource("gammaSubstitutionConstraintTests.txt")
    void gammaSubstitutionConstraintTest(String testStrings) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        List<LinkedList<Case>> testCases = loadTestCases(testStrings, lib);

        Case struveTest = testCases.get(1).get(0);
        List<String> constraintsPrior = struveTest.getConstraints(dlmfTrans, "11.5.E2");
        assertEquals(1, constraintsPrior.size());
        assertEquals("Re(z) > 0", constraintsPrior.get(0));

        struveTest = struveTest.replaceSymbolsUsed(lib);
        List<String> constraintsAfter = struveTest.getConstraints(dlmfTrans, "11.5.E2");
        assertEquals(2, constraintsAfter.size());
        assertEquals("Re(z) > 0", constraintsAfter.get(0));
        assertEquals("Re(nu +(1)/(2)) > 0", constraintsAfter.get(1));
    }

    @Resource("gammaSubstitutionConstraintTests.txt")
    void gammaSubstitutionMultiConstraintTest(String testStrings) {
        SymbolDefinedLibrary lib = new SymbolDefinedLibrary();
        List<LinkedList<Case>> testCases = loadTestCases(testStrings, lib);

        Case gammaCases = testCases.get(2).get(0);
        gammaCases = gammaCases.replaceSymbolsUsed(lib);
        List<String> constraintsAfter = gammaCases.getConstraints(dlmfTrans, "5.2.E5");
        assertEquals(2, constraintsAfter.size());
        assertTrue(constraintsAfter.contains("Re(a + n) > 0"));
        assertTrue(constraintsAfter.contains("Re(a) > 0"));

        assertEquals("a", gammaCases.getConstraintVariables(dlmfTrans, "5.2.E5").get(0));
        assertEquals("1", gammaCases.getConstraintValues().get(0));
    }

    private List<LinkedList<Case>> loadTestCases(String testStrings, SymbolDefinedLibrary lib) {
        int[] lineCounter = new int[]{0};
        return Arrays.stream(testStrings.split("\n"))
                .peek( l -> lineCounter[0]++ )
                .map(l -> CaseAnalyzer.analyzeLine(l, lineCounter[0], lib))
                .collect(Collectors.toList());
    }
}
