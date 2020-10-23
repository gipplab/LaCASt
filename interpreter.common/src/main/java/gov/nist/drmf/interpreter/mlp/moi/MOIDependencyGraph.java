package gov.nist.drmf.interpreter.mlp.moi;

import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphLib;
import prefuse.visual.VisualItem;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MOIDependencyGraph {
    private static final Logger LOG = LogManager.getLogger(MOIDependencyGraph.class.getName());

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    private final HashMap<String, MOINode> vertices;

    public MOIDependencyGraph() {
        this.vertices = new HashMap<>();
    }

    public void addNode(String id, String moi) throws ParseException {
        try {
            addNode(id, mlp.parse(moi));
        } catch (Exception e) {
            LOG.warn("Unable to generate MOI node for: " + moi, e);
        }
    }

    public void addNode(String id, PrintablePomTaggedExpression moi) {
        MOINode node = new MOINode(id, new MathematicalObjectOfInterest(moi));
        LOG.debug("Add node for moi: " + moi.getTexString());
        updateDependencies(node);
        vertices.put(node.getId(), node);
    }

    private void updateDependencies(MOINode node) {
        for (MOINode ref : vertices.values() ) {
            node.setupDependency(ref);
        }
    }

    public Map<String, MOINode> getVerticesMap() {
        return vertices;
    }

    public Collection<MOINode> getVertices() {
        return vertices.values();
    }

    public Collection<MOINode> getSinks() {
        return vertices.values().stream().filter(INode::isSink).collect(Collectors.toSet());
    }

    public Collection<MOINode> getSources() {
        return vertices.values().stream().filter(INode::isSource).collect(Collectors.toSet());
    }

    public static MOIDependencyGraph generateGraph(HashMap<String, String> mathNodeLibrary) throws ParseException {
        MOIDependencyGraph graph = new MOIDependencyGraph();
        LOG.info("Generate graph with " + mathNodeLibrary.size() + " nodes");

        for (Map.Entry<String, String> mathNode : mathNodeLibrary.entrySet()) {
            graph.addNode(mathNode.getKey(), mathNode.getValue());
        }

        return graph;
    }

    public static void main(String[] args) throws ParseException, IOException {
        HashMap<String, String> library = new HashMap<>();
        int[] counter = new int[]{0};

        Files.lines(Paths.get("interpreter.common/src/test/resources/gov/nist/drmf/interpreter/mlp/extensions/StressTestList.txt"))
                .forEach(l -> {
            library.put(""+counter[0], l);
            counter[0]++;
        });
        System.out.println("Loaded library.");

//        library.put("1", "f(x)");
//        library.put("2", "g(x)");
//        library.put("3", "f(z) + g(z)");

        MOIDependencyGraph moiGraph = MOIDependencyGraph.generateGraph(library);
        Graph graph = new Graph(true);
        graph.getNodeTable().addColumns(GraphLib.LABEL_SCHEMA);

        HashMap<MOINode, Node> nodeMapper = new HashMap<>();
        for ( MOINode moiNode : moiGraph.getVertices() ) {
            Node n = graph.addNode();
            n.setString("label", moiNode.getNode().getOriginalLaTeX());
            nodeMapper.put(moiNode, n);
        }

        for ( MOINode moiNode : moiGraph.getVertices() ) {
            for ( MOIDependency moiEdge : moiNode.getOutgoingDependencies() ) {
                graph.addEdge(
                        nodeMapper.get(moiNode),
                        nodeMapper.get(moiEdge.getSink())
                );
            }
        }

        // -- 2. the visualization --------------------------------------------
        // add the graph to the visualization as the data group "graph"
        // nodes and edges are accessible as "graph.nodes" and "graph.edges"
        Visualization vis = new Visualization();
        vis.add("graph", graph);
        vis.setInteractive("graph.edges", null, false);

        // -- 3. the renderers and renderer factory ---------------------------
        LabelRenderer r = new LabelRenderer(GraphLib.LABEL);
        r.setRoundedCorner(8, 8); // round the corners

        // create a new default renderer factory
        // return our name label renderer as the default for all non-EdgeItems
        // includes straight line edges for EdgeItems by default
        vis.setRendererFactory(new DefaultRendererFactory(r));

        // -- 4. the processing actions ---------------------------------------
        ColorAction fill = new ColorAction("graph.nodes",
                VisualItem.FILLCOLOR, ColorLib.rgb(200, 200, 255));
        // use black for node text
        ColorAction text = new ColorAction("graph.nodes",
                VisualItem.TEXTCOLOR, ColorLib.gray(0));
        // use light grey for edges
        ColorAction edges = new ColorAction("graph.edges",
                VisualItem.STROKECOLOR, ColorLib.gray(200));

        // create an action list containing all color assignments
        ActionList color = new ActionList();
        color.add(fill);
        color.add(text);
        color.add(edges);

        // create an action list with an animated layout
        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(new ForceDirectedLayout("graph"));
        layout.add(new RepaintAction());

        // add the actions to the visualization
        vis.putAction("color", color);
        vis.putAction("layout", layout);

        // -- 5. the display and interactive controls -------------------------
        int W = 640;
        int H = 480;
        Display d = new Display(vis) {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(W, H);
            }
        };
        d.setSize(W, H); // set display size
        d.pan(W / 2, H / 2); // pan to center
        d.addControlListener(new DragControl());
        d.addControlListener(new PanControl());
        d.addControlListener(new ZoomControl());

        // -- 6. launch the visualization -------------------------------------
        JFrame frame = new JFrame("prefuse label example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(d);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true); // show the window

        vis.run("color");
        vis.run("layout");
    }
}
