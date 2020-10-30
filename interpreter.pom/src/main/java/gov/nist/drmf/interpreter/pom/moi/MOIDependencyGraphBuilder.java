package gov.nist.drmf.interpreter.pom.moi;

import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public final class MOIDependencyGraphBuilder {
    private static final Logger LOG = LogManager.getLogger(MOIDependencyGraphBuilder.class.getName());

    private MOIDependencyGraphBuilder() {}

    public static MOIDependencyGraph generateGraph(Map<String, String> mathNodeLibrary) throws ParseException {
        MOIDependencyGraph graph = new MOIDependencyGraph();
        LOG.info("Generate graph with " + mathNodeLibrary.size() + " nodes");

        for (Map.Entry<String, String> mathNode : mathNodeLibrary.entrySet()) {
            graph.addNode(mathNode.getKey(), mathNode.getValue());
        }

        return graph;
    }

//    public static MOIDependencyGraph generateAnnotatedGraph(Map<String, MathTag> mathNodeLibrary) throws ParseException {
//        MOIDependencyGraph graph = new MOIDependencyGraph();
//        LOG.info("Generate annotated graph with " + mathNodeLibrary.size() + " nodes");
//
//        for (Map.Entry<String, MathTag> mathNode : mathNodeLibrary.entrySet()) {
//            MathTag tag = mathNode.getValue();
//            graph.addNode(mathNode.getKey(), tag.getContent(), tag);
//        }
//
//        return graph;
//    }
}
