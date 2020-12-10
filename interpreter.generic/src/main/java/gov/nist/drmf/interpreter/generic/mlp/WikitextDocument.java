package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.cli.BaseConfig;
import com.formulasearchengine.mathosphere.mlp.contracts.CreateCandidatesMapper;
import com.formulasearchengine.mathosphere.mlp.contracts.WikiTextAnnotatorMapper;
import com.formulasearchengine.mathosphere.mlp.pojos.DocumentMetaLib;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.ParsedWikiDocument;
import com.formulasearchengine.mathosphere.mlp.pojos.RawWikiDocument;
import com.formulasearchengine.mathosphere.mlp.text.WikiTextUtils;
import gov.nist.drmf.interpreter.common.config.ConfigDiscovery;
import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class WikitextDocument extends RawWikiDocument implements Document {
    private static final Logger LOG = LogManager.getLogger(WikitextDocument.class.getName());

    private final GenericLacastConfig lacastConfig;

    private final BaseConfig mlpConfig;

    public WikitextDocument(String context) {
        super(context);
        this.lacastConfig = ConfigDiscovery.getConfig().getGenericLacastConfig();
        this.mlpConfig = buildConfig();
    }

    public WikitextDocument(RawWikiDocument rawWikiDocument) {
        super(rawWikiDocument);
        this.lacastConfig = ConfigDiscovery.getConfig().getGenericLacastConfig();
        this.mlpConfig = buildConfig();
    }

    @Override
    public MLPDependencyGraph getMOIDependencyGraph() {
        MLPDependencyGraph graph = new MLPDependencyGraph();
        ParsedWikiDocument parsedWikiDocument = generateParsedWikiDocument(graph);

        CreateCandidatesMapper mlp = new CreateCandidatesMapper(mlpConfig);
        mlp.moiMapping(parsedWikiDocument);
        return graph;
    }

    @Override
    public MOINode<MOIAnnotation> getAnnotatedMOINode(String latex) throws ParseException {
        // first we got the parsed document
        MLPDependencyGraph graph = new MLPDependencyGraph();
        ParsedWikiDocument parsedWikiDocument = generateParsedWikiDocument(graph);

        MathTag mathTag = new MathTag(latex, WikiTextUtils.MathMarkUpType.LATEX);
        MOINode<MOIAnnotation> node = graph.addFormulaNode( mathTag );

        // looks strange but if the node already exist in the graph, it may have positions attached
        // and positions are necessary to perform scoring between definitions and formulae.
        // hence we need this new math tag (with potential positions) and not the mathtag object we created.
        mathTag = node.getAnnotation().getFormula();

        CreateCandidatesMapper mlp = new CreateCandidatesMapper(mlpConfig);
        mlp.analyzeSingleFormulaWithDependencies(parsedWikiDocument, mathTag);
        return node;
    }

    private ParsedWikiDocument generateParsedWikiDocument(MLPDependencyGraph graph) {
        DocumentMetaLib metaLib = new DocumentMetaLib(graph);

        WikiTextAnnotatorMapper annotator = new WikiTextAnnotatorMapper(mlpConfig);
        annotator.open(null);

        return annotator.parse(this, metaLib);
    }

    private BaseConfig buildConfig() {
        BaseConfig config = new BaseConfig();
        config.setUseTeXIdentifiers(true);
        config.setUseMOI(true);
        config.setDefinitionMerging(true);
        config.setTexvcinfoUrl(this.lacastConfig.getMathoidUrl());
        return config;
    }
}
