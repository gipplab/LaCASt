package gov.nist.drmf.interpreter.cas.translation.components.cases;

import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.tests.ExceptionalTestCase;

/**
 * @author Andre Greiner-Petter
 */
public enum InvalidTests implements ExceptionalTestCase {
    AIRY_INVALID_PRIME(
            "\\AiryAi@'{x}"
    ),
    MODIFIED_BESSEL_K_INVALID_LAGRANGE(
            "\\modBesselKimag{\\nu}^{(5^3}@{x}"
    ),
    HYPER_F_INVALID_PRIME(
            "\\hyperF@'@@{a}{b}{c}{z}"
    ),
    HYPER_F_INVALID_LAGRANGE_AND_PRIME_MIX(
            "\\hyperF^{(3)}'@@@{a}{b}{c}{z}"
    ),
    HYPER_F_INVALID_LAGRANGE_AND_PRIME_MIX_2(
            "\\hyperF'^{(3)}@@@{a}{b}{c}{z}"
    ),
    FERRERS_P_INVALID_PRIME_ORDER(
            "\\FerrersP'[\\mu]{\\nu}@{x}"
    ),
    FERRERS_P_INVALID_PRIME_ORDER_2(
            "\\FerrersP[\\mu]'{\\nu}@{x}"
    ),
    FAKE_MACRO(
            "\\notmacro@{x}"
    ),
    FAKE_MACRO_PRIME(
            "\\notmacro'@{x}"
    ),
    FAKE_MACRO_LAGRANGE(
            "\\notmacro^{(3)}@{x}"
//    ),
//    POCHHAMMER_ILLEGAL_PRIME(
//            "\\pochhammer'{a}{n}"
    );

    private String tex;

    InvalidTests(String tex) {
        this.tex = tex;
    }

    @Override
    public String getTitle() {
        return name();
    }

    @Override
    public String getTeX() {
        return tex;
    }

    @Override
    public Class getException() {
        return TranslationException.class;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
