package gov.nist.drmf.interpreter.roundtrip;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.*;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.RelationResults;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs the equation test cases.
 *
 * Created by AndreG-P on 21.04.2017.
 */
public class EquationTestCases {
    public static final Logger LOG = LogManager.getLogger(EquationTestCases.class.toString());

    private static final String FERRER_DEF_ASS = "assume(-1 < x, x < 1);";
    private static final String LEGENDRE_DEF_ASS = "assume(1 < x);";
    private static final String RESET = "restart;";

    private static HashMap<String, String> link_librarie;

    private static MapleTranslator mapleT;
    private static MapleSimplifier mapleS;
    private static LinkedList<TestCaseInLaTeX> testCases;

    private static HashMap<String, Integer> unknown_macro_counter;

    private static final Path overview_file =
            GlobalPaths.PATH_REFERENCE_DATA.resolve("test-cases-overview.txt");

    private static int inTotalTestCases = 0;

    @BeforeAll
    public static void init(){
        Integer[] ca = new Integer[]{1};
        testCases = new LinkedList<>();
        mapleT = new MapleTranslator();
        link_librarie = new HashMap<>();
        unknown_macro_counter = new HashMap<>();
        TestStatus.reset();

        // /mnt/SharedPartition/Privacy/MAPrivate
        Path private_repo = Paths.get("/", "home", "andreg-p", "Howard");
        Path lib = private_repo.resolve("BruceLabelLinks.txt");
        LOG.info("Start Link-Lib Init...");

        try ( BufferedReader br = Files.newBufferedReader(lib) ){
            br.lines()
                    .parallel()
                    .map( l -> l.split("=>") )
                    .filter( l -> l.length == 2 )
                    .forEach( l -> {
                        String t = l[1].trim().substring("DLMF:/".length());
                        link_librarie.put( l[0].trim(), "http://dlmf.nist.gov/"+t );
                    } );

            //for ( String key : link_librarie.keySet() )
            //    LOG.info( key + ": " + link_librarie.get(key) );
        } catch ( Exception e ){
            e.printStackTrace();
            return;
        }

        //Path p = GlobalPaths.PATH_REFERENCE_DATA.resolve("TestCases.txt");
        Path p = private_repo.resolve("lessformulas.txt");
        try ( BufferedReader br = Files.newBufferedReader(p) ){
            mapleT.init();
            mapleS = mapleT.getMapleSimplifier();
            mapleT.enterMapleCommand(
                    MapleInterface.extractProcedure( GlobalPaths.PATH_MAPLE_NUMERICAL_PROCEDURES )
            );

            br.lines()
                    .limit(300) // TODO debug limit
//                    .filter( l -> {
//                        if ( l.contains("'") ){
//                            int line = ca[0]++;
//                            TestStatus.SINGLE_QUOTES.add( line );
//                            return false;
//                        } return true;
//                    } )
                    .map( l -> analyzeLine( l, ca ))
                    .filter( Objects::nonNull )
                    .forEach( t -> {
                        testCases.add(t);
                        //LOG.info("Created test case: " + t);
                    } );

            inTotalTestCases = testCases.getLast().line;
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void end(){
        LOG.info("Results:"+System.lineSeparator() + TestStatus.stats());

        try {
            overview_file.toFile().createNewFile();
        } catch ( Exception e ){}

        try ( BufferedWriter bw = Files.newBufferedWriter(overview_file) ){
            String nl = System.lineSeparator();
            bw.write( "Results from: " + (new Date()) + nl );
            bw.write( "Total Number of Test Cases: " + inTotalTestCases + nl + nl );
            bw.write( TestStatus.stats() );
            bw.flush();
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    private static final String LABEL = "\\\\label";
    private static final String CONSTRAINT = "\\\\constraint";

    private static final Pattern CONST_LABEL_PATTERN =
            Pattern.compile("[\\s{,;'\\$]*(.*)");

    public static TestCaseInLaTeX analyzeLine( String line, Integer[] counter ){
        String eq, constraint = null, label = null;
        String[] constSplit = line.split( CONSTRAINT );
        String[] labelSplit = line.split( LABEL );

        // there is a constraint
        if ( constSplit.length > 1 ){
            labelSplit = constSplit[1].split( LABEL );
            eq = constSplit[0];
            constraint = labelSplit[0];
            if ( labelSplit.length > 1 )
                label = labelSplit[1];
        } else if ( labelSplit.length > 1 ){
            eq = labelSplit[0];
            label = labelSplit[1];
        } else {
            eq = line;
        }

        String ending_deletion = "[\\s,;.]*$";

        if ( constraint != null ){
            Matcher m = CONST_LABEL_PATTERN.matcher(constraint);
            if ( m.matches() ){
                constraint = m.group(1);
                constraint = constraint.replaceAll("[.$]*","");
                constraint = constraint.replaceAll("[}\\s,;.$]*$", "");
            }
        }

        if ( label != null ){
            Matcher m = CONST_LABEL_PATTERN.matcher(label);
            if ( m.matches() ){
                label = m.group(1);
                label = label.replaceAll("[{}$,;]","");
                label = label.replaceAll(ending_deletion, "");
            }
        }

        eq = eq.replaceAll(ending_deletion,"");
        String[] lrHS = eq.split("=");

        if ( lrHS.length == 2 ){
            return new TestCaseInLaTeX( lrHS[0], lrHS[1], constraint, label, counter[0]++ );
        } else {
            String[] f = null;
            String symb = null;
            if ( eq.contains("<") ){
                f = eq.split( "<" );
                symb = "<";
            } else if ( eq.contains("\\leq") ){
                f = eq.split( "\\\\leq" );
                symb = "<=";
            } else if ( eq.contains("\\le") ){
                f = eq.split( "\\\\le" );
                symb = "<=";
            } else if ( eq.contains( ">" ) ){
                f = eq.split( ">" );
                symb = ">";
            } else if ( eq.contains( "\\geq" ) ){
                f = eq.split( "\\\\geq" );
                symb = ">=";
            } else if ( eq.contains("\\ge") ){
                f = eq.split( "\\\\geq" );
                symb = ">=";
            } else if ( eq.contains( "\\neq" ) ){
                f = eq.split( "\\\\neq" );
                symb = "<>";
            }

            if ( f == null || f.length < 2 ){
                TestStatus.ERROR_LINE.add( counter[0] );
                LOG.info("Line cannot be analyzed: " + counter[0]++ );
                return null;
            }

            return new RelationInLaTeX(
                    "(" + f[0] + ") - (" + f[1] + ")",
                    symb,
                    constraint,
                    label,
                    counter[0]++
            );
        }
    }

    /**
     * @deprecated There are newer versions available to perform tests more reliable.
     * @see gov.nist.drmf.interpreter.evaluation.SymbolicEvaluator
     * @see gov.nist.drmf.interpreter.evaluation.NumericalEvaluator
     */
    @Disabled
    @TestFactory
    @Deprecated
    Iterable<DynamicTest> iterateAllTestCases(){
        LinkedList<DynamicTest> tests = new LinkedList<>();

        while( !testCases.isEmpty() ){
            TestCaseInLaTeX test = testCases.remove(0);
            tests.add(
                    DynamicTest.dynamicTest(
                            "Test Line: " + test.line,
                            createTestCase( test )
                    )
            );
        }

        return tests;
    }

    private Executable createTestCase( TestCaseInLaTeX test ){
        return () -> testFunction( test, test.assumption );
    }

    private String translateAssumption(String assumption, int line){
        String mapleAss = null;
        if ( assumption != null ){
            LOG.info("Found assumption: " + assumption);

            String[] asses = assumption.split("[,;]");
            String modAss = "";
            for ( int i = 0; i < asses.length; i++ ){
                modAss += splitAssumptions(asses[i]);
                if ( i < asses.length-1 )
                    modAss += ", ";
            }
            modAss = modAss.replaceAll("[\\s,;.{}]*$", "");

            try {
                LOG.info("Split assumption: " + modAss);
                mapleAss = mapleT.translateFromLaTeXToMapleSetModeClean(modAss);
                LOG.info("Translated assumption to: " + mapleAss);
            } catch ( Exception e ){
                LOG.error("Cannot translate assumption! -> Ignore Assumption, line " + line);
                e.printStackTrace();
                TestStatus.ERROR_IN_ASSUMPTION_TRANSLATION.add(line);
            }
        }
        return mapleAss;
    }

    private String splitAssumptions(String ass){
        Pattern p = Pattern.compile("\\s*(\\\\[lg]eq?|[<>=]).*");
        //String w = "0 < y \\leq z \\ge \\cpi, q > 0";
        String[] els = ass.split("(\\\\[lg]eq?|[<>=])");
        //System.out.println( Arrays.toString(els) );
        if ( els.length == 1 ) return ass;

        LinkedList<String> symbs = new LinkedList<>();
        Matcher m;
        for ( int i = 0; i < els.length; i++ ){
            //System.out.println(ass);
            String sub = ass.substring(els[i].length());
            //System.out.println(sub);
            m = p.matcher( sub );
            if ( m.matches() ) {
                String symb = m.group(1);
                symbs.add(symb);
                sub = sub.substring(symb.length());
            }
            ass = sub;
        }

        //System.out.println(symbs);
        String out = "";
        for ( int i = 0; i < els.length-1; i++ ){
            out += els[i];
            out += symbs.removeFirst();
            out += els[i+1];
            if ( i < els.length-2 )
                out += ", ";
        }

        return out;
    }

    private void testFunction( TestCaseInLaTeX test, String assumption ) throws MapleException, ComputerAlgebraSystemEngineException {
        if ( test.line == 458 ){
            TestStatus.NOT_SUCCESSFUL.add(test.line);
            fail("Skipped line 458!");
            return;
        }

        if ( test instanceof RelationInLaTeX ){
            relationTest( (RelationInLaTeX)test, assumption );
            return;
        }

        String mapleAss = translateAssumption( assumption, test.line );

        LOG.info(test.toString());
        String mapleLHS = null, mapleRHS = null;
        try {
            mapleLHS = mapleT.translateFromLaTeXToMapleClean(test.LHS);
            mapleRHS = mapleT.translateFromLaTeXToMapleClean(test.RHS);
        } catch ( TranslationException te ){
            handleTransExcInPreTrans( te, test );
            return;
        } catch ( Exception e ){
            TestStatus.ERROR_FORWARD_TRANS_UNKOWN.add(test.line);
            e.printStackTrace();
            fail("ERROR in line " + test.line + ", " + e.getMessage());
            return;
        }

        LOG.info("Translated to: " + mapleLHS + " = " + mapleRHS);
        PreAfterCommands pac = getPrevCommand( test.LHS + "-" + test.RHS, mapleAss );

        try {
            assertTrue(
                    simplifyTestEquation(
                            mapleLHS,
                            mapleRHS,
                            pac.pre,
                            pac.after,
                            test.line
                    ),
                    "Simplification fails: " + test.line
            );
        } catch ( MapleException me ){
            LOG.debug("Maple Error occurred, line " + test.line, me);
            TestStatus.ERROR_IN_MAPLE.add(test.line);
            fail("ERROR in line " + test.line + ", " + me.getMessage());
        }

        // garbage collection
        mapleT.forceGC();
    }

    private void relationTest(
            RelationInLaTeX test, String assumption )
            throws MapleException {
        String mapleAss = translateAssumption( assumption, test.line );

        LOG.info(test.toString());
        String mapleLHS = null, mapleRelationSymb = null;
        try {
            mapleLHS = mapleT.translateFromLaTeXToMapleClean(test.LHS);
            mapleRelationSymb = test.relationSymbol; //mapleT.translateFromLaTeXToMapleClean(test.relationSymbol);
        } catch ( TranslationException te ){
            handleTransExcInPreTrans( te, test );
        } catch ( Exception e ){
            TestStatus.ERROR_FORWARD_TRANS_UNKOWN.add(test.line);
            e.printStackTrace();
            fail("ERROR in line " + test.line + ", " + e.getMessage());
            return;
        }
        LOG.info("Translated to: " + mapleLHS + mapleRelationSymb + "0");

        PreAfterCommands pac = getPrevCommand( test.LHS, mapleAss );

        try {
            enterPreDefines(mapleLHS, null);

            if ( pac.pre != null ){
                LOG.info("Previous command: " + pac.pre);
                mapleT.enterMapleCommand(pac.pre);
            }

            RelationResults result =
                    mapleS.holdsRelation( mapleLHS + mapleRelationSymb + "0" );
            switch( result ){
                case TRUE:
                    TestStatus.SUCCESSFUL_INEQ.add( test.line );
                    return;
                case FALSE:
                    TestStatus.NOT_SUCCESSFUL.add( test.line );
                    fail("Not successful relation!");
                    return;
                case FAIL:
                    TestStatus.NOT_SUCCESSFUL.add( test.line );
                    fail("Not successful relation!");
                    return;
                case ERROR:
                    TestStatus.ERROR_IN_MAPLE.add( test.line );
                    fail("Error or Fail in relation test.");
            }
        } catch ( MapleException me ){
            LOG.debug("Maple Error occurred, line " + test.line, me);
            TestStatus.ERROR_IN_MAPLE.add(test.line);
        }
    }

    private PreAfterCommands getPrevCommand( String overAll, String mapleAss ){
        PreAfterCommands pac = new PreAfterCommands();
        if ( overAll.contains("\\Ferrer") ){
            pac.pre = MapleConstants.ENV_VAR_LEGENDRE_CUT_FERRER;
            pac.pre += System.lineSeparator();
            pac.pre += FERRER_DEF_ASS + System.lineSeparator();
            pac.after = MapleConstants.ENV_VAR_LEGENDRE_CUT_LEGENDRE;
            pac.after += System.lineSeparator() + RESET;
        } else if ( overAll.contains("\\Legendre") ){
            pac.pre = LEGENDRE_DEF_ASS;
            pac.after = RESET;
        }

        if ( pac.pre != null )
            pac.pre += mapleAss == null ?
                    "" :
                    "assume("+mapleAss+");" + System.lineSeparator();
        if ( pac.pre == null && mapleAss != null )
            pac.pre = "assume("+mapleAss+");" + System.lineSeparator();
        return pac;
    }



    private void handleTransExcInPreTrans( TranslationException te, TestCaseInLaTeX test ){
        TranslationExceptionReason r = te.getReason();
        if ( r != null && r.equals(TranslationExceptionReason.DLMF_MACRO_ERROR) ) {
            TestStatus.ERROR_UNKNOWN_MACRO.add(test.line);
            String uM = (String)te.getReasonObj();
            Integer i = unknown_macro_counter.get(uM);
            if ( i == null ){
                unknown_macro_counter.put(uM, 1);
            } else {
                unknown_macro_counter.put(uM, i+1);
            }
        } else TestStatus.ERROR_FORWARD_TRANS_UNKOWN.add(test.line);
        fail("Cannot translate equation: "
                + test.LHS + "=" + test.RHS + System.lineSeparator()
                + "Reason: " + te.getMessage()
        );
    }

    private void enterPreDefines(String mapleLHS, String mapleRHS) throws MapleException{
        /*
        if ( mapleRHS == null )
            mapleT.enterMapleCommand("tmp := '" + mapleLHS + "';");
        else
            mapleT.enterMapleCommand("tmp := '" + mapleLHS +"-"+ mapleRHS + "';");
        mapleT.enterMapleCommand("mySet := indets(tmp, name) minus {constants};");
        mapleT.enterMapleCommand("mySeq := seq('(op(myVar, mySet),Not(negative))', myVar = 1..nops(mySet), 1);");
//        mapleT.enterMapleCommand("mySeq := seq(Re(op(myVar,mySet))>0, myVar = 1..nops(mySet));");
        mapleT.enterMapleCommand("assume(mySeq);");
        */
    }

    private boolean simplifyTestEquation( String mapleLHS, String mapleRHS,
                                  String preCommand, String afterCommand,
                                  int line )
            throws ComputerAlgebraSystemEngineException, MapleException {
        enterPreDefines(mapleLHS, mapleRHS);

        if ( preCommand != null ){
            LOG.info("Previous command: " + preCommand);
            mapleT.enterMapleCommand(preCommand);
        }

        Algebraic a;
        boolean resultB = mapleS.isEquivalent(mapleLHS, mapleRHS);
        if ( resultB )
            TestStatus.SUCCESSFUL.add( line );

        if (!resultB) {
            a = mapleS.isMultipleEquivalent(mapleLHS, mapleRHS);
            if (a != null && a instanceof Numeric) {
                TestStatus.SUCCESSFUL_DIVIDE.add(line);
                resultB = true;
            } else resultB = false;
        }

        if ( !resultB ){
            resultB = mapleS.isEquivalentWithConversion(mapleLHS, mapleRHS, "exp");
            if ( resultB )
                TestStatus.SUCCESSFUL_EXP.add(line);
        }

        if (!resultB){
            a = mapleS.isMultipleEquivalentWithConversion(mapleLHS, mapleRHS, "exp");
            if ( a != null && a instanceof Numeric ) {
                TestStatus.SUCCESSFUL_EXP_DIVIDE.add(line);
                resultB = true;
            } else resultB = false;
        }

        if ( !resultB ) {
            resultB = mapleS.isEquivalentWithConversion(mapleLHS, mapleRHS, "hypergeom");
            if ( resultB )
                TestStatus.SUCCESSFUL_HYPER.add(line);
        }

        if (!resultB){
            a = mapleS.isMultipleEquivalentWithConversion(mapleLHS, mapleRHS, "hypergeom");
            if ( a != null && a instanceof Numeric ) {
                TestStatus.SUCCESSFUL_HYPER_DIVIDE.add(line);
                resultB = true;
            } else resultB = false;
        }

        if ( !resultB ) {
            resultB = mapleS.isEquivalentWithExpension(mapleLHS, mapleRHS, null);
            if ( resultB )
                TestStatus.SUCCESSFUL_EXPAND.add(line);
        }

        if (!resultB){
            a = mapleS.isMultipleEquivalentWithExpension(mapleLHS, mapleRHS, null);
            if ( a != null && a instanceof Numeric ) {
                TestStatus.SUCCESSFUL_HYPER_DIVIDE.add(line);
                resultB = true;
            } else resultB = false;
        }

        /*
        if (!resultB){ // TODO numerical test
            resultB = numericalTest("(" + mapleLHS + ") / (" + mapleRHS + ")", line);
            if ( !resultB ){
                resultB = numericalTest("(" + mapleRHS + ") / (" + mapleLHS + ")", line);
            }
        }
        */

        if ( !resultB )
            TestStatus.NOT_SUCCESSFUL.add(line);

        if ( afterCommand != null ){
            LOG.info("After command: " + afterCommand);
            mapleT.enterMapleCommand(afterCommand);
        }

        return resultB;
    }

    private boolean numericalTest(String test, int line) throws MapleException{
        boolean resultB = false;
        Algebraic a = mapleS.numericalMagic( test );
        if ( a != null )
            LOG.debug("Numerical Output: " + a.toString());
        else LOG.warn( "Hmm, numerical output is null..." );

        if ( a instanceof com.maplesoft.openmaple.List ){
            com.maplesoft.openmaple.List aList = (com.maplesoft.openmaple.List)a;
            if ( aList.length() == 0 ){
                TestStatus.SUCCESSFUL_NUMERICAL_MAGIC.add(line);
                resultB = true;
            } else {
                int l = aList.length();
                for ( int i = 1; i <= l; i++ ){
                    Algebraic e = aList.select(i);
                    if ( e instanceof com.maplesoft.openmaple.List ){
                        com.maplesoft.openmaple.List innerL = (com.maplesoft.openmaple.List)e;
                        String num = innerL.select(1).toString();
                        if ( num.matches( "[+-]?\\d*[.]?[^1-9]*" ) ){
                            TestStatus.SPECIAL_INTEREST.add(line);
                            break;
                        }
                    }
                }
            }
        }
        return resultB;
    }

    public static class PreAfterCommands{
        private String pre, after;
    }

    public static class RelationInLaTeX extends TestCaseInLaTeX{
        String relationSymbol;

        public RelationInLaTeX( String LHS, String relationSymbol, String assumption, String label, int line ){
            super( LHS, "0", assumption, label, line );
            this.relationSymbol = relationSymbol;
        }
    }

    public static class TestCaseInLaTeX{
        String LHS, RHS;
        int line;
        String assumption;
        String dlmf;

        public TestCaseInLaTeX( String LHS, String RHS, String assumption, String label, int line ){
            this.LHS = LHS;
            this.RHS = RHS;
            this.assumption = assumption;
            this.dlmf = link_librarie.get(label);
            //LOG.info(label + " -> " + dlmf);
            this.line = line;
        }

        @Override
        public String toString(){
            return line + ": " + LHS + "=" + RHS + " CONSTR=" + assumption + "; " + dlmf;
        }
    }

    private enum TestStatus {
        SUCCESSFUL(0),
        SUCCESSFUL_DIVIDE(0),
        SUCCESSFUL_INEQ(0),
        SUCCESSFUL_EXP(0),
        SUCCESSFUL_EXP_DIVIDE(0),
        SUCCESSFUL_HYPER(0),
        SUCCESSFUL_HYPER_DIVIDE(0),
        SUCCESSFUL_EXPAND(0),
        SUCCESSFUL_EXPAND_DIVIDE(0),
        SUCCESSFUL_NUMERICAL_MAGIC(0),
        NOT_SUCCESSFUL(0),
        ERROR_FORWARD_TRANS_UNKOWN(0),
        ERROR_IN_ASSUMPTION_TRANSLATION(0),
        ERROR_UNKNOWN_MACRO(0),
        ERROR_IN_MAPLE(0),
        ERROR_LINE(0),
        SINGLE_QUOTES(0),
        SPECIAL_INTEREST(0);

        int num;
        LinkedList<Integer> lines;
        LinkedList<String> represent;

        TestStatus(int num){
            this.num = num;
            this.lines = new LinkedList<>();
            this.represent = new LinkedList<>();
        }

        public void add( int line ){
            num++;
            lines.add(line);
            if ( represent.isEmpty() ){
                represent.add(""+line);
                return;
            }

            String l = represent.getLast();
            if ( l != null && !l.isEmpty() && l.endsWith(""+(line-1)) ){
                represent.removeLast();
                if ( l.contains("-") ){
                    String end = (line-1)+"";
                    l = l.substring(0, l.length()-end.length());
                    l += line;
                } else l += "-" + line;
                represent.addLast(l);
            } else represent.addLast(""+line);
        }

        public static void reset(){
            for ( TestStatus t : TestStatus.values() ){
                t.num = 0;
                t.lines = new LinkedList<>();
            }
        }

        private String state(){
            return num + ", Lines: " + represent.toString();
        }

        public static String stats(){
            String nl = System.lineSeparator();
            String tab = "\t";
            String out =
                    "Total Number of Lines with non-equation, non-relations: "
                            +nl+tab+ ERROR_LINE.state() + nl;
            out += "Total Number of Ignored Single Quotes ': "+nl+tab + SINGLE_QUOTES.state() + nl;
            out += "Total Number of Success:  " +
                    (SUCCESSFUL.num+SUCCESSFUL_EXP.num+SUCCESSFUL_HYPER.num+
                            SUCCESSFUL_EXPAND.num+SUCCESSFUL_INEQ.num)
                    + nl;
            out += "Total Number of Failures: " +
                    (NOT_SUCCESSFUL.num+ERROR_UNKNOWN_MACRO.num+ERROR_FORWARD_TRANS_UNKOWN.num+ERROR_IN_MAPLE.num)
                    + nl+nl;

            out += "// Exp-Conv: simplify(convert(..., exp));" +nl;
            out += "// Hypergeom-Conv: simplify(convert(..., hypergeom));" + nl;
            out += "// Expansion: simplify(expand(...));" + nl;
            out += "Successful Tests after Simplify:       " + SUCCESSFUL.state() + nl;
            out += "Successful Tests after Exp-Conv:       " + SUCCESSFUL_EXP.state() + nl;
            out += "Successful Tests after Hypergeom-Conv: " + SUCCESSFUL_HYPER.state() + nl;
            out += "Successful Tests after Expansion:      " + SUCCESSFUL_EXPAND.state() + nl;
            out += "Successful Tests of Relations (not =): " + SUCCESSFUL_INEQ.state() + nl + nl;

            out += "Successful Tests after Simplify-DIV:      " + SUCCESSFUL_DIVIDE.state() + nl;
            out += "Successful Tests after Exp-Conv-DIV:      " + SUCCESSFUL_EXP_DIVIDE.state() + nl;
            out += "Successful Tests after Hypergeom-Conv-DIV:" + SUCCESSFUL_HYPER_DIVIDE.state() + nl;
            out += "Successful Tests after Expansion-DIV:     " + SUCCESSFUL_EXPAND_DIVIDE.state() + nl + nl;

            out += "Successful Numerical Tests: " + SUCCESSFUL_NUMERICAL_MAGIC.state() + nl + nl;

            out += "Special Interest: " + SPECIAL_INTEREST.state() + nl + nl;

            out += "// No Errors, but each kind of simplify returns something different to 0!" + nl +
                    "Non-Successful Tests:   " + nl + tab +
                    NOT_SUCCESSFUL.state() + nl + nl;

            out += "DLMF/DRMF macro cannot be translated (see list below for details):" + nl + tab +
                    ERROR_UNKNOWN_MACRO.state() + nl;
            out += "Error during forward translation process: " + nl + tab +
                    ERROR_FORWARD_TRANS_UNKOWN.state() + nl;
            out += "Error in Maple: " + ERROR_IN_MAPLE.state() + nl;
            out += "Error in Constraints-Translations (not included in the sum of all errors):" + nl + tab +
                    ERROR_IN_ASSUMPTION_TRANSLATION.state() + nl;

            List<Map.Entry<String, Integer>> entries =
                    new ArrayList<>(unknown_macro_counter.entrySet());
            Collections.sort(
                    entries,
                    (o1,o2) -> Integer.compare(o2.getValue(),o1.getValue())
            );

            String unknM = "";
            for ( Map.Entry<String, Integer> e : entries ){
                unknM += tab + e.getKey() + ": " + e.getValue() + nl;
            }
            out += nl + "// The Unknown-Macros-Lib show used DLMF/DRMF macros, that cannot be translated."
                    + nl + "Unknown-Macros-Lib (macro: in how many lines it appears):"
                    + nl + unknM;

            return out;
        }
    }
}
