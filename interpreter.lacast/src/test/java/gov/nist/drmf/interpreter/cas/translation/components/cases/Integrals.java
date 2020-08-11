package gov.nist.drmf.interpreter.cas.translation.components.cases;

import gov.nist.drmf.interpreter.common.meta.DLMF;

/**
 * @author Andre Greiner-Petter
 */
public enum Integrals implements ForwardTestCase {
    SIMPLE(
            "\\int_{0}^{1} x \\diff{x}",
            "int(x, x = 0..1)",
            "Integrate[x, {x, 0, 1}]"
    ),
    SIMPLE_DIFFD(
            "\\int_{0}^{1} x \\diffd x",
            "int(x, x = 0..1)",
            "Integrate[x, {x, 0, 1}]"
    ),
    SIMPLE_INDEF(
            "\\int \\sin@{x} \\diff{x}",
            "int(sin(x), x)",
            "Integrate[Sin[x], x]"
    ),
    SIMPLE_REVERSE(
            "\\int^{1}_0 x \\diff{x}",
            "int(x, x = 0..1)",
            "Integrate[x, {x, 0, 1}]"
    ),
    SIMPLE_AFTER(
            "\\int_{0}^1 x \\diff{x} + x^2",
            "int(x, x = 0..1) + (x)^(2)",
            "Integrate[x, {x, 0, 1}] + (x)^(2)"
    ),
    SIMPLE_MULTI_INNER(
            "\\int_{0}^{1} \\int_{0}^{1} yx \\diff{y} \\diff{x}",
            "int(int(y*x, y = 0..1), x = 0..1)",
            "Integrate[Integrate[y*x, {y, 0, 1}], {x, 0, 1}]"
    ),
    SIMPLE_MULTI_CHAIN(
            "\\int_{0}^{1} x \\diff{x} \\int_{0}^{1} y \\diff{y}",
            "int(x, x = 0..1)*int(y, y = 0..1)",
            "Integrate[x, {x, 0, 1}]*Integrate[y, {y, 0, 1}]"
    ),
    SIMPLE_FRAC_DIFF(
            "\\int_{0}^{1} \\frac{\\diff{x}}{x}",
            "int((1)/(x), x = 0..1)",
            "Integrate[Divide[1, x], {x, 0, 1}]"
    ),
    SIMPLE_MULTI_INNER_FRAC_DIFF(
            "\\int_{0}^{1} \\int_{0}^{1} \\frac{\\diff{x} \\diff{y}}{xy}",
            "int(int((1)/(x*y), x = 0..1), y = 0..1)",
            "Integrate[Integrate[Divide[1, x*y], {x, 0, 1}], {y, 0, 1}]"
    ),
    SIMPLE_IINT(
            "\\iint_0^1 x \\diff{x}",
            "int(int(x, x = 0..1), x = 0..1)",
            "Integrate[Integrate[x, {x, 0, 1}], {x, 0, 1}]"
    ),
    SIMPLE_IIINT(
            "\\iiint_0^1 x \\diff{x}",
            "int(int(int(x, x = 0..1), x = 0..1), x = 0..1)",
            "Integrate[Integrate[Integrate[x, {x, 0, 1}], {x, 0, 1}], {x, 0, 1}]"
    ),
    SIMPLE_IIIINT(
            "\\iiiint_0^1 x \\diff{x}",
            "int(int(int(int(x, x = 0..1), x = 0..1), x = 0..1), x = 0..1)",
            "Integrate[Integrate[Integrate[Integrate[x, {x, 0, 1}], {x, 0, 1}], {x, 0, 1}], {x, 0, 1}]"
    ),
    @DLMF("6.7.13")
    DLMF_EQUAL(
            "\\int_0^{\\infty} \\frac{\\sin@{t}}{t+z} \\diff{t} = \\int_0^{\\infty} \\frac{\\expe^{-zt} \\diff{t}}{t^2+z}",
            "int((sin(t))/(t+z), t = 0..infinity) = int((exp(-z*t))/((t)^(2)+z), t = 0..infinity)",
            "Integrate[Divide[Sin[t], t+z], {t, 0, Infinity}] == Integrate[Divide[Exp[-z*t], (t)^(2)+z], {t, 0, Infinity}]"
    )
    ;

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
