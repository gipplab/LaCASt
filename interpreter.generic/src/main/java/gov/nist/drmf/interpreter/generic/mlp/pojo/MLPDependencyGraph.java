package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTagGraph;
import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.pom.moi.MOIDependency;
import gov.nist.drmf.interpreter.pom.moi.MOIDependencyGraph;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MLPDependencyGraph extends MOIDependencyGraph<MOIAnnotation> implements MathTagGraph {
    private static final Logger LOG = LogManager.getLogger(MLPDependencyGraph.class.getName());

    public MLPDependencyGraph() {
        super();
    }

    @Override
    public void addFormula(MathTag mathTag) {
        try {
            addFormulaNode(mathTag);
        } catch (ParseException e) {
            LOG.error("Unable to add formula to graph", e);
        }
    }

    public MOINode<MOIAnnotation> addFormulaNode(MathTag mathTag) throws ParseException {
        return super.addNode(mathTag.placeholder(), mathTag.getContent(), new MOIAnnotation(mathTag));
    }

    @Override
    public MathTag removeFormula(MathTag mathTag) {
        super.removeNode(mathTag.placeholder());
        return mathTag;
    }

    @Override
    public boolean contains(MathTag mathTag) {
        return super.containsNode(mathTag.placeholder());
    }

    @Override
    public void appendMOIRelation(MathTag mathTag, Relation relation) {
        MOINode<MOIAnnotation> node = super.getNode(mathTag.placeholder());
        if ( node == null ) {
            LOG.warn("Given mathtag does not exist, nothing to append.");
            return;
        }

        node.getAnnotation().appendRelation(relation);
    }

    @Override
    public void setMOIRelation(MathTag mathTag, Collection<Relation> relations) {
        MOINode<MOIAnnotation> node = super.getNode(mathTag.placeholder());
        if ( node == null ) {
            LOG.warn("Given mathtag does not exist, nothing to append.");
            return;
        }

        node.getAnnotation().setRelations(relations);
    }

    @Override
    public List<Relation> getRelations(MathTag mathTag) {
        MOINode<MOIAnnotation> node = super.getNode(mathTag.placeholder());
        if ( node == null ) {
            LOG.warn("Given mathtag does not exist, nothing to append.");
            return new LinkedList<>();
        }

        return node.getAnnotation().getAttachedRelations();
    }

    @Override
    public Collection<MathTag> getOutgoingEdges(MathTag mathTag) {
        MOINode<MOIAnnotation> node = super.getNode(mathTag.placeholder());
        if ( node == null ) return new HashSet<>();
        return node.getOutgoingDependencies().stream()
                .map( MOIDependency::getSink )
                .map( MOINode<MOIAnnotation>::getAnnotation )
                .map( MOIAnnotation::getFormula )
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<MathTag> getIngoingEdges(MathTag mathTag) {
        MOINode<MOIAnnotation> node = super.getNode(mathTag.placeholder());
        if ( node == null ) return new HashSet<>();
        return node.getIngoingDependencies().stream()
                .map( MOIDependency::getSource )
                .map( MOINode<MOIAnnotation>::getAnnotation )
                .map( MOIAnnotation::getFormula )
                .collect(Collectors.toSet());
    }
}
