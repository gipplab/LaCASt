package gov.nist.drmf.interpreter.generic.eval;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.generic.EvaluationHelper;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedGoldDocument;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;
import gov.nist.drmf.interpreter.mathematica.core.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import gov.nist.drmf.interpreter.pom.extensions.MatchablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.extensions.PomMatcherBuilder;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class TexTableEntry {
    private static final Logger LOG = LogManager.getLogger(TexTableEntry.class.getName());

    private int num;
    private String title;
    private String idBase;
    private String url = "", urlName = "";
    private String cleanTex = "", tex = "", semantictex = "", mathematica = "", maple = "";
    private String origMM = "", origMA = "";

    private String translationST = "", translationMM = "", translationMA = "";
    private boolean correctTST = false, correctTMM = false, correctTMA = false;

    public TexTableEntry(int num) {
        this.num = num;
        this.idBase = buildIDBase(num);
        this.title = "ungenerated";
    }

    public static TexTableEntry generate(SemanticEnhancedGoldDocument goldSed) {
        TexTableEntry entry = new TexTableEntry(goldSed.getId());
        entry.title = goldSed.getTitle();

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
            entry.origMM = casResult.getCasRepresentation();
            entry.setMathematica(casResult.getCasRepresentation()
                    .replace("\n", " ").replace("$", "$\\$$"));
        }

        casResult = null;
        casResult = moi.getCasResults(Keys.KEY_MAPLE);
        if ( casResult != null ) {
            entry.origMA = casResult.getCasRepresentation();
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

    public String getGoldURL() {
        return "https://sigir21.wmflabs.org/wiki/Gold_" + num;
    }

    public static String cleanTex(String tex) {
        tex = tex.replace("\\begin{align}", "");
        tex = tex.replace("\\end{align}", "");
        tex = tex.replace("\\\\", " ");
        tex = tex.replace("&", "");
        tex = tex.replace("\n", "");
        return tex;
    }

    public void addTranslations(SemanticEnhancedDocument sed) throws ParseException {
        if ( !sed.getTitle().equals(this.title) )
            throw new IllegalArgumentException("Given translation doesnt match gold entry: '" +
                    this.title + "' vs '" + sed.getTitle() + "'");

        MOIPresentations moi = sed.getFormulae().get(0);
        this.translationST = moi.getSemanticLatex();

        if ( moi.getCasResults(Keys.KEY_MATHEMATICA) != null )
            this.translationMM = moi.getCasResults(Keys.KEY_MATHEMATICA).getCasRepresentation();
        if ( moi.getCasResults(Keys.KEY_MAPLE) != null )
            this.translationMA = moi.getCasResults(Keys.KEY_MAPLE).getCasRepresentation();

        // when is what correct? lets do it simple just for now
        this.correctTST = matchSemanticLatex(semantictex, translationST);
        LOG.info(sed.getTitle() + ": Semantic LaTeX: " + correctTST);
        this.correctTMM = matchMathematica(origMM, translationMM);
        LOG.info(sed.getTitle() + ": Mathematica: " + correctTMM);
        this.correctTMA = matchMaple(origMA, translationMA);
        LOG.info(sed.getTitle() + ": Maple: " + correctTMA);
    }

    private boolean matchSemanticLatex(String latex, String translation) {
        if ( latex.equals(translation) ) return true;

        if ( latex.replace(" ", "").equals(translation.replace(" ", "")) ) return true;

        // if that didnt work, match via pom matcher
        try {
            MatchablePomTaggedExpression mpom = PomMatcherBuilder.compile(latex);
            return mpom.match(translation);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean matchMathematica(String mm, String translation) {
        if ( mm == null || mm.isEmpty() ) return false;
        if ( translation == null || translation.isEmpty() ) return false;
        if ( mm.equals(translation) ) return true;
        if ( mm.replace(" ", "").equals(translation.replace(" ", "")) ) return true;

        try {
//            mm = "HoldForm[" + mm + "]";
//            translation = "HoldForm[" + translation + "]";

            MathematicaInterface mi = MathematicaInterface.getInstance();
            String goldParsed = mi.evaluate(mm);
            String tranParsed = mi.evaluate(translation);

            EvaluationHelper.clearCache(mi);
            return goldParsed.equals(tranParsed);
        } catch (MathLinkException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean matchMaple(String ma, String translation) {
        if ( ma == null || ma.isEmpty() ) return false;
        if ( translation == null || translation.isEmpty() ) return false;
        if ( ma.equals(translation) ) return true;
        if ( ma.replace(" ", "").equals(translation.replace(" ", "")) ) return true;

        MapleInterface mi = MapleInterface.getUniqueMapleInterface();
        try {
            Algebraic goldPrasedA = mi.evaluate(ma+";");
            Algebraic tranPrasedA = mi.evaluate(translation+";");

            if ( goldPrasedA == null ) return tranPrasedA == null;
            String goldParsed = goldPrasedA.toString();
            if ( tranPrasedA == null ) return false;
            else return goldParsed.equals(tranPrasedA.toString());
        } catch (MapleException | NullPointerException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                mi.restart();
            } catch (MapleException e) {
                e.printStackTrace();
            }
        }
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

    public String getTitle() {
        return title;
    }

    public String getIdBase() {
        return idBase;
    }

    public String getTranslationST() {
        return translationST;
    }

    public String getTranslationMM() {
        return translationMM;
    }

    public String getTranslationMA() {
        return translationMA;
    }

    public boolean isCorrectTST() {
        return correctTST;
    }

    public boolean isCorrectTMM() {
        return correctTMM;
    }

    public boolean isCorrectTMA() {
        return correctTMA;
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
