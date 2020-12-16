package gov.nist.drmf.interpreter.pom.moi;

import gov.nist.drmf.interpreter.common.interfaces.IMapStringFunction;
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

    public static MOIDependencyGraph<Void> generateGraph(Map<String, String> mathNodeLibrary) throws ParseException {
        MOIDependencyGraph<Void> graph = new MOIDependencyGraph<>();
        LOG.info("Generate graph with " + mathNodeLibrary.size() + " nodes");

        for (Map.Entry<String, String> mathNode : mathNodeLibrary.entrySet()) {
            graph.addNode(mathNode.getKey(), mathNode.getValue());
        }

        return graph;
    }

    /**
     * Generates an annotated graph. The {@link IMapStringFunction} defines how to get the LaTeX string content
     * of the annotation object.
     * @param mathNodeLibrary the library of objects that is used to generate the graph
     * @param contentMapper the method to get the LaTeX string of the annotation object
     * @param <T> the annotation object class
     * @return the annotated graph of the provided map
     * @throws ParseException if the latex string cannot be parsed
     */
    public static <T> MOIDependencyGraph<T> generateAnnotatedGraph(
            Map<String, T> mathNodeLibrary,
            IMapStringFunction<T> contentMapper
    ) throws ParseException {
        MOIDependencyGraph<T> graph = new MOIDependencyGraph<>();
        LOG.info("Generate annotated graph with " + mathNodeLibrary.size() + " nodes");

        for (Map.Entry<String, T> mathNode : mathNodeLibrary.entrySet()) {
            T value = mathNode.getValue();
            graph.addNode(mathNode.getKey(), contentMapper.get(value), value);
        }

        return graph;
    }
}
