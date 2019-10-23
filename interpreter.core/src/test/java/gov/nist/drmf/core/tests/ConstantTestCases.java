package gov.nist.drmf.core.tests;

/**
 * @author Andre Greiner-Petter
 */
public enum ConstantTestCases {
    Pi( "\\cpi", "Pi", "Pi"),
    Exponential("\\expe", "exp(1)", "E"),
    ImaginaryUnit("\\iunit", "I", "I"),
    EulerConstant("\\EulerConstant", "gamma", "EulerGamma"),
    CatalansConstant("\\CatalansConstant", "Catalan", "Catalan"),
    Infinity("\\infty", "infinity", "Infinity");

    public String dlmf, maple, mathematica;

    ConstantTestCases(String dlmf, String maple, String mathematica){
        this.dlmf = dlmf;
        this.maple = maple;
        this.mathematica = mathematica;
    }
}
