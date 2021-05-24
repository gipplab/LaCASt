package gov.nist.drmf.interpreter.generic;

import com.formulasearchengine.mathosphere.mlp.pojos.RawWikiDocument;
import gov.nist.drmf.interpreter.generic.mlp.ContextAnalyzer;
import gov.nist.drmf.interpreter.generic.mlp.Document;
import gov.nist.drmf.interpreter.generic.mlp.WikitextDocument;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class is an alternative way to get {@link SemanticEnhancedDocument} documents from a variety
 * of different arguments, including Wikidata ids and Wikipedia pages directly.
 * @author Andre Greiner-Petter
 */
public final class SemanticEnhancedDocumentBuilder {
    private final MediaWikiBot wikipediaBot;

    private final WikibaseDataFetcher wikidataFetcher;

    private static SemanticEnhancedDocumentBuilder builderInstance;

    private SemanticEnhancedDocumentBuilder() {
        HttpActionClient client = HttpActionClient.builder()
                .withUrl("https://en.wikipedia.org/w/")
                .withUserAgent("DKEWuppertalUniversityBot", "1.0", "andre.greiner-petter@t-online.de")
                .withRequestsPerUnit(1, TimeUnit.MINUTES)
                .build();
        wikipediaBot = new MediaWikiBot(client);
        wikidataFetcher = WikibaseDataFetcher.getWikidataDataFetcher();
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
        String enPageTitle = wikiDataSiteLink.getPageTitle();
        return getDocument(wikipediaBot.getArticle(enPageTitle));
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
        EntityDocument entityDocument = wikidataFetcher.getEntityDocument(qid);
        if (!(entityDocument instanceof ItemDocument)) {
            throw new IllegalArgumentException("Given ID '"+qid+"' is not a Wikidata item");
        }

        ItemDocument iDoc = (ItemDocument) entityDocument;
        Map<String, SiteLink> linkMap = iDoc.getSiteLinks();
        if ( linkMap == null || !linkMap.containsKey("enwiki") ) {
            String errorMessage = String.format(
                    "The given QID '%s' with title '%s' is not linked to an English Wikipedia article",
                    qid, iDoc.getLabels().get("enwiki")
            );
            throw new IllegalArgumentException(errorMessage);
        }

        return getDocument(linkMap.get("enwiki"));
    }
}
