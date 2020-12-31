package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTagGraph;
import com.formulasearchengine.mathosphere.mlp.pojos.Position;
import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import com.formulasearchengine.mathosphere.mlp.text.WikiTextUtils;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
import gov.nist.drmf.interpreter.pom.moi.*;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MLPDependencyGraph extends MOIDependencyGraph<MOIAnnotation> implements MathTagGraph {
    private static final Logger LOG = LogManager.getLogger(MLPDependencyGraph.class.getName());

    public MLPDependencyGraph() {
        super();
    }

    /**
     * This constructs an existing dependency graph from the given list of MOIs.
     * @param formulae the list of nodes for this graph including dependencies
     */
    public MLPDependencyGraph(List<MOIPresentations> formulae) {
        Map<String, String> texIDMap = addNodes(formulae);
        addDependencies(formulae, texIDMap);
    }

    private Map<String, String> addNodes(List<MOIPresentations> formulae) {
        Map<String, String> texIDMap = new HashMap<>();
        for ( MOIPresentations f : formulae ) {
            MathTag mathTag = new MathTag(f.getGenericLatex(), WikiTextUtils.MathMarkUpType.LATEX);
            texIDMap.put( f.getGenericLatex(), mathTag.placeholder() );
            for ( Position p : f.getPositions() ) mathTag.addPosition(p);

            MOIAnnotation annotation = new MOIAnnotation(mathTag);
            for ( FormulaDefinition def : f.getDefiniens() ) {
                Relation relation = new Relation();
                relation.setMathTag(mathTag);
                relation.setDefinition(def.getDefinition());
                relation.setScore(def.getScore());
                annotation.appendRelation( relation );
            }

            try {
                // I wonder if we really must create an MOI here including parsing all the stuff.
                // Probably we must because we take the PTE later to translate it to CAS...
                MathematicalObjectOfInterest moi = new MathematicalObjectOfInterest(mathTag.getContent());
                MOINode<MOIAnnotation> node = new MOINode<>(mathTag.placeholder(), moi, annotation);
                super.addNode(node);
            } catch (ParseException e) {
                LOG.warn("Unable to generate MOI from given TeX string. " +
                        "Not adding the node to the graph even though it existed in the given JSON.");
            }
        }
        return texIDMap;
    }

    private void addDependencies(List<MOIPresentations> formulae, Map<String, String> texToIdMap) {
        for ( MOIPresentations f : formulae ) {
            String id = texToIdMap.get(f.getGenericLatex());
            if ( id == null ) continue;

            MOINode<MOIAnnotation> node = super.getNode(id);
            List<String> ingoingNodes = f.getIngoingNodes();
            for ( String sources : ingoingNodes ) {
                id = texToIdMap.get(sources);
                if ( id == null ) continue;
                MOINode<MOIAnnotation> sourceNode = super.getNode(id);
                super.addDependency(sourceNode, node);
            }

            List<String> outgoingNodes = f.getOutgoingNodes();
            for ( String sink : outgoingNodes ) {
                id = texToIdMap.get(sink);
                if ( id == null ) continue;
                MOINode<MOIAnnotation> sinkNode = super.getNode(id);
                super.addDependency(node, sinkNode);
            }
        }
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
                .map( IDependency::getSink )
                .map( n -> (MOINode<MOIAnnotation>)n )
                .map( MOINode::getAnnotation )
                .map( MOIAnnotation::getFormula )
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<MathTag> getIngoingEdges(MathTag mathTag) {
        MOINode<MOIAnnotation> node = super.getNode(mathTag.placeholder());
        if ( node == null ) return new HashSet<>();
        return node.getIngoingDependencies().stream()
                .map( IDependency::getSource )
                .map( n -> (MOINode<MOIAnnotation>)n )
                .map( MOINode::getAnnotation )
                .map( MOIAnnotation::getFormula )
                .collect(Collectors.toSet());
    }
}
