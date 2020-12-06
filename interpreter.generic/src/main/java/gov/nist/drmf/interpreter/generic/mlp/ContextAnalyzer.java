package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.cli.BaseConfig;
import com.formulasearchengine.mathosphere.mlp.contracts.CreateCandidatesMapper;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextAnnotatorMapper;
import com.formulasearchengine.mathosphere.mlp.pojos.*;
import gov.nist.drmf.interpreter.common.config.ConfigDiscovery;
import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.generic.mlp.struct.ContextContentType;
import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class ContextAnalyzer {
    private static final Logger LOG = LogManager.getLogger(ContextAnalyzer.class.getName());

    private final GenericLacastConfig config;

    private final String context;
    private final ContextContentType contentType;

    private String title;

    private RawWikiDocument rawWikiDocument;

    private MLPDependencyGraph dependencyGraph;

    public ContextAnalyzer(String context) {
        this(context, ContextContentType.guessContentType(context));
    }

    public ContextAnalyzer(RawWikiDocument rawWikiDocument) {
        this.config = ConfigDiscovery.getConfig().getGenericLacastConfig();
        this.context = rawWikiDocument.getContent();
        this.title = rawWikiDocument.getTitle();
        this.contentType = ContextContentType.WIKITEXT;
        this.rawWikiDocument = rawWikiDocument;
    }

    public ContextAnalyzer(String context, ContextContentType contentType) {
        if( ContextContentType.INDETERMINATE.equals(contentType) ) {
            throw new IllegalArgumentException("An indeterminate content type is not supported.");
        }

        this.config = ConfigDiscovery.getConfig().getGenericLacastConfig();
        this.context = context;
        this.contentType = contentType;
    }

    public void analyze() throws IllegalCallerException {
        switch (contentType) {
            case WIKITEXT:
                this.dependencyGraph = rawWikiDocument != null ?
                        extractDefiniensFromWikitext(rawWikiDocument) :
                        extractDefiniensFromWikitext();
                break;
            case LATEX:
                this.dependencyGraph = extractDefiniensFromLaTeX();
                break;
            default:
                throw new IllegalCallerException("Unable extract relations for the given content type.");
        }
    }

    private MLPDependencyGraph extractDefiniensFromWikitext() {
        RawWikiDocument document = new RawWikiDocument(context);
        this.title = document.getTitle();
        return extractDefiniensFromWikitext(document);
    }

    private MLPDependencyGraph extractDefiniensFromWikitext(RawWikiDocument doc) {
        if ( doc == null ) throw new NullPointerException("RawWikiDocument was null");

        BaseConfig config = buildConfig();
        MLPDependencyGraph graph = new MLPDependencyGraph();
        DocumentMetaLib metaLib = new DocumentMetaLib(graph);

        WikiTextAnnotatorMapper annotator = new WikiTextAnnotatorMapper(config);
        annotator.open(null);

        ParsedWikiDocument parsedWikiDocument = annotator.parse(doc, metaLib);
        CreateCandidatesMapper mlp = new CreateCandidatesMapper(config);
        mlp.moiMapping(parsedWikiDocument);
        return graph;
    }

    private MLPDependencyGraph extractDefiniensFromLaTeX() {
        throw new IllegalCallerException("LaTeX format is not yet supported");
    }

    private BaseConfig buildConfig() {
        BaseConfig config = new BaseConfig();
        config.setUseTeXIdentifiers(true);
        config.setUseMOI(true);
        config.setDefinitionMerging(true);
        config.setTexvcinfoUrl(this.config.getMathoidUrl());
        return config;
    }

    public String getTitle() {
        return title;
    }

    public MLPDependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }
}
