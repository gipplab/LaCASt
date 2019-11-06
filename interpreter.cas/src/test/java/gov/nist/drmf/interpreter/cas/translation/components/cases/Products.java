package gov.nist.drmf.interpreter.cas.translation.components.cases;

import gov.nist.drmf.interpreter.common.meta.DLMF;

/**
 * @author Andre Greiner-Petter
 */
public enum Products implements TestCase {
    SIMPLE(
            "\\prod_{-\\infty < x < \\infty}x^3",
            "product((x)^(3), x=- infinity..infinity)",
            "Product[(x)^(3), {x, - Infinity, Infinity}]"
    ),
    TRICKY_INNER_PROD_AND_SUMS(
            "\\prod_{n \\leq i \\leq m}\\sin{i} + \\sum_{n \\leq j \\leq m}i^2+j\\prod_{k=0}^{\\infty}k+j+i",
            "product(sin(i)+sum((i)^(2)+j*product(k, k = 0..infinity)+j, j=n..m)+i, i=n..m)",
            "Product[Sin[i]+Sum[(i)^(2)+j*Product[k, {k, 0, Infinity}]+j, {j, n, m}]+i, {i, n, m}]"
    ),
    SIMPLE2(
            "\\prod_{i=0}^{\\infty}k^3",
            "product((k)^(3), i = 0..infinity)",
            "Product[(k)^(3), {i, 0, Infinity}]"
    ),
    SIMPLE_SET(
            "\\prod_{x \\in P}x^2+x^3-3",
            "product((x)^(2)+(x)^(3), x in P)-3",
            "Product[(x)^(2)+(x)^(3), {x, P}]-3"
    ),
    MULTI_PROD(
            "\\prod_{i=0}^{k}i^2+\\prod_{j=0}^{k}i^3-3j+\\prod_{l=0}^{k}j+2+\\sin{l}",
            "product((i)^(2)+product((i)^(3)-3*j+product(j+2+sin(l), l = 0..k), j = 0..k), i = 0..k)",
            "Product[(i)^(2)+Product[(i)^(3)-3*j+Product[j+2+Sin[l], {l, 0, k}], {j, 0, k}], {i, 0, k}]"
    ),
    SHARE_PROD(
            "\\prod_{i=0}^{10}i^2+\\prod_{i=2}^{12}i k",
            "product((i)^(2), i = 0..10)+product(i*k, i = 2..12)",
            "Product[(i)^(2) , {i, 0, 10}]+Product[i*k , {i, 2, 12}]"
    ),
    @DLMF("26.12.4")
    DLMF_MULTI( //26.12.4
            "\\prod_{h=1}^r \\prod_{j=1}^s \\frac{h+j+t-1}{h+j-1}",
            "product(product((h + j + t - 1)/(h + j - 1), j = 1..s), h = 1..r)",
            "Product[Product[Divide[h + j + t - 1,h + j - 1], {j, 1, s}], {h, 1, r}]"
    ),
    @DLMF("5.14.4")
    DLMF_MULTI_SPECIAL_FUNC( //5.14.4
            "\\prod_{k=1}^m \\frac{a+(n-k)c}{a+b+(2n-k-1)c} \\prod_{k=1}^n \\frac{\\EulerGamma@{a+(n-k)c} \\EulerGamma@{b+(n-k)c} \\EulerGamma@{1+kc}} {\\EulerGamma@{a+b+(2n-k-1)c}}",
            "product((a +(n - k)*c)/(a + b +(2*n - k - 1)*c), k = 1..m) * product((GAMMA(a +(n - k)* c)*GAMMA(b +(n - k)* c)*GAMMA(1 + k*c))/(GAMMA(a + b +(2*n - k - 1)* c)), k = 1..n)",
            "Product[Divide[a +(n - k)*c,a + b +(2*n - k - 1)*c], {k, 1, m}]*Product[Divide[Gamma[a +(n - k)* c]*Gamma[b +(n - k)* c]*Gamma[1 + k*c],Gamma[a + b +(2*n - k - 1)* c]], {k, 1, n}]"
    ),
    @DLMF("20.5.1")
    DLMF_SIMPLE_LONG( //20.5.1
            "\\prod_{n=1}^{\\infty} {\\left( 1 - q^{2n} \\right)} {\\left( 1 - 2 q^{2n} \\cos@{2z} + q^{4n} \\right)}",
            "product((1 - (q)^(2*n))*(1 - 2*(q)^(2*n)* cos(2*z) + (q)^(4*n)), n = 1..infinity)",
            "Product[(1 - (q)^(2*n))*(1 - 2*(q)^(2*n)* Cos[2*z] + (q)^(4*n)), {n, 1, Infinity}]"
    ),
    @DLMF("4.22.2")
    DLMF_SIMPLE( //4.22.2
            "\\prod_{n=1}^\\infty \\left( 1 - \\frac{4z^2}{(2n - 1)^2 \\pi^2} \\right)",
            "product(1 -(4*(z)^(2))/((2*n - 1)^(2)* (pi)^(2)), n = 1..infinity)",
            "Product[1 -Divide[4*(z)^(2),(2*n - 1)^(2)*(\\[Pi])^(2)], {n, 1, Infinity}]"
    ),
    @DLMF("27.4.1")
    DLMF_PROD_SUM( //27.4.1
            "\\prod_{p} \\left( 1 + \\sum_{r=1}^\\infty f(p^r) \\right)",
            "product(1 + sum(f*((p)^(r)), r = 1..infinity), p = -infinity..infinity)",
            "Product[1 + Sum[f*((p)^(r)), {r, 1, Infinity}], {p, -Infinity, Infinity}]"
    ),
    @DLMF("5.14.5")
    DLMF_MULTI_PROD( //5.14.5
            "\\prod_{k=1}^m (a + (n-k)c) \\frac{\\prod_{k=1}^n \\EulerGamma@{a+(n-k)c} \\EulerGamma@{1+kc}} {(\\EulerGamma@{1+c})^n}",
            "product((a +(n - k)*c)*(product(GAMMA(a +(n - k)* c)*GAMMA(1 + k*c), k = 1..n))/((GAMMA(1 + c))^(n)), k = 1..m)",
            "Product[(a +(n - k)*c)*Divide[Product[Gamma[a +(n - k)* c]*Gamma[1 + k*c], {k, 1, n}],(Gamma[1 + c])^(n)], {k, 1, m}]"
    ),
    @DLMF("17.2.49")
    DLMF_SIMPLE_3( //17.2.49 part 2
            "\\prod_{n=0}^\\infty \\frac{1}{(1 - q^{5n+1}) (1 - q^{5n+4})}",
            "product((1)/((1 - (q)^(5*n + 1))*(1 - (q)^(5*n + 4))), n = 0..infinity)",
            "Product[Divide[1,(1 - (q)^(5*n + 1))*(1 - (q)^(5*n + 4))], {n, 0, Infinity}]"
    ),
    @DLMF("23.8.7")
    DLMF_LONG_FRAC( //23.8.7
            "\\prod_{n=1}^\\infty \\frac{\\sin@{\\pi (2n \\omega_3 + z) / (2 \\omega_1)} \\sin@{\\pi (2n \\omega_3 - z) / (2 \\omega_1)}} {\\sin^2@{\\pi n \\omega_3 / \\omega_1}}",
            "product((sin(pi*(2*n*omega[3] + z)/(2*omega[1]))*sin(pi*(2*n*omega[3] - z)/(2*omega[1])))/((sin(pi*n*omega[3]/ omega[1]))^(2)), n = 1..infinity)",
            "Product[Divide[Sin[\\[Pi]*(2*n*Subscript[\\[Omega], 3] + z)/(2*Subscript[\\[Omega], 1])]*Sin[\\[Pi]*(2*n*Subscript[\\[Omega], 3] - z)/(2*Subscript[\\[Omega], 1])],(Sin[\\[Pi]*n*Subscript[\\[Omega], 3]/ Subscript[\\[Omega], 1]])^(2)], {n, 1, Infinity}]"
    ),
    @DLMF("3.4.3")
    DLMF_MULTI_PROD_INNER( //3.4.3
            "\\prod_{k = n_0}^{n_1}(t-k)+f^{(n+2)}(\\xi_1)\\prod_{k = n_0}^{n_1}(t-k)",
            "product(t - k, k = n[0]..n[1]) + (f)^(n+2)*(xi[1])*product(t-k,k = n[0]..n[1])",
            "Product[t - k, {k, Subscript[n, 0], Subscript[n, 1]}] + (f)^(n+2)*(Subscript[\\[Xi],1])*Product[t - k, {k, Subscript[n, 0], Subscript[n, 1]}]"
    );

    private String tex, maple, mathematica;

    Products( String tex, String maple, String mathematica ) {
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
