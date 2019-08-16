package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SumProductTranslatorTest {

    private static final String stuffBeforeMathematica = "\n" +
            "This is a program that translated given LaTeX\n" +
            "code into a specified computer algebra system\n" +
            "representation.\n" +
            "\n" +
            "You set the following CAS: Mathematica\n" +
            "\n" +
            "You want to translate the following expression: " + "\n" +
            "\n" +
            "Set global variable to given CAS.\n" +
            "Set up translation...\n" +
            "Initialize translation...\n" +
            "Start translation...\n" +
            "\n" +
            "Finished conversion to Mathematica:\n";

    private static final String stuffBeforeMaple = "\n" +
            "This is a program that translated given LaTeX\n" +
            "code into a specified computer algebra system\n" +
            "representation.\n" +
            "\n" +
            "You set the following CAS: Maple\n" +
            "\n" +
            "You want to translate the following expression: " + "\n" +
            "\n" +
            "Set global variable to given CAS.\n" +
            "Set up translation...\n" +
            "Initialize translation...\n" +
            "Start translation...\n" +
            "\n" +
            "Finished conversion to Maple:\n";

    private static final String[] sums= {
            "\\sum_{i=0}^\\infty i^2\\log{i}^3+i(2+3)",
            "\\sum^{200}_{k=-3}3i+k+i^2",
            "\\sum_{x, y = -\\infty}^{\\infty}\\sin{2^x}+\\cos{2^y}+23",
            "\\sum_{j=0}^{r}\\tan{x^3}^2\\sin{j}+\\frac{2^j}{x-3}",
            "\\sum_{x=-\\infty}^{\\infty}x^2(x+2)(y^3-3)-2x+y-2",
            "\\sum_{n=1}^{\\infty}\\frac{(-1)^{n}2^{2n-1}B_{2n}}{n(2n)!}z^{2n}",
            "\\sum^{50}_{r=0}r\\cos{\\Theta}r(3r^2-3)/23x+3q",
            "\\sum_{x=0}^{\\infty}x^3(3x+2y)^{25x^2}(x+2)x^2(x+3)+2x(x+2)^2",
            //6.6.5
            "\\sum_{n=0}^\\infty \\frac{\\opminus^n z^{2n+1}}{(2n+1)!(2n+1)}",
            //29.6.36
            "\\sum_{p \\hiderel{=} 0}^{\\infty} (2p+1) B_{2p+1}",
            //22.12.2 all
            "\\sum_{n=-\\infty}^{\\infty} \\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}} = \\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty} \\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            //22.12.2 part 1
            "\\sum_{n=-\\infty}^{\\infty}\\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}",
            //22.12.2 part 2
            "\\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty}\\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau}\\right)",
            //10.23.4 all
            "\\sum_{k \\hiderel{=} 0}^{2n} \\opminus^k \\BesselJ{k}@{z} \\BesselJ{2n-k}@{z} + 2 \\sum_{k \\hiderel{=} 1}^\\infty \\BesselJ{k}@{z} \\BesselJ{2n+k}@{z} = 0",
            //10.23.4 part 1
            "\\sum_{k \\hiderel{=} 0}^{2n} \\opminus^k \\BesselJ{k}@{z} \\BesselJ{2n-k}@{z}",
            //10.23.4 part 2
            "\\sum_{k \\hiderel{=} 1}^\\infty \\BesselJ{k}@{z} \\BesselJ{2n+k}@{z}",
            //10.23.17 all
            "\\sum_{k=0}^{n-1} \\frac{(\\tfrac{1}{2} z)^k \\BesselJ{k}@{z}}{k! (n-k)} + \\frac{2}{\\pi} \\left( \\ln@{\\tfrac{1}{2} z} - \\digamma@{n+1} \\right)\\BesselJ{n}@{z}-\\frac{2}{\\pi}\\sum_{k=1}^\\infty \\opminus^k \\frac{(n+2k) \\BesselJ{n+2k}@{z}}{k (n+k)}",
            //10.23.17 part 1
            "\\sum_{k=0}^{n-1} \\frac{(\\tfrac{1}{2} z)^k \\BesselJ{k}@{z}}{k! (n-k)} + \\frac{2}{\\pi} \\left( \\ln@{\\tfrac{1}{2} z} - \\digamma@{n+1} \\right)\\BesselJ{n}@{z}",
            //10.23.17 part 2
            "\\sum_{k=1}^\\infty \\opminus^k \\frac{(n+2k) \\BesselJ{n+2k}@{z}}{k (n+k)}",
            //22.12.5 part 1
            "\\sum_{n=-\\infty}^{\\infty}\\frac{\\pi}{\\sin@{\\pi (t+\\frac{1}{2}-(n+\\frac{1}{2}) \\tau)}}",
            //22.12.5 part 2
            "\\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty} \\frac{(-1)^m}{t + \\frac{1}{2} - m - (n+\\frac{1}{2}) \\tau}\\right)",
            //35.7.3
            "\\sum_{k=0}^\\infty\\frac{\\Pochhammersym{a}{k} \\Pochhammersym{c-a}{k}\\Pochhammersym{b}{k} \\Pochhammersym{c-b}{k}}{k! \\, \\Pochhammersym{c}{2k} \\Pochhammersym{c-\\tfrac{1}{2}}{k}}(t_1 t_2)^k",
            //25.16.11
            "\\sum_{n=1}^\\infty \\frac{1}{n^s} \\sum_{m=1}^n \\frac{1}{m^z}",
            //18.2.6
            "\\sum_{x \\in X} x \\left( p_n(x) \\right)^2 w_x",
            //16.11.2
            "\\sum_{m=1}^p \\sum_{k=0}^\\infty \\frac{\\opminus^k}{k!} \\EulerGamma@{a_m + k} \\left(\\frac{\\prod_{\\ell=1}^p \\EulerGamma@{a_\\ell - a_m - k}} {\\prod_{\\ell=1}^q \\EulerGamma@{b_\\ell - a_m - k}}\\right) z^{-a_m - k}",
            //17.2.49
            "\\sum_{n \\hiderel{=} 1}^\\infty \\frac{q^{n^2}}{(1 - q) (1 - q^2) \\cdots (1 - q^n)} = \\prod_{n=0}^\\infty \\frac{1}{(1 - q^{5n+1}) (1 - q^{5n+4})}",
            //8.15.1
            "\\sum_{k=0}^\\infty \\incgamma@{a+k}{x} \\frac{(1-\\lambda)^k}{k!}",
            //26.18.3
            "\\sum_{t \\hiderel{=} 1}^n \\opminus^t r_t(B) (n-t)!",
            //20.6.8
            "\\sum_{n=-\\infty}^{\\infty} \\sum_{m=-\\infty}^{\\infty} (m - \\tfrac{1}{2} + (n-\\tfrac{1}{2}) \\tau)^{-2j}",
            //20.11.3
            "\\sum_{n=-\\infty}^\\infty a^{n(n+1)/2} b^{n(n-1)/2}",
            //7.6.4
            "\\sum_{n=0}^\\infty \\frac{\\opminus^n (\\frac{1}{2} \\pi)^{2n}}{(2n)! (4n+1)} z^{4n+1}",
            //24.4.24 with removed \choose
            "\\sum_{k=1}^n \\sum_{j=0}^{k-1} \\opminus^j\\left( \\sum_{r=1}^{m-1} \\frac{e^{2\\pi i (k-j) r/m}}{(1 - e^{2\\pi ir/m})^n} \\right) (j+m*x)^{n-1}",
            //31.15.1
            "\\sum_{p=1}^{\\infty}{\\deriv[2]{w}{z} + \\left( \\sum_{j=1}^N \\frac{\\gamma_j}{z - a_j} \\right) \\deriv{w}{z} + \\frac{\\Phi(z)}{\\prod_{j=1}^N (z - a_j)} w}",
            //26.8.9
            "\\sum_{n, k \\hiderel{=} 0}^{\\infty} \\Stirlingnumbers@{n}{k} \\frac{x^n}{n!}y^k = (1+x)^y",

    };

    private static final String[] prods = {
            "\\prod_{i=0}^{\\infty}k^3",
            "\\prod_{x \\in P}x^2+x^3-3",
            "\\prod_{i=0}^{k}i^2+\\prod_{j=0}^{k}i^3-3j+\\prod_{l=0}^{k}j+2+\\sin{l}",
            "\\prod_{i=0}^{10}i^2\\prod_{i=2}^{12}k",
            "\\prod_{i=0}^{10}i^2\\prod_{j=2}^{12}i",
            "\\prod_{i=0}^{10}\\sum_{j=2}^{12}i+\\sin{j^2}",
            "\\prod_{i=0}^{10}\\prod_{j=2}^{12}j^2+i",
            "\\prod_{i=0}^{10}\\prod_{j=2}^{12}\\sum_{k=-\\infty}^\\infty {j+k}^2-3j+2+\\log{i}^2-5",
            "\\prod_{i=0}^{\\infty}\\sin{\\prod_{k=0}^{r}k^3-2k}\\sum_{i=0}^{r}12i^2+k",
            "\\prod_{x=0}^{\\infty}x\\sin{x^2}\\cos{t}+2sin{4}+3",
            //26.12.4
            "\\prod_{h=1}^r \\prod_{j=1}^s \\frac{h+j+t-1}{h+j-1}",
            //5.14.4
            "\\prod_{k=1}^m \\frac{a+(n-k)c}{a+b+(2n-k-1)c} \\prod_{k=1}^n \\frac{\\EulerGamma@{a+(n-k)c} \\EulerGamma@{b+(n-k)c} \\EulerGamma@{1+kc}} {\\EulerGamma@{a+b+(2n-k-1)c}}",
            //20.5.1
            "\\prod_{n=1}^{\\infty} {\\left( 1 - q^{2n} \\right)} {\\left( 1 - 2 q^{2n} \\cos@{2z} + q^{4n} \\right)}",
            //4.22.2
            "\\prod_{n=1}^\\infty \\left( 1 - \\frac{4z^2}{(2n - 1)^2 \\pi^2} \\right)",
             //20.4.3
            "\\prod_{n=1}^{\\infty} \\left( 1 - q^{2n} \\right) \\left( 1 + q^{2n} \\right)^2",
            //27.4.1
            "\\prod_p \\left( 1 + \\sum_{r=1}^\\infty f(p^r) \\right)",
            //5.14.5
            "\\prod_{k=1}^m (a + (n-k)c) \\frac{\\prod_{k=1}^n \\EulerGamma@{a+(n-k)c} \\EulerGamma@{1+kc}} {(\\EulerGamma@{1+c})^n}",
            //17.2.49 part 2
            "\\prod_{n=0}^\\infty \\frac{1}{(1 - q^{5n+1}) (1 - q^{5n+4})}",
            //23.8.7
            "\\prod_{n=1}^\\infty \\frac{\\sin@{\\pi (2n \\omega_3 + z) / (2 \\omega_1)} \\sin@{\\pi (2n \\omega_3 - z) / (2 \\omega_1)}} {\\sin^2@{\\pi n \\omega_3 / \\omega_1}}",
            //3.4.3
            "\\prod_{k = n_0}^{n_1}(t-k)+f^{(n+2)}(\\xi_1)\\prod_{k = n_0}^{n_1}(t-k)",

    };

    private static final String[] lims = {
            //4.31.1
            "\\lim_{z \\to 0} \\frac{\\sinh@@{z}}{z}",
            //4.31.3
            "\\lim_{z \\to 0} \\frac{\\cosh@@{z} - 1}{z^2}",
            //4.4.13
            "\\lim_{x \\to \\infty} x^{-a} \\ln@@{x}",
            //4.4.17
            "\\lim_{n \\to \\infty} \\left( 1 + \\frac{z}{n} \\right)^n",
            //22.12.4 all
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t - (n+\\frac{1}{2}) \\tau)}} = \\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            //22.12.4 part 1
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}",
            //22.12.4 part 2
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            //22.12.13 all
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t-n\\tau)}} = \\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - n \\tau} \\right)",
            //22.12.13 part 1
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t-n\\tau)}}",
            //22.12.13 part 2
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - n \\tau} \\right)",
            //20.5.15
            "\\lim_{N \\to \\infty} \\prod_{n=-N}^{N} \\lim_{M \\to \\infty} \\prod_{m=1-M}^{M} \\left( 1 + \\frac{z}{(m - \\tfrac{1}{2} + n \\tau) \\pi} \\right)",
            //20.5.17
            "\\lim_{N \\to \\infty} \\prod_{n=1-N}^{N} \\lim_{M \\to \\infty} \\prod_{m=-M}^{M} \\left( 1 + \\frac{z}{(m + (n-\\tfrac{1}{2}) \\tau) \\pi} \\right)",

    };

    private static final String[] translatedMapleLims = {
            "limit((sinh(z))/(z), z = 0)",
            "limit((cosh(z)- 1)/((z)^(2)), z = 0)",
            "limit((x)^(- a)*ln(x), x = infinity)",
            "limit((1 +(z)/(n))^(n), n = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t -(n +(1)/(2))tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t -(n +(1)/(2))tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m -(n +(1)/(2))*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t - n*tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t - n*tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m - n*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "limit(product(limit(product((1 +(z)/((m -(1)/(2)+ n*tau)*pi)), m = 1 - M..M), M = infinity), n = - N..N), N = infinity)",
            "limit(product(limit(product((1 +(z)/((m +(n -(1)/(2))tau)*pi)), m = - M..M), M = infinity), n = 1 - N..N), N = infinity)",
    };

    private static final String[] translatedMathematicaLims = {
            "Limit[Divide[Sinh[z],z], z -> 0]",
            "Limit[Divide[Cosh[z] - 1,(z)^(2)], z -> 0]",
            "Limit[(x)^(- a) Log[x], x -> Infinity]",
            "Limit[(1 +Divide[z,n])^(n), n -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) (Limit[Sum[Divide[1,t - m -(n +Divide[1,2]) \\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t - n \\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t - n \\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) (Limit[Sum[Divide[1,t - m - n \\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]",
            "Limit[Product[Limit[Product[(1 +Divide[z,(m -Divide[1,2]+ n \\[Tau]) \\[Pi]]), {m, 1-M, M}], M -> Infinity], {n, -N, N}], N -> Infinity]",
            "Limit[Product[Limit[Product[(1 +Divide[z,(m +(n -Divide[1,2])\\[Tau]) \\[Pi]]), {m, -M, M}], M -> Infinity], {n, 1-N, N}], N -> Infinity]",
    };
    private static final String[] translatedMapleSums = {
            "sum((i)^(2)*(log(i))^(3)+i(2 + 3), i = 0..infinity)",
            "sum(3i+k, k = - 3..200)",
            "sum(sum(sin((2)^(x))+cos((2)^(y)), y=-infinity..infinity), x=-infinity..infinity)",
            "sum((tan((x)^(3)))^(2)*sin(j)+((2)^(j))/(x - 3), j = 0..r)",
            "sum((x)^(2)*(x + 2)((y)^(3) - 3)-2x, x = - infinity..infinity)",
            "sum(((- 1)^(n)* (2)^(2*n - 1)* B[2*n])/(n*factorial((2*n)))(z)^(2*n), n = 1..infinity)",
            "sum(rcos(Theta)r(3*(r)^(2) - 3)/23x, r = 0..50)",
            "sum((x)^(3)*(3*x + 2*y)^(25*(x)^(2))*(x + 2)(x)^(2)*(x + 3)+2x(x + 2)^(2), x = 0..infinity)",
            "sum(((-1)^(n)* (z)^(2*n + 1))/(factorial((2*n + 1))*(2*n + 1)), n = 0..infinity)",
            "sum((2*p + 1)B[2*p + 1], p = 0..infinity)",
            "sum((pi)/(sin(pi*(t -(n +(1)/(2))tau))), n = - infinity..infinity)",
            "sum((pi)/(sin(pi*(t -(n +(1)/(2))tau))), n = - infinity..infinity)",
            "sum((sum(((- 1)^(m))/(t - m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "sum((-1)^(k)*BesselJ(k, z)BesselJ(2*n - k, z), k = 0..2*n)",
            "sum((-1)^(k)*BesselJ(k, z)BesselJ(2*n - k, z), k = 0..2*n)",
            "sum(BesselJ(k, z)BesselJ(2*n + k, z), k = 1..infinity)",
            "sum((((1)/(2)*z)^(k)* BesselJ(k, z))/(factorial(k)*(n - k)), k = 0..n - 1)",
            "sum((((1)/(2)*z)^(k)* BesselJ(k, z))/(factorial(k)*(n - k)), k = 0..n - 1)",
            "sum((-1)^(k)*((n + 2*k)*BesselJ(n + 2*k, z))/(k*(n + k)), k = 1..infinity)",
            "sum((pi)/(sin(pi*(t +(1)/(2)-(n +(1)/(2))tau))), n = - infinity..infinity)",
            "sum((sum(((- 1)^(m))/(t +(1)/(2)- m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "sum((pochhammer(a, k)*pochhammer(c - a, k)*pochhammer(b, k)*pochhammer(c - b, k))/(factorial(k)*pochhammer(c, 2*k)*pochhammer(c -(1)/(2), k))(t[1] t[2])^(k), k = 0..infinity)",
            "sum((1)/((n)^(s))sum((1)/((m)^(z)), m = 1..n), n = 1..infinity)",
            "sum(x(p[n](x))^(2)*w[x], x in X)",
            "sum(sum(((-1)^(k))/(factorial(k))GAMMA(a[m] + k)((product(GAMMA(a[ell] - a[m] - k), ell = 1..p))/(product(GAMMA(b[ell] - a[m] - k), ell = 1..q)))(z)^(- a[m] - k), k = 0..infinity), m = 1..p)",
            "sum(((q)^((n)^(2)))/((1 - q)*(1 - (q)^(2))..(1 - (q)^(n))), n = 1..infinity)",
            "sum(GAMMA(a + k)-GAMMA(a + k, x)((1 - lambda)^(k))/(factorial(k)), k = 0..infinity)",
            "sum((-1)^(t)*r[t](B)factorial((n - t)), t = 1..n)",
            "sum(sum((m -(1)/(2)+(n -(1)/(2))tau)^(- 2*j), m = - infinity..infinity), n = - infinity..infinity)",
            "sum((a)^(n*(n + 1)/ 2)*(b)^(n*(n - 1)/ 2), n = - infinity..infinity)",
            "sum(((-1)^(n)*((1)/(2)*pi)^(2*n))/(factorial((2*n))*(4*n + 1))(z)^(4*n + 1), n = 0..infinity)",
            "sum(sum((-1)^(j)*(sum(((e)^(2*pi*i*(k - j)*r/ m))/((1 - (e)^(2*pi*i*r/ m))^(n)), r = 1..m - 1))(j + m * x)^(n - 1), j = 0..k - 1), k = 1..n)",
            "sum(diff(w, [z$(2)])+(sum((gamma[j])/(z - a[j]), j = 1..N))*diff(w, z)+(Phi*(z))/(product((z - a[j]), j = 1..N))*w, p = 1..infinity)",
            "sum(sum(Stirling1(n, k)((x)^(n))/(factorial(n))(y)^(k), k=0..infinity), n=0..infinity)",

    };

    private static final String[] translatedMapleProds = {
            "product((k)^(3), i = 0..infinity)",
            "product((x)^(2)+(x)^(3), x in P)",
            "product((i)^(2)+product((i)^(3)-3j+product(j+2+sin(l), l = 0..k), j = 0..k), i = 0..k)",
            "product((i)^(2)*, i = 0..10)",
            "product((i)^(2)*product(i, j = 2..12), i = 0..10)",
            "product(sum(i+sin((j)^(2)), j = 2..12), i = 0..10)",
            "product(product((j)^(2), j = 2..12)+i, i = 0..10)",
            "product(product(sum((j + k)^(2), k = - infinity..infinity)-3j, j = 2..12)+2+(log(i))^(2), i = 0..10)",
            "product(sin(product((k)^(3)-2k, k = 0..r)), i = 0..infinity)",
            "product(xsin((x)^(2))cos(t), x = 0..infinity)",
            "product(product((h + j + t - 1)/(h + j - 1), j = 1..s), h = 1..r)",
            "product((a +(n - k)*c)/(a + b +(2*n - k - 1)*c), k = 1..m)",
            "product((1 - (q)^(2*n))(1 - 2*(q)^(2*n)* cos(2*z) + (q)^(4*n)), n = 1..infinity)",
            "product((1 -(4*(z)^(2))/((2*n - 1)^(2)* (pi)^(2))), n = 1..infinity)",
            "product((1 - (q)^(2*n))(1 + (q)^(2*n))^(2), n = 1..infinity)",
            "product((1 + sum(f((p)^(r)), r = 1..infinity)), p)",
            "product((a +(n - k)c), k = 1..m)",
            "product((1)/((1 - (q)^(5*n + 1))*(1 - (q)^(5*n + 4))), n = 0..infinity)",
            "product((sin(pi*(2*n*omega[3] + z)/(2*omega[1]))*sin(pi*(2*n*omega[3] - z)/(2*omega[1])))/((sin(pi*n*omega[3]/ omega[1]))^(2)), n = 1..infinity)",
            "product((t - k), k = n[0]..n[1])",

    };

    private static final String[] translatedMathematicaSums = {
            "Sum[(i)^(2) (Log[i])^(3)+i(2 + 3), {i, 0, Infinity}]",
            "Sum[3i+k, {k, -3, 200}]",
            "Sum[Sum[Sin[(2)^(x)]+Cos[(2)^(y)], {y, -Infinity, Infinity}], {x, -Infinity, Infinity}]",
            "Sum[(Tan[(x)^(3)])^(2) Sin[j]+Divide[(2)^(j),x - 3], {j, 0, r}]",
            "Sum[(x)^(2) (x + 2)((y)^(3) - 3)-2x, {x, -Infinity, Infinity}]",
            "Sum[Divide[(- 1)^(n)  (2)^(2 n - 1)  Subscript[B, 2 n],n (2 n)!](z)^(2 n), {n, 1, Infinity}]",
            "Sum[rCos[\\[CapitalTheta]]r(3 (r)^(2) - 3)/23x, {r, 0, 50}]",
            "Sum[(x)^(3) (3 x + 2 y)^(25 (x)^(2)) (x + 2)(x)^(2) (x + 3)+2x(x + 2)^(2), {x, 0, Infinity}]",
            "Sum[Divide[(-1)^(n)  (z)^(2 n + 1),(2 n + 1)!(2 n + 1)], {n, 0, Infinity}]",
            "Sum[(2 p + 1)Subscript[B, 2 p + 1], {p, 0, Infinity}]",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]",
            "Sum[(Sum[Divide[(- 1)^(m),t - m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]",
            "Sum[(-1)^(k) BesselJ[k, z]BesselJ[2 n - k, z], {k, 0, 2 n}]",
            "Sum[(-1)^(k) BesselJ[k, z]BesselJ[2 n - k, z], {k, 0, 2 n}]",
            "Sum[BesselJ[k, z]BesselJ[2 n + k, z], {k, 1, Infinity}]",
            "Sum[Divide[(Divide[1,2] z)^(k)  BesselJ[k, z],k!(n - k)], {k, 0, n - 1}]",
            "Sum[Divide[(Divide[1,2] z)^(k)  BesselJ[k, z],k!(n - k)], {k, 0, n - 1}]",
            "Sum[(-1)^(k) Divide[(n + 2 k) BesselJ[n + 2 k, z],k (n + k)], {k, 1, Infinity}]",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t +Divide[1,2]-(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]",
            "Sum[(Sum[Divide[(- 1)^(m),t +Divide[1,2]- m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]",
            "Sum[Divide[Pochhammer[a, k] Pochhammer[c - a, k] Pochhammer[b, k] Pochhammer[c - b, k],k! Pochhammer[c, 2 k] Pochhammer[c -Divide[1,2], k]](Subscript[t, 1] Subscript[t, 2])^(k), {k, 0, Infinity}]",
            "Sum[Divide[1,(n)^(s)]Sum[Divide[1,(m)^(z)], {m, 1, n}], {n, 1, Infinity}]",
            "Sum[x(Subscript[p, n](x))^(2) Subscript[w, x], {x, X}]",
            "Sum[Sum[Divide[(-1)^(k),k!]Gamma[Subscript[a, m] + k](Divide[Product[Gamma[Subscript[a, \\[ScriptL]] - Subscript[a, m] - k], {\\[ScriptL], 1, p}],Product[Gamma[Subscript[b, \\[ScriptL]] - Subscript[a, m] - k], {\\[ScriptL], 1, q}]])(z)^(- Subscript[a, m] - k), {k, 0, Infinity}], {m, 1, p}]",
            "Sum[Divide[(q)^((n)^(2)),(1 - q) (1 - (q)^(2)) ... (1 - (q)^(n))], {n, 1, Infinity}]",
            "Sum[Gamma[a + k, 0, x]Divide[(1 - \\[Lambda])^(k),k!], {k, 0, Infinity}]",
            "Sum[(-1)^(t) Subscript[r, t](B)(n - t)!, {t, 1, n}]",
            "Sum[Sum[(m -Divide[1,2]+(n -Divide[1,2])\\[Tau])^(- 2 j), {m, -Infinity, Infinity}], {n, -Infinity, Infinity}]",
            "Sum[(a)^(n (n + 1)/ 2) (b)^(n (n - 1)/ 2), {n, -Infinity, Infinity}]",
            "Sum[Divide[(-1)^(n)  (Divide[1,2] \\[Pi])^(2 n),(2 n)!(4 n + 1)](z)^(4 n + 1), {n, 0, Infinity}]",
            "Sum[Sum[(-1)^(j) (Sum[Divide[(e)^(2 \\[Pi] i (k - j) r/ m),(1 - (e)^(2 \\[Pi] i r/ m))^(n)], {r, 1, m - 1}])(j + m   x)^(n - 1), {j, 0, k - 1}], {k, 1, n}]",
            "Sum[D[w, {z, 2}] +(Sum[Divide[Subscript[\\[Gamma], j],z - Subscript[a, j]], {j, 1, N}]) D[w, z] +Divide[\\[CapitalPhi] (z),Product[(z - Subscript[a, j]), {j, 1, N}]] w, {p, 1, Infinity}]",
            "Sum[Sum[StirlingS1[n, k]Divide[(x)^(n),n!](y)^(k), {k, 0, Infinity}], {n, 0, Infinity}]",
    };

    private static final String[] translatedMathematicaProds = {
            "Product[(k)^(3), {i, 0, Infinity}]",
            "Product[(x)^(2)+(x)^(3), {x, P}]",
            "Product[(i)^(2)+Product[(i)^(3)-3j+Product[j+2+Sin[l], {l, 0, k}], {j, 0, k}], {i, 0, k}]",
            "Product[(i)^(2) , {i, 0, 10}]",
            "Product[(i)^(2) Product[i, {j, 2, 12}], {i, 0, 10}]",
            "Product[Sum[i+Sin[(j)^(2)], {j, 2, 12}], {i, 0, 10}]",
            "Product[Product[(j)^(2), {j, 2, 12}]+i, {i, 0, 10}]",
            "Product[Product[Sum[(j + k)^(2), {k, -Infinity, Infinity}]-3j, {j, 2, 12}]+2+(Log[i])^(2), {i, 0, 10}]",
            "Product[Sin[Product[(k)^(3)-2k, {k, 0, r}]], {i, 0, Infinity}]",
            "Product[xSin[(x)^(2)]Cos[t], {x, 0, Infinity}]",
            "Product[Product[Divide[h + j + t - 1,h + j - 1], {j, 1, s}], {h, 1, r}]",
            "Product[Divide[a +(n - k) c,a + b +(2 n - k - 1) c], {k, 1, m}]",
            "Product[(1 - (q)^(2 n))(1 - 2 (q)^(2 n)  Cos[2 z] + (q)^(4 n)), {n, 1, Infinity}]",
            "Product[(1 -Divide[4 (z)^(2),(2 n - 1)^(2)  (\\[Pi])^(2)]), {n, 1, Infinity}]",
            "Product[(1 - (q)^(2 n))(1 + (q)^(2 n))^(2), {n, 1, Infinity}]",
            "Product[(1 + Sum[f((p)^(r)), {r, 1, Infinity}]), {p, p}]",
            "Product[(a +(n - k)c), {k, 1, m}]",
            "Product[Divide[1,(1 - (q)^(5 n + 1)) (1 - (q)^(5 n + 4))], {n, 0, Infinity}]",
            "Product[Divide[Sin[\\[Pi] (2 n Subscript[\\[Omega], 3] + z)/(2 Subscript[\\[Omega], 1])] Sin[\\[Pi] (2 n Subscript[\\[Omega], 3] - z)/(2 Subscript[\\[Omega], 1])],(Sin[\\[Pi] n Subscript[\\[Omega], 3]/ Subscript[\\[Omega], 1]])^(2)], {n, 1, Infinity}]",
            "Product[(t - k), {k, Subscript[n, 0], Subscript[n, 1]}]",

    };

    private static SemanticLatexTranslator slt;
    private static PomParser parser;
    private static SumProductTranslator spt;

    @TestFactory
    Stream<DynamicTest>  sumMathematicaTest() {
        List<String> expressions = Arrays.asList(sums);
        List<String> output = Arrays.asList(translatedMathematicaSums);
        return test(expressions, output);
    }

    @TestFactory
    Stream<DynamicTest>  prodMathematicaTest() {
        List<String> expressions = Arrays.asList(prods);
        List<String> output = Arrays.asList(translatedMathematicaProds);
        return test(expressions, output);
    }
