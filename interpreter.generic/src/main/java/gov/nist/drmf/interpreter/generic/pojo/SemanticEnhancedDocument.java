package gov.nist.drmf.interpreter.generic.pojo;

import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocument {

    private final List<MOIPresentations> formulae;

    public SemanticEnhancedDocument() {
        this.formulae = new LinkedList<>();
    }

    public SemanticEnhancedDocument(MLPDependencyGraph graph) {
        this.formulae = graph.getVertices().stream()
                .map( node -> node.getNode() )
                .map( moi -> moi.getOriginalLaTeX() )
                .map( MOIPresentations::new )
                .collect(Collectors.toList());
    }

    public Stream<MOIPresentations> stream() {
        return formulae.stream();
    }

    public List<MOIPresentations> getFormulae() {
        return formulae;
    }
}
