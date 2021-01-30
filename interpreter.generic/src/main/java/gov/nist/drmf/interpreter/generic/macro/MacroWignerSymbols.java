package gov.nist.drmf.interpreter.generic.macro;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MacroWignerSymbols {

    private final MacroDistributionAnalyzer macroDistributions;

    public MacroWignerSymbols() {
        this.macroDistributions = MacroDistributionAnalyzer.getStandardInstance();
    }

    public MacroBean getWignerSymbol(Type type) {
        MacroBean wignerBean = new MacroBean(type.macro);
        wignerBean.setNumberOfParameters(0);
        wignerBean.setNumberOfOptionalParameters(0);
        wignerBean.setNumberOfArguments(type.args);

        MacroCounter counter = macroDistributions.getMacroCounter("\\" + type.macro);
        double score = counter.getScore(false, 0);

        MacroGenericSemanticEntry semanticEntry = new MacroGenericSemanticEntry(
            type.tex, type.semantic, score
        );
        wignerBean.setTex(List.of(semanticEntry));

        MacroMetaBean metaBean = new MacroMetaBean();
        if ( type.equals( Type.THREE ) ) {
            metaBean.setDescription("Wigner 3 j symbol");
            metaBean.setMeaning("Wigner-3-j-symbol");
        }
        else {
            metaBean.setDescription("Wigner "+ type.args +" j symbol");
            metaBean.setMeaning("Wigner-"+type.args+"-j-symbol");
        }
        metaBean.setOpenMathID("none");

        MacroStandardArgumentsBean standards = new MacroStandardArgumentsBean();
        standards.setStandardVariables(type.standardVars);
        metaBean.setStandardArguments(standards);

        wignerBean.setMetaInformation(metaBean);
        return wignerBean;
    }

    public enum Type{
        THREE("Wignerthreejsym", 6,
                "\\begin{pmatrix} var1 & var2 & var3 \\\\ var4 & var5 & var6 \\end{pmatrix}",
                "\\Wignerthreejsym{var1}{var2}{var3}{var4}{var5}{var6}",
                List.of("j_1", "j_2", "j_3", "m_1", "m_2", "m_3")
        ),
        SIX("Wignersixjsym", 6,
                "\\begin{Bmatrix} var1 & var2 & var3 \\\\ var4 & var5 & var6 \\end{Bmatrix}",
                "\\Wignersixjsym{var1}{var2}{var3}{var4}{var5}{var6}",
                List.of("j_1", "j_2", "j_3", "l_1", "l_2", "l_3")
        ),
        NINE("Wignerninejsym", 9,
                "\\begin{Bmatrix} var1 & var2 & var3 \\\\ var4 & var5 & var6 \\\\ var7 & var8 & var9 \\end{Bmatrix}",
                "\\Wignerninejsym{var1}{var2}{var3}{var4}{var5}{var6}",
                List.of("j_{11}", "j_{12}", "j_{13}",
                        "j_{21}", "j_{22}", "j_{23}",
                        "j_{31}", "j_{32}", "j_{33}")
        );

        private String macro, tex, semantic;
        private int args;
        private LinkedList<String> standardVars;

        Type(String macro, int args, String tex, String semantic, List<String> standardVars) {
            this.macro = macro;
            this.args = args;
            this.tex = tex;
            this.semantic = semantic;
            this.standardVars = new LinkedList<>(standardVars);
        }

        public String getMacro() {
            return macro;
        }
    }
}
