package gov.nist.drmf.interpreter.maple.cases;

import gov.nist.drmf.interpreter.common.meta.DLMF;

/**
 * @author Andre Greiner-Petter
 */
public enum TranslationTestCases implements BackwardMapleTestCase {
    SIMPLE(
            "n + 2",
            "n + 2"
    ),
    SUMS_1(
            "\\Sum{n}{0}{m+1}@{n}",
            "sum(n, n = 0..m + 1)"
    ),
    SUMS_2(
            "\\Sum{n}{m-1}{m+1}@{n}",
            "sum(n, n = m - 1..m + 1)"
    ),
    SUMS_3(
            "\\cpi + \\Sum{n}{0}{10}@{n}",
            "Pi + sum(n, n = 0..10)"
    ),
    AIRY(
            "\\AiryAi@{x}",
            "AiryAi(x)"
    ),
    @DLMF("6.2.14")
    SIN_INT(
            "\\Lim{x}{\\infty}@{\\sinint@{x}} = \\frac{1}{2} \\idot \\cpi",
            "limit(Si(x), x = infinity) = (1)/(2)*Pi"
    ),
    @DLMF("6.2.14")
    COS_INT(
            "\\Lim{x}{\\infty}@{\\cosint@{x}} = 0",
            "limit(Ci(x), x = infinity) = 0"
    );

    private String tex, maple;

    TranslationTestCases(String tex, String maple ) {
        this.tex = tex;
        this.maple = maple;
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
}