/*
messed with onlyLower
    @TestFactory
    Stream<DynamicTest> limMathematicaTest(){
        List<String> expressions = Arrays.asList(lims);
        List<String> output = Arrays.asList(translatedMathematicaLims);
        return test(expressions, output);
    }


 */
    @TestFactory
    Stream<DynamicTest>  sumMapleTest() {
        mapleSetUp();
        List<String> expressions = Arrays.asList(sums);
        List<String> output = Arrays.asList(translatedMapleSums);
        return test(expressions, output);
    }

    @TestFactory
    Stream<DynamicTest>  prodMapleTest() {
        mapleSetUp();
        List<String> expressions = Arrays.asList(prods);
        List<String> output = Arrays.asList(translatedMapleProds);
        return test(expressions, output);
    }
/*
    @TestFactory
    Stream<DynamicTest> limMapleTest(){
        mapleSetUp();
        List<String> expressions = Arrays.asList(lims);
        List<String> output = Arrays.asList(translatedMapleLims);
        return test(expressions, output);
    }

 */

    @BeforeEach
    private void mathematicaSetUp(){
        GlobalConstants.CAS_KEY = Keys.KEY_MATHEMATICA;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MATHEMATICA);
        try {
            slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA);
        parser.addLexicons(MacrosLexicon.getDLMFMacroLexicon());
        spt = new SumProductTranslator();
    }

    private void mapleSetUp(){
        GlobalConstants.CAS_KEY = Keys.KEY_MAPLE;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MAPLE);
        try {
            slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private Stream<DynamicTest> test(List<String> expressions, List<String> output){
        return expressions
                .stream()
                .map(
                        exp -> DynamicTest.dynamicTest("Expression: " + exp, () -> {
                            int index = expressions.indexOf(exp);
                            PomTaggedExpression ex = parser.parse(TeXPreProcessor.preProcessingTeX(expressions.get(index)));

                            List<PomTaggedExpression> components = ex.getComponents();
                            PomTaggedExpression first = components.remove(0);
                            spt.translate(first, components);
                            assertEquals(output.get(index), spt.getTranslation());
                        }));
    }

    @AfterEach
    private void tearDown(){
        slt = null;
        parser = null;
        spt = null;
    }

//    @Test
//    public void mathematicaTest(){
//        String more = "";
//        for(int i = 0; i < expression.length; i++){
//            String[] args = {"-CAS=Mathematica", "-Expression=" + expression[i]};
//            SemanticToCASInterpreter.main(args);
//            more += stuffBeforeMathematica.substring(0, stuffBeforeMathematica.indexOf("n: ") + 3) + expression[i]
//                    + stuffBeforeMathematica.substring(stuffBeforeMathematica.indexOf("n: ") + 3);
//            more += translatedMathematica[i] + "\n\n";
//            assertEquals(more, result.toString());
//        }
//    }
//
//    @Test
//    public void mapleTest(){
//        String more = "";
//        for(int i = 0; i < expression.length; i++){
//            String[] args = {"-CAS=Maple", "-Expression=" + expression[i]};
//            SemanticToCASInterpreter.main(args);
//            more += stuffBeforeMaple.substring(0, stuffBeforeMaple.indexOf("n: ") + 3) + expression[i]
//                    + stuffBeforeMaple.substring(stuffBeforeMaple.indexOf("n: ") + 3);
//            more += translatedMaple[i] + "\n\n";
//            assertEquals(more, result.toString());
//        }
//    }

}
