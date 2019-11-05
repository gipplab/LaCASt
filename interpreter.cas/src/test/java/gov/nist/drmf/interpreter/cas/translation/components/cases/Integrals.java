package gov.nist.drmf.interpreter.cas.translation.components.cases;

/**
 * @author Andre Greiner-Petter
 */
public enum Integrals implements TestCase {
    SIMPLE(
            "\\int_{0}^{1} x \\mathrm{d}x",
            "int(x, x = 0..1)",
            "Integrate[x, {x, 0, 1}]"
    );

    private String tex, maple, mathematica;

    Integrals( String tex, String maple, String mathematica ) {
        this.tex = tex;
        this.maple = maple;
        this.mathematica = mathematica;
    }

    @Override
    public String getTitle() {
        return this.name();
    }

    @Override
    public String getTeX() {
        return tex;
    }

    @Override
    public String getMaple() {
        return maple;
    }

    @Override
    public String getMathematica() {
        return mathematica;
    }
}
