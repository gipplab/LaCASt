package gov.nist.drmf.interpreter.generic;

import com.formulasearchengine.mathosphere.mlp.pojos.RawWikiDocument;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
import gov.nist.drmf.interpreter.generic.mediawiki.DefiningFormula;
import gov.nist.drmf.interpreter.generic.mediawiki.MediaWikiHelper;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.Document;
import gov.nist.drmf.interpreter.generic.mlp.WikitextDocument;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class is an alternative way to get {@link SemanticEnhancedDocument} documents from a variety
 * of different arguments, including Wikidata ids and Wikipedia pages directly.
 * @author Andre Greiner-Petter
 */
public final class SemanticEnhancedDocumentBuilder {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancedDocumentBuilder.class.getName());

    private final MediaWikiHelper mwHelper;

    private static SemanticEnhancedDocumentBuilder builderInstance;

    private SemanticEnhancedDocumentBuilder() {
        mwHelper = new MediaWikiHelper();
    }

    public static SemanticEnhancedDocumentBuilder getDefaultBuilder() {
        if ( builderInstance == null )
            builderInstance = new SemanticEnhancedDocumentBuilder();
        return builderInstance;
    }

    public SemanticEnhancedDocument getDocument(String context) {
        Document document = ContextAnalyzer.getDocument(context);
        return getDocument(document);
    }

    public SemanticEnhancedDocument getDocument(Document document) {
        MLPDependencyGraph annotatedGraph = document.getMOIDependencyGraph();
        return new SemanticEnhancedDocument(document.getTitle(), annotatedGraph);
    }

    public SemanticEnhancedDocument getDocument(Article wikiArticle) {
        if ( wikiArticle == null ) {
            throw new IllegalArgumentException("Given wiki article does not exist");
        }
        RawWikiDocument rawWikiDoc = new RawWikiDocument(
                wikiArticle.getTitle(),
                0, wikiArticle.getText()
        );
        return getDocument(new WikitextDocument(rawWikiDoc));
    }

    public SemanticEnhancedDocument getDocument(SiteLink wikiDataSiteLink) {
        return getDocument(mwHelper.getArticle(wikiDataSiteLink));
    }

    /**
     * @param qid the QID of a wikidata item
     * @return the semantic enhanced document of the given wikidata QID
     * @throws MediaWikiApiErrorException if the given qid cannot be loaded from wikidata
     * @throws IOException if wikidata is not reachable
     * @throws IllegalArgumentException if the given argument is invalid because it is not a QID or is not linked
     * to an english wikipedia article
     */
    public SemanticEnhancedDocument getDocumentFromWikidataItem(String qid)
            throws MediaWikiApiErrorException, IOException, IllegalArgumentException {
        ItemDocument iDoc = mwHelper.getItemDocument(qid);
        SiteLink siteLink = mwHelper.getEnglishWikiSiteLink(iDoc);
        return getDocument(siteLink);
    }

    /**
     * Returns suggested defining formulae and has parts for a given QID. The result list might be empty.
     * @param qid given QID
     * @return suggested scored list of defining formulae and its elements
     * @throws MediaWikiApiErrorException cannot establish connection to mediawiki api
     * @throws IOException cannot read from mediawiki
     * @throws IllegalArgumentException given ID was not an item ID
     */
    public List<DefiningFormula> enhanceWikidataItem(String qid)
            throws MediaWikiApiErrorException, IOException, IllegalArgumentException {
        ItemDocument iDoc = mwHelper.getItemDocument(qid);
        String englishLabel = iDoc.getLabels().get("en").getText();

        SiteLink siteLink = mwHelper.getEnglishWikiSiteLink(iDoc);
        SemanticEnhancedDocument sed = getDocument(siteLink);

        List<DefiningFormula> defFormulae = new LinkedList<>();
        LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();

        List<MOIPresentations> mois = sed.getFormulae();
        for ( MOIPresentations moi : mois ) {
            List<FormulaDefinition> defs = moi.getDefiniens();
            double moiScore = 0;

            for ( FormulaDefinition def : defs ) {
                if ( def.getScore() < 0.8 ) break;
                String defStr = def.getDefinition();
                Integer levDistance = distance.apply(englishLabel, defStr);
                double score = def.getScore();
                if ( levDistance > 0 ) {
                    score *= 1./levDistance;
                }
                if ( score > moiScore ) moiScore = score;
            }

            DefiningFormula defMoi = new DefiningFormula(moiScore, moi);
            defFormulae.add(defMoi);
        }

        defFormulae.sort( Comparator.comparingDouble( DefiningFormula::getScore ) );
        Collections.reverse(defFormulae);

        List<DefiningFormula> results = new LinkedList<>();

        for (DefiningFormula moi : defFormulae) {
            if ( moi.getScore() == 0 ) break;
            results.add(moi);
        }

        return results;
    }
}
