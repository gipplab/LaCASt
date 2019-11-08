package gov.nist.drmf.interpreter.cas.translation.components.cases;

import gov.nist.drmf.interpreter.common.meta.DLMF;

/**
 * @author Andre Greiner-Petter
 */
public enum Lims implements ForwardTestCase {
    @DLMF("4.31.1")
    FRAC(
            "\\lim_{z \\to 0} \\frac{\\sinh@@{z}}{z}",
            "limit((sinh(z))/(z), z = 0)",
            "Limit[Divide[Sinh[z],z], z -> 0]"
    ),
    @DLMF("4.31.3")
    FRAC_2(
            "\\lim_{z \\to 0} \\frac{\\cosh@@{z} - 1}{z^2}",
            "limit((cosh(z)- 1)/((z)^(2)), z = 0)",
            "Limit[Divide[Cosh[z] - 1,(z)^(2)], z -> 0]"
    ),
    @DLMF("4.4.13")
    MULTIPLY_ARGS(
            "\\lim_{x \\to \\infty} x^{-a} \\ln@@{x}",
            "limit((x)^(- a)*ln(x), x = infinity)",
            "Limit[(x)^(- a)*Log[x], x -> Infinity]"
    ),
    @DLMF("4.4.17")
    PARA_ARGS(
            "\\lim_{n \\to \\infty} \\left( 1 + \\frac{z}{n} \\right)^n",
            "limit((1 +(z)/(n))^(n), n = infinity)",
            "Limit[(1 +Divide[z,n])^(n), n -> Infinity]"
    ),
    @DLMF("6.2.14")
    SIN_INT(
            "\\lim_{x \\to \\infty} \\sinint@{x} = \\frac{1}{2} \\cpi",
            "limit(Si(x), x = infinity) = (1)/(2)*Pi",
            "Limit[SinIntegral[x], x -> Infinity] = Divide[1,2]*Pi"
    ),
    @DLMF("6.2.14")
    COS_INT(
            "\\lim_{x \\to \\infty} \\cosint@{x} = 0",
            "limit(Ci(x), x = infinity) = 0",
            "Limit[CosIntegral[x], x -> Infinity] = 0"
    ),
    @DLMF("22.12.4")
    LONG_MULTI(
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\cpi}{\\tan@{\\cpi (t - (n+\\frac{1}{2}) \\tau)}} = \\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            "limit(sum((-1)^(n)*(Pi)/(tan(Pi*(t - (n+(1)/(2))*tau))), n = - N..N), N = infinity) = limit(sum((-1)^(n)*(limit(sum((1)/(t - m -(n +(1)/(2))*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "Limit[Sum[(-1)^(n)*Divide[Pi,Tan[Pi*(t - (n+Divide[1,2])*\\[Tau])]], {n, -N, N}], N -> Infinity] = Limit[Sum[(-1)^(n)*(Limit[Sum[Divide[1,t - m -(n +Divide[1,2])*\\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]"
    ),
    @DLMF("22.12.4")
    LONG_MULTI_PART_1(
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\cpi}{\\tan@{\\cpi (t - (n+\\frac{1}{2}) \\tau)}}",
            "limit(sum((-1)^(n)*(Pi)/(tan(Pi*(t - (n+(1)/(2))*tau))), n = - N..N), N = infinity)",
            "Limit[Sum[(-1)^(n)*Divide[Pi,Tan[Pi*(t - (n+Divide[1,2])*\\[Tau])]], {n, -N, N}], N -> Infinity]"
    ),
    @DLMF("22.12.4")
    LONG_MULTI_PART_2(
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m -(n +(1)/(2))*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "Limit[Sum[(-1)^(n)*(Limit[Sum[Divide[1,t - m -(n +Divide[1,2])*\\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]"
    ),
    @DLMF("22.12.13")
    LONG_PARA(
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t-n\\tau)}} = \\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - n \\tau} \\right)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t-n*tau))), n = -N .. N), N = infinity) = limit(sum((-1)^(n)*(limit(sum((1)/(t - m - n*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "Limit[Sum[(-1)^(n)*Divide[\\[Pi], Tan[\\[Pi]*(t-n*\\[Tau])]], {n, -N, N}], N -> Infinity] = Limit[Sum[(-1)^(n)*(Limit[Sum[Divide[1,t - m - n*\\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]"
    ),
    @DLMF("22.12.13")
    LONG_PARA_PART_1(
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t-n\\tau)}}",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t-n*tau))), n = -N .. N), N = infinity)",
            "Limit[Sum[(-1)^(n)*Divide[\\[Pi], Tan[\\[Pi]*(t-n*\\[Tau])]], {n, -N, N}], N -> Infinity]"
    ),
    @DLMF("22.12.13")
    LONG_PARA_PART_2(
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - n \\tau} \\right)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m - n*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "Limit[Sum[(-1)^(n)*(Limit[Sum[Divide[1,t - m - n*\\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]"
    ),
    @DLMF("20.5.15")
    LONG_PROD(
            "\\lim_{N \\to \\infty} \\prod_{n=-N}^{N} \\lim_{M \\to \\infty} \\prod_{m=1-M}^{M} \\left( 1 + \\frac{z}{(m - \\tfrac{1}{2} + n \\tau) \\cpi} \\right)",
            "limit(product(limit(product(1+(z)/((m-(1)/(2)+n*tau)*Pi),m = 1-M .. M),M = infinity),n = -N .. N), N = infinity)",
            "Limit[Product[Limit[Product[1+Divide[z, (m - Divide[1,2] + n*\\[Tau])*Pi], {m, 1-M, M}], M -> Infinity], {n, -N, N}], N -> Infinity]"
    );

    private String tex, maple, mathematica;

    Lims( String tex, String maple, String mathematica ) {
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
