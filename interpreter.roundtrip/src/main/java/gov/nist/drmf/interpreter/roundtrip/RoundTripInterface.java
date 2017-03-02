package gov.nist.drmf.interpreter.roundtrip;

import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by AndreG-P on 01.03.2017.
 */
public class RoundTripInterface {
    public static final Logger LOG = LogManager.getLogger( RoundTripInterface.class );
    private final String MAPLE_VALIDATION = "simplify";

    private MapleInterface I_MAPLE;
    private SemanticLatexTranslator I_DLMF;

    public RoundTripInterface(){
        // set translation for DLMF-site
        GlobalConstants.CAS_KEY = Keys.KEY_MAPLE;
        I_DLMF  = new SemanticLatexTranslator( Keys.KEY_LATEX, Keys.KEY_MAPLE );
        I_MAPLE = new MapleInterface();
    }

    public void init() throws Exception {
        I_MAPLE.init();
        I_DLMF.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    public static void main( String[] args ){
        RoundTripInterface ri = new RoundTripInterface();
        try {
            ri.init();
            String test = "(infinity+Catalan/2)^gamma";
//            test = "gamma+alpha^5-I^(x/5)+Catalan";
//            test = "x + x^2 + ((1-gamma)*x/2)^I";
//            test = "gamma + alpha^(5) +(1 * I)^(x *(1)/(5)) *(- 1)+ Catalan";
            test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
//            test = "((x^a)^b)^c";
            //test = "";

            String latex_result = ri.translateFromMaple( test );
            String back_to_maple = ri.translateFromLaTeX( latex_result );
            boolean equ = ri.equivalenceValidationMaple( test, back_to_maple );

            System.out.println( "Original Input:    " + test );
            System.out.println( "LaTeX Translation: " + latex_result );
            System.out.println( "Maple Translation: " + back_to_maple );
            System.out.println( "Simplifies to 0:   " + (equ ? "yes" : "no") );
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    public synchronized String translateFromMaple( String mapleString ){
        try {
            return I_MAPLE.parse( mapleString );
        } catch ( Exception e ){
            e.printStackTrace();
            return null;
        }
    }

    public synchronized String translateFromLaTeX( String latexString ){
        I_DLMF.parse( latexString );
        return I_DLMF.getTranslatedExpression();
    }

    public synchronized boolean equivalenceValidationMaple( String exp1, String exp2 ) throws Exception {
        try {
            String cmd = MAPLE_VALIDATION + "((" + exp1 + ") - (" + exp2 + "))";
            cmd = I_MAPLE.getProcedureName() + "(ToInert(" + cmd + "));";

            LOG.info( cmd );
            Algebraic result = MapleInterface.evaluateExpression( cmd );
            LOG.info( result.toString() );

            if ( result instanceof List ){
                List l = (List)result;
                if ( l.length() != 2 ) return false;
                Algebraic a = l.select( 2 );
                if ( a instanceof Numeric ){
                    Numeric n = (Numeric)a;
                    return n.intValue() == 0;
                } else return false;
            } else return false;
        } catch ( Exception e ){
            throw e;
        }
    }
}
