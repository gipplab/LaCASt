package gov.nist.drmf.interpreter.generic.mediawiki;

import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Andre Greiner-Petter
 */
public class MediaWikiHelper {
    private static final Logger LOG = LogManager.getLogger(MediaWikiHelper.class.getName());

    private final MediaWikiBot wikipediaBot;

    private final WikibaseDataFetcher wikidataFetcher;

    public MediaWikiHelper() {
        HttpActionClient client = HttpActionClient.builder()
                .withUrl("https://en.wikipedia.org/w/")
                .withUserAgent("DKEWuppertalUniversityBot", "1.0", "andre.greiner-petter@t-online.de")
                .withRequestsPerUnit(1, TimeUnit.MINUTES)
                .build();
        wikipediaBot = new MediaWikiBot(client);
        wikidataFetcher = WikibaseDataFetcher.getWikidataDataFetcher();
    }

    public Article getArticle(SiteLink siteLink) {
        String enPageTitle = siteLink.getPageTitle();
        return wikipediaBot.getArticle(enPageTitle);
    }

    public ItemDocument getItemDocument(String qid)
            throws MediaWikiApiErrorException, IOException, IllegalArgumentException {
        LOG.debug("Try loading QID " + qid + " from Wikidata");
        EntityDocument entityDocument = wikidataFetcher.getEntityDocument(qid);
        if (!(entityDocument instanceof ItemDocument)) {
            LOG.error("Found an entity but it is not an item");
            throw new IllegalArgumentException("Given ID '"+qid+"' is not a Wikidata item");
        }

        return (ItemDocument) entityDocument;
    }

    public SiteLink getEnglishWikiSiteLink(ItemDocument iDoc) {
        String qid = iDoc.getEntityId().getId();
        LOG.debug("Loaded Wikidata item " + qid + ". Search for enwiki links");
        Map<String, SiteLink> linkMap = iDoc.getSiteLinks();
        if ( linkMap == null || !linkMap.containsKey("enwiki") ) {
            LOG.error("Given QID " + qid + " does not contain an english wikipedia link");
            String errorMessage = String.format(
                    "The given QID '%s' with title '%s' is not linked to an English Wikipedia article",
                    qid, iDoc.getLabels().get("enwiki")
            );
            throw new IllegalArgumentException(errorMessage);
        }
        return linkMap.get("enwiki");
    }
}
