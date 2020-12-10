package gov.nist.drmf.interpreter.generic.mlp;

import gov.nist.drmf.interpreter.generic.mlp.struct.ContextContentType;
import org.apache.logging.log4j.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * @author Andre Greiner-Petter
 */
public final class ContextAnalyzer {
    private static final Logger LOG = getLogger(ContextAnalyzer.class.getName());

    private ContextAnalyzer(){}

    public static Document getDocument(String context) {
        return getDocument(context, ContextContentType.guessContentType(context));
    }

    /**
     * Returns the document instance for the given context
     * @param context the string of the document
     * @param type the content type of the document
     * @return the document instance or null
     */
    public static Document getDocument(String context, ContextContentType type) {
        boolean fallback = true;
        switch (type) {
            case LATEX:
                LOG.error("LaTeX documents are not yet supported");
                return null;
            case WIKITEXT:
                fallback = false;
            case INDETERMINATE:
            default:
                if ( fallback ) LOG.info("We are not able to analyze the context type. " +
                        "Assuming default document type Wikitext");
                return new WikitextDocument(context);
        }
    }
}
