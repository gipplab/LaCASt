package gov.nist.drmf.interpreter.generic.eval;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedGoldDocument;

/**
 * @author Andre Greiner-Petter
 */
public class TexTableEntry {
    private int num;
    private String idBase;
    private String url = "", urlName = "";
    private String cleanTex = "", tex = "", semantictex = "", mathematica = "", maple = "";

    public TexTableEntry(int num) {
        this.num = num;
        this.idBase = buildIDBase(num);
    }

    public static TexTableEntry generate(SemanticEnhancedGoldDocument goldSed) {
        TexTableEntry entry = new TexTableEntry(goldSed.getId());
        if ( goldSed.getFormulae().size() == 0 ) {
            System.err.println("ERR: Entry " + goldSed.getId() + " is empty!");
            return entry;
        }

        MOIPresentations moi = goldSed.getFormulae().get(0);
        entry.setCleanTex(cleanTex(moi.getGenericLatex()));
        entry.setTex(moi.getGenericLatex().replace("$", "$\\$$"));
        entry.setSemantictex(moi.getSemanticLatex());

        CASResult casResult = moi.getCasResults(Keys.KEY_MATHEMATICA);
        if ( casResult != null ) {
            entry.setMathematica(casResult.getCasRepresentation()
                    .replace("\n", " ").replace("$", "$\\$$"));
        }

        casResult = moi.getCasResults(Keys.KEY_MAPLE);
        if ( casResult != null ) {
            entry.setMaple(casResult.getCasRepresentation()
                    .replace("\n", " ").replace("$", "$\\$$"));
        }

        String url = "https://sigir21.wmflabs.org/wiki/"
                + goldSed.getTitle().replace(" ", "_")
                + "\\#"
                + goldSed.getEid();
        entry.setUrl(url);
        entry.setUrlName(goldSed.getTitle());
        return entry;
    }

    public static String cleanTex(String tex) {
        tex = tex.replace("\\begin{align}", "");
        tex = tex.replace("\\end{align}", "");
        tex = tex.replace("\\\\", " ");
        tex = tex.replace("&", "");
        tex = tex.replace("\n", "");
        return tex;
    }

    /**
     * Careful, number cannot exceed 26^2 (= 676)
     * @param num id
     * @return base id
     */
    private String buildIDBase(int num) {
        char upperID = (char)('A' + (num / 26));
        char lowerID = (char)('A' + (num % 26));
        return upperID + "" + lowerID;
    }

    public String buildID(String post) {
        return idBase + post;
    }

    public int getNum() {
        return num;
    }

    public String getCleanTex() {
        return cleanTex;
    }

    public TexTableEntry setCleanTex(String cleanTex) {
        this.cleanTex = cleanTex;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public TexTableEntry setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUrlName() {
        return urlName;
    }

    public TexTableEntry setUrlName(String urlName) {
        this.urlName = urlName;
        return this;
    }

    public String getTex() {
        return tex;
    }

    public TexTableEntry setTex(String tex) {
        this.tex = tex;
        return this;
    }

    public String getSemantictex() {
        return semantictex;
    }

    public TexTableEntry setSemantictex(String semantictex) {
        this.semantictex = semantictex;
        return this;
    }

    public String getMathematica() {
        return mathematica;
    }

    public TexTableEntry setMathematica(String mathematica) {
        this.mathematica = mathematica;
        return this;
    }

    public String getMaple() {
        return maple;
    }

    public TexTableEntry setMaple(String maple) {
        this.maple = maple;
        return this;
    }
}
