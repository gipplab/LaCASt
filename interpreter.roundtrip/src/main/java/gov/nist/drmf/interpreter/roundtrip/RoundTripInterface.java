package gov.nist.drmf.interpreter.roundtrip;

import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

/**
 * Created by AndreG-P on 01.03.2017.
 */
public class RoundTripInterface {

    private MapleInterface I_MAPLE;
    private SemanticLatexParser I_DLMF;

    public RoundTripInterface(){
        // set translation for DLMF-site
        GlobalConstants.CAS_KEY = Keys.KEY_MAPLE;
        I_DLMF  = new SemanticLatexParser( Keys.KEY_LATEX, Keys.KEY_MAPLE );
        I_MAPLE = new MapleInterface();
    }

    public void init(){
        try{
            I_MAPLE.init();
            I_DLMF.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    public String translateFromMaple( String mapleString ){
        try {
            return I_MAPLE.parse( mapleString );
        } catch ( Exception e ){
            e.printStackTrace();
            return null;
        }
    }

    public String translateFromLaTeX( String latexString ){
        I_DLMF.parse( latexString );
        return I_DLMF.getTranslatedExpression();
    }
}
