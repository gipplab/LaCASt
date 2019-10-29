package gov.nist.drmf.interpreter.cas.translation.components.cases;

/**
 * @author Andre Greiner-Petter
 */
public enum Sums implements TestCase {
    SIMPLE(
            "\\sum_{n=0}^{10} n",
            "sum(n, n = 0..10)",
            "Sum[n, {n, 0, 10}]"
    ),
    SIMPLE_BEFORE(
            "\\cpi + \\sum_{n=0}^{10} n",
            "Pi + sum(n, n = 0..10)",
            "Pi + Sum[n, {n, 0, 10}]"
    ),
    SIMPLE_AFTER(
            "\\sum_{n=0}^{10} n + \\cpi",
            "sum(n, n = 0..10) + Pi",
            "Sum[n, {n, 0, 10}] + Pi"
    ),
    SIMPLE_AFTER_AND_AFTER(
            "\\cpi + \\sum_{n=0}^{10} n + \\cpi",
            "Pi + sum(n, n = 0..10) + Pi",
            "Pi + Sum[n, {n, 0, 10}] + Pi"
    ),
    SIMPLE_AFTER_INCLUDED(
            "\\sum_{n=0}^{10} n + \\cpi n",
            "sum(n + Pi*n, n = 0..10)",
            "Sum[n + Pi n, {n, 0, 10}]"
    ),
    SIMPLE_AFTER_INCLUDED_PARTIALLY(
            "\\sum_{n=0}^{10} n + \\cpi n + \\cpi",
            "sum(n + Pi*n, n = 0..10) + Pi",
            "Sum[n + Pi n, {n, 0, 10}] + Pi"
    ),
    SIMPLE_AFTER_GAP_INCLUDED(
            "\\sum_{n=0}^{10} n + \\cpi + \\cpi n",
            "sum(n + Pi + Pi*n, n = 0..10)",
            "Sum[n + Pi + Pi n, {n, 0, 10}]"
    ),
    SIMPLE_STOPPER(
            "\\sum_{n=0}^{10} n = 2 n",
            "sum(n, n = 0..10) = 2*n",
            "Sum[n, {n, 0, 10}] = 2 n"
    ),
    SIMPLE_MULTI_SUM_INSIDE(
            "\\sum_{n=0}^{10} \\sum_{k=0}^{10} k n",
            "sum(sum(k*n, k = 0..10), n = 0..10)",
            "Sum[Sum[k n, {k, 0, 10}], {n, 0, 10}]"
    ),
    SIMPLE_MULTI_SUM_INSIDE_GAP(
            "\\sum_{n=0}^{10} 1 + \\sum_{k=0}^{10} k n",
            "sum(1 + sum(k*n, k = 0..10), n = 0..10)",
            "Sum[1 + Sum[k n, {k, 0, 10}], {n, 0, 10}]"
    ),
    SIMPLE_MULTI_SUM_OUTSIDE(
            "\\sum_{n=0}^{10} n \\sum_{k=0}^{10} k",
            "sum(n, n = 0..10)*sum(k, k = 0..10)",
            "Sum[n, {n, 0, 10}] Sum[k, {k, 0, 10}]"
    ),
    SUB_ONLY_RANGE(
            "\\sum_{-100 \\leq i < 100}i^2+2i+1",
            "sum((i)^(2)+2i, i=-100..100-1)+1",
            "Sum[(i)^(2)+2i, {i, -100, 100-1}]+1"
    ),
    NORM_MULTIPLE_SUMMANDS_FUNCS(
            "\\sum_{i=0}^\\infty i^2\\log{i}^3+i(2+3)",
            "sum((i)^(2)*(log(i))^(3)+i(2 + 3), i = 0..infinity)",
            "Sum[(i)^(2) (Log[i])^(3)+i(2 + 3), {i, 0, Infinity}]"
    ),
    NORM_MULTIPLE_SUMMANDS_EASY(
            "\\sum^{200}_{k=-3}3i+k+i^2",
            "sum(3i+k, k = - 3..200)",
            "Sum[3i+k, {k, -3, 200}]"
    ),
    MULTI_VAR_SUM_MULTIPLE_SUMMANDS(
            "\\sum_{x, y = -\\infty}^{\\infty}\\sin{2^x}+\\cos{2^y}+23",
            "sum(sum(sin((2)^(x))+cos((2)^(y)), y=-infinity..infinity), x=-infinity..infinity)",
            "Sum[Sum[Sin[(2)^(x)]+Cos[(2)^(y)], {y, -Infinity, Infinity}], {x, -Infinity, Infinity}]"
    ),
    NORM_MULTIPLE_SUMMANDS_ARG_GAP(
            "\\sum_{j=0}^{r}\\tan{x^3}^2\\sin{j}+\\frac{2^j}{x-3}",
            "sum((tan((x)^(3)))^(2)*sin(j)+((2)^(j))/(x - 3), j = 0..r)",
            "Sum[(Tan[(x)^(3)])^(2) Sin[j]+Divide[(2)^(j),x - 3], {j, 0, r}]"
    ),
    NORM_MULTIPLE_SUMMANDS_ARG_GAP_2(
            "\\sum_{x=-\\infty}^{\\infty}x^2(x+2)(y^3-3)-2x+y-2",
            "sum((x)^(2)*(x + 2)((y)^(3) - 3)-2x, x = - infinity..infinity)",
            "Sum[(x)^(2) (x + 2)((y)^(3) - 3)-2x, {x, -Infinity, Infinity}]"
    ),
    NORM_MULTIPLE_SUMMANDS_HARD(
            "\\sum_{n=1}^{\\infty}\\frac{(-1)^{n}2^{2n-1}B_{2n}}{n(2n)!}z^{2n}",
            "sum(((- 1)^(n)* (2)^(2*n - 1)* B[2*n])/(n*factorial((2*n)))(z)^(2*n), n = 1..infinity)",
            "Sum[Divide[(- 1)^(n)  (2)^(2 n - 1)  Subscript[B, 2 n],n (2 n)!](z)^(2 n), {n, 1, Infinity}]"
    ),
    NORM_MULTIPLE_SUMMANDS_HARD_FRAC(
            "\\sum^{50}_{r=0}r\\cos{\\Theta}r(3r^2-3)/23x+3q",
            "sum(rcos(Theta)r(3*(r)^(2) - 3)/23x, r = 0..50)",
            "Sum[rCos[\\[CapitalTheta]]r(3 (r)^(2) - 3)/23x, {r, 0, 50}]"
    ),
    NORM_MULTIPLE_SUMMANDS_HARD_LONG(
            "\\sum_{x=0}^{\\infty}x^3(3x+2y)^{25x^2}(x+2)x^2(x+3)+2x(x+2)^2",
            "sum((x)^(3)*(3*x + 2*y)^(25*(x)^(2))*(x + 2)(x)^(2)*(x + 3)+2x(x + 2)^(2), x = 0..infinity)",
            "Sum[(x)^(3) (3 x + 2 y)^(25 (x)^(2)) (x + 2)(x)^(2) (x + 3)+2x(x + 2)^(2), {x, 0, Infinity}]"
    ),
    DLMF_NORM_EASY( // 6.6.5
            "\\sum_{n=0}^\\infty \\frac{\\opminus^n z^{2n+1}}{(2n+1)!(2n+1)}",
            "sum(((-1)^(n)* (z)^(2*n + 1))/(factorial((2*n + 1))*(2*n + 1)), n = 0..infinity)",
            "Sum[Divide[(-1)^(n)  (z)^(2 n + 1),(2 n + 1)!(2 n + 1)], {n, 0, Infinity}]"
    ),
    DLMF_ADVANCED_MULTI_ARG_EASY( // 29.6.36
            "\\sum_{p \\hiderel{=} 0}^{\\infty} (2p+1) B_{2p+1}",
            "sum((2*p + 1)B[2*p + 1], p = 0..infinity)",
            "Sum[(2 p + 1)Subscript[B, 2 p + 1], {p, 0, Infinity}]"
    ),
    DLMF_MULTI_SUM_LONG( //22.12.2 all
            "\\sum_{n=-\\infty}^{\\infty} \\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}} = \\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty} \\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            "sum((pi)/(sin(pi*(t -(n +(1)/(2))tau))), n = - infinity..infinity) = sum((sum(((- 1)^(m))/(t - m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}] = Sum[(Sum[Divide[(- 1)^(m),t - m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]"
    ),
    DLMF_MULTI_SUM_LONG_PART1( //22.12.2 part 1
            "\\sum_{n=-\\infty}^{\\infty}\\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}",
            "sum((pi)/(sin(pi*(t -(n +(1)/(2))tau))), n = - infinity..infinity)",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]"
    ),
    DLMF_MULTI_SUM_LONG_PART2( //22.12.2 part 2
            "\\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty}\\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau}\\right)",
            "sum((sum(((- 1)^(m))/(t - m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "Sum[(Sum[Divide[(- 1)^(m),t - m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]"
    ),
    DLMF_MULTI_SUM_SPECIAL_FUNC( //10.23.4 all
            "\\sum_{k \\hiderel{=} 0}^{2n} \\opminus^k \\BesselJ{k}@{z} \\BesselJ{2n-k}@{z} + 2 \\sum_{k \\hiderel{=} 1}^\\infty \\BesselJ{k}@{z} \\BesselJ{2n+k}@{z} = 0",
            "sum((-1)^(k)*BesselJ(k, z)BesselJ(2*n - k, z), k = 0..2*n) + 2*sum(BesselJ(k, z)BesselJ(2*n + k, z), k = 1..infinity) = 0",
            "Sum[(-1)^(k) BesselJ[k, z]BesselJ[2 n - k, z], {k, 0, 2 n}] + 2 Sum[BesselJ[k, z]BesselJ[2 n + k, z], {k, 1, Infinity}] = 0"
    ),
    DLMF_MULTI_SUM_SPECIAL_FUNC_PART1( //10.23.4 part 1
            "\\sum_{k \\hiderel{=} 0}^{2n} \\opminus^k \\BesselJ{k}@{z} \\BesselJ{2n-k}@{z}",
            "sum((-1)^(k)*BesselJ(k, z)BesselJ(2*n - k, z), k = 0..2*n)",
            "Sum[(-1)^(k) BesselJ[k, z]BesselJ[2 n - k, z], {k, 0, 2 n}]"
    ),
    DLMF_MULTI_SUM_SPECIAL_FUNC_PART2( //10.23.4 part 2
            "\\sum_{k \\hiderel{=} 1}^\\infty \\BesselJ{k}@{z} \\BesselJ{2n+k}@{z}",
            "sum(BesselJ(k, z)BesselJ(2*n + k, z), k = 1..infinity)",
            "Sum[BesselJ[k, z]BesselJ[2 n + k, z], {k, 1, Infinity}]"
    ),
    DLMF_MULTI_SUM_SPECIAL_FUNC_LONG( //10.23.17 all
            "\\sum_{k=0}^{n-1} \\frac{(\\tfrac{1}{2} z)^k \\BesselJ{k}@{z}}{k! (n-k)} + \\sum_{m=1}^\\infty \\opminus^m \\frac{(n+2m) \\BesselJ{n+2m}@{z}}{m (n+m)}",
            "sum((((1)/(2)*z)^(k)* BesselJ(k, z))/(factorial(k)*(n - k)), k = 0..n - 1) + sum((-1)^(m)*((n + 2*m)*BesselJ(n + 2*m, z))/(m*(n + m)), m = 1..infinity)",
            "Sum[Divide[(Divide[1,2] z)^(k)  BesselJ[k, z],k!(n - k)], {k, 0, n - 1}] + Sum[(-1)^(k) Divide[(n + 2 k) BesselJ[n + 2 k, z],k (n + k)], {k, 1, Infinity}]"
    ),
    DLMF_MULTI_SUM_SPECIAL_FUNC_LONG_PART1( //10.23.17 part 1
            "\\sum_{k=0}^{n-1} \\frac{(\\tfrac{1}{2} z)^k \\BesselJ{k}@{z}}{k! (n-k)}",
            "sum((((1)/(2)*z)^(k)* BesselJ(k, z))/(factorial(k)*(n - k)), k = 0..n - 1)",
            "Sum[Divide[(Divide[1,2] z)^(k)  BesselJ[k, z],k!(n - k)], {k, 0, n - 1}]"
    ),
    DLMF_MULTI_SUM_SPECIAL_FUNC_LONG_PART2( //10.23.17 part 2
            "\\sum_{k=1}^\\infty \\opminus^k \\frac{(n+2k) \\BesselJ{n+2k}@{z}}{k (n+k)}",
            "sum((-1)^(k)*((n + 2*k)*BesselJ(n + 2*k, z))/(k*(n + k)), k = 1..infinity)",
            "Sum[(-1)^(k) Divide[(n + 2 k) BesselJ[n + 2 k, z],k (n + k)], {k, 1, Infinity}]"
    ),
    DLMF_NORM_EASY_2( //22.12.5 part 1
            "\\sum_{n=-\\infty}^{\\infty}\\frac{\\pi}{\\sin@{\\pi (t+\\frac{1}{2}-(n+\\frac{1}{2}) \\tau)}}",
            "sum((pi)/(sin(pi*(t +(1)/(2)-(n +(1)/(2))tau))), n = - infinity..infinity)",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t +Divide[1,2]-(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]"
    ),
    DLMF_INNER_SUMS( //22.12.5 part 2
            "\\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty} \\frac{(-1)^m}{t + \\frac{1}{2} - m - (n+\\frac{1}{2}) \\tau}\\right)",
            "sum((sum(((- 1)^(m))/(t +(1)/(2)- m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "Sum[(Sum[Divide[(- 1)^(m),t +Divide[1,2]- m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]"
    ),
    DLMF_NORM_POCHHAMMER( //35.7.3
            "\\sum_{k=0}^\\infty\\frac{\\Pochhammersym{a}{k} \\Pochhammersym{c-a}{k}\\Pochhammersym{b}{k} \\Pochhammersym{c-b}{k}}{k! \\, \\Pochhammersym{c}{2k} \\Pochhammersym{c-\\tfrac{1}{2}}{k}}(t_1 t_2)^k",
            "sum((pochhammer(a, k)*pochhammer(c - a, k)*pochhammer(b, k)*pochhammer(c - b, k))/(factorial(k)*pochhammer(c, 2*k)*pochhammer(c -(1)/(2), k))(t[1] t[2])^(k), k = 0..infinity)",
            "Sum[Divide[Pochhammer[a, k] Pochhammer[c - a, k] Pochhammer[b, k] Pochhammer[c - b, k],k! Pochhammer[c, 2 k] Pochhammer[c -Divide[1,2], k]](Subscript[t, 1] Subscript[t, 2])^(k), {k, 0, Infinity}]"
    ),
    DLMF_INNER_SUMS_TRICKY( //25.16.11
            "\\sum_{n=1}^\\infty \\frac{1}{n^s} \\sum_{m=1}^n \\frac{1}{m^z}",
            "sum((1)/((n)^(s))sum((1)/((m)^(z)), m = 1..n), n = 1..infinity)",
            "Sum[Divide[1,(n)^(s)]Sum[Divide[1,(m)^(z)], {m, 1, n}], {n, 1, Infinity}]"
    ),
    DLMF_SET_INDEX( //18.2.6
            "\\sum_{x \\in X} x \\left( p_n(x) \\right)^2 w_x",
            "sum(x(p[n](x))^(2)*w[x], x in X)",
            "Sum[x(Subscript[p, n](x))^(2) Subscript[w, x], {x, X}]"
    ),
    DLMF_SUM_CHAIN_SPECIAL_FUNC( //16.11.2
            "\\sum_{m=1}^p \\sum_{k=0}^\\infty \\frac{\\opminus^k}{k!} \\EulerGamma@{a_m + k} \\left(\\frac{\\prod_{\\ell=1}^p \\EulerGamma@{a_\\ell - a_m - k}} {\\prod_{\\ell=1}^q \\EulerGamma@{b_\\ell - a_m - k}}\\right) z^{-a_m - k}",
            "sum(sum(((-1)^(k))/(factorial(k))GAMMA(a[m] + k)((product(GAMMA(a[ell] - a[m] - k), ell = 1..p))/(product(GAMMA(b[ell] - a[m] - k), ell = 1..q)))(z)^(- a[m] - k), k = 0..infinity), m = 1..p)",
            "Sum[Sum[Divide[(-1)^(k),k!]Gamma[Subscript[a, m] + k](Divide[Product[Gamma[Subscript[a, \\[ScriptL]] - Subscript[a, m] - k], {\\[ScriptL], 1, p}],Product[Gamma[Subscript[b, \\[ScriptL]] - Subscript[a, m] - k], {\\[ScriptL], 1, q}]])(z)^(- Subscript[a, m] - k), {k, 0, Infinity}], {m, 1, p}]"
    ),
    DLMF_NORM_DOTS( //17.2.49
            "\\sum_{n \\hiderel{=} 1}^\\infty \\frac{q^{n^2}}{(1 - q) (1 - q^2) \\cdots (1 - q^n)}",
            "sum(((q)^((n)^(2)))/((1 - q)*(1 - (q)^(2))..(1 - (q)^(n))), n = 1..infinity)",
            "Sum[Divide[(q)^((n)^(2)),(1 - q) (1 - (q)^(2)) ... (1 - (q)^(n))], {n, 1, Infinity}]"
    ),
    DLMF_NORM_SPECIAL_FUNC( //8.15.1
            "\\sum_{k=0}^\\infty \\incgamma@{a+k}{x} \\frac{(1-\\lambda)^k}{k!}",
            "sum(GAMMA(a + k)-GAMMA(a + k, x)((1 - lambda)^(k))/(factorial(k)), k = 0..infinity)",
            "Sum[Gamma[a + k, 0, x]Divide[(1 - \\[Lambda])^(k),k!], {k, 0, Infinity}]"
    ),
    DLMF_NORM_ABSTRACT( //26.18.3
            "\\sum_{t \\hiderel{=} 1}^n \\opminus^t r_t(B) (n-t)!",
            "sum((-1)^(t)*r[t](B)factorial((n - t)), t = 1..n)",
            "Sum[(-1)^(t) Subscript[r, t](B)(n - t)!, {t, 1, n}]"
    ),
    DLMF_NORM_MULTIPLE_SUM( //20.6.8
            "\\sum_{n=-\\infty}^{\\infty} \\sum_{m=-\\infty}^{\\infty} (m - \\tfrac{1}{2} + (n-\\tfrac{1}{2}) \\tau)^{-2j}",
            "sum(sum((m -(1)/(2)+(n -(1)/(2))tau)^(- 2*j), m = - infinity..infinity), n = - infinity..infinity)",
            "Sum[Sum[(m -Divide[1,2]+(n -Divide[1,2])\\[Tau])^(- 2 j), {m, -Infinity, Infinity}], {n, -Infinity, Infinity}]"
    ),
    DLMF_NORM_POWER_SUMMANDS( //20.11.3
            "\\sum_{n=-\\infty}^\\infty a^{n(n+1)/2} b^{n(n-1)/2}",
            "sum((a)^(n*(n + 1)/ 2)*(b)^(n*(n - 1)/ 2), n = - infinity..infinity)",
            "Sum[(a)^(n (n + 1)/ 2) (b)^(n (n - 1)/ 2), {n, -Infinity, Infinity}]"
    ),
    DLMF_NORM_LONG_SIMPLE( //7.6.4
            "\\sum_{n=0}^\\infty \\frac{\\opminus^n (\\frac{1}{2} \\pi)^{2n}}{(2n)! (4n+1)} z^{4n+1}",
            "sum(((-1)^(n)*((1)/(2)*pi)^(2*n))/(factorial((2*n))*(4*n + 1))(z)^(4*n + 1), n = 0..infinity)",
            "Sum[Divide[(-1)^(n)  (Divide[1,2] \\[Pi])^(2 n),(2 n)!(4 n + 1)](z)^(4 n + 1), {n, 0, Infinity}]"
    ),
    DLMF_NORM_MULTIPLE_SUM_LONG( //24.4.24 with removed \choose
            "\\sum_{k=1}^n \\sum_{j=0}^{k-1} \\opminus^j\\left( \\sum_{r=1}^{m-1} \\frac{e^{2\\cpi i (k-j) r/m}}{(1 - e^{2\\cpi ir/m})^n} \\right) (j+m*x)^{n-1}",
            "sum(sum((-1)^(j)*(sum(((e)^(2*pi*i*(k - j)*r/ m))/((1 - (e)^(2*pi*i*r/ m))^(n)), r = 1..m - 1))(j + m * x)^(n - 1), j = 0..k - 1), k = 1..n)",
            "Sum[Sum[(-1)^(j) (Sum[Divide[(e)^(2 \\[Pi] i (k - j) r/ m),(1 - (e)^(2 \\[Pi] i r/ m))^(n)], {r, 1, m - 1}])(j + m   x)^(n - 1), {j, 0, k - 1}], {k, 1, n}]"
    ),
    DLMF_NORM_SUM_PROD_HARD( //31.15.1
            "\\sum_{p=1}^{\\infty}{\\deriv[2]{w}{z} + \\left( \\sum_{j=1}^N \\frac{\\gamma_j}{z - a_j} \\right) \\deriv{w}{z} + \\frac{\\Phi(z)}{\\prod_{j=1}^N (z - a_j)} w}",
            "sum(diff(w, [z$(2)])+(sum((gamma[j])/(z - a[j]), j = 1..N))*diff(w, z)+(Phi*(z))/(product((z - a[j]), j = 1..N))*w, p = 1..infinity)",
            "Sum[D[w, {z, 2}] +(Sum[Divide[Subscript[\\[Gamma], j],z - Subscript[a, j]], {j, 1, N}]) D[w, z] +Divide[\\[CapitalPhi] (z),Product[(z - Subscript[a, j]), {j, 1, N}]] w, {p, 1, Infinity}]"
    ),
    DLMF_MULTI_VAR_EQUALITY( //26.8.9
            "\\sum_{n, k \\hiderel{=} 0}^{\\infty} \\Stirlingnumbers@{n}{k} \\frac{x^n}{n!}y^k = (1+x)^y",
            "sum(sum(Stirling1(n, k)((x)^(n))/(factorial(n))(y)^(k), k=0..infinity), n=0..infinity) = (1+x)^(y)",
            "Sum[Sum[StirlingS1[n, k]Divide[(x)^(n),n!](y)^(k), {k, 0, Infinity}], {n, 0, Infinity}]"
    ),
    DLMF_RANGE_VAR_LONG( //25.9.1 all
            "\\sum_{1 \\leq n \\leq x} \\frac{1}{n^s}",
            "sum((1)/((n)^(s)), n=1..x)",
            "Sum[Divide[1,(n)^(s)], {n, 1, x}]"
    ),
    DLMF_RANGE_VAR( //25.9.1 part 2
            "\\sum_{1 \\leq n \\leq y} \\frac{1}{n^{1-s}}",
            "sum((1)/((n)^(1 - s)), n=1..y)",
            "Sum[Divide[1,(n)^(1 - s)], {n, 1, y}]"
    );

    private String tex, maple, mathematica;

    Sums( String tex, String maple, String mathematica ) {
        this.tex = tex;
        this.maple = maple;
        this.mathematica = mathematica;
    }

    public String getTeX() {
        return tex;
    }

    public String getMaple() {
        return maple;
    }

    public String getMathematica() {
        return mathematica;
    }
}
