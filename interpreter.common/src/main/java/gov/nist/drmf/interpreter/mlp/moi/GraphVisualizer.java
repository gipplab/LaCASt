package gov.nist.drmf.interpreter.mlp.moi;

import mlp.ParseException;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class GraphVisualizer {

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        HashMap<String, String> library = new HashMap<>();
        int[] counter = new int[]{0};

        Files.lines(Paths.get("interpreter.common/src/test/resources/gov/nist/drmf/interpreter/mlp/extensions/StressTestList.txt"))
                .forEach(l -> {
            library.put(""+counter[0], l);
            counter[0]++;
        });
        MOIDependencyGraph moiGraph = MOIDependencyGraphBuilder.generateGraph(library);
        System.out.println("Loaded library.");

        String searchFor = "";

        System.setProperty("org.graphstream.ui", "swing");
        Graph graph = new SingleGraph("Tutorial 1");
//        graph.display();



        int i = 0;
        for ( MOINode moiNode : moiGraph.getVertices() ) {
            if ( (!searchFor.isBlank() && !moiNode.getNode().getOriginalLaTeX().contains(searchFor))
                    || nodeMemory.contains(moiNode.hashCode())
                    || moiNode.isIsolated()
            ) continue;
            Node n = graph.addNode(moiNode.hashCode()+"");
            nodeMemory.add(moiNode.hashCode());
            setNodeAttribute(n, moiNode.getNode().getOriginalLaTeX());

//            Thread.sleep(500);

//            addDependencies(graph, moiNode.getOutgoingDependencies(), true);
//            addDependencies(graph, moiNode.getIngoingDependencies(), false);

//            Thread.sleep(500);
        }

        for ( MOINode<?> moiNode : moiGraph.getVertices() ) {
            for ( MOIDependency moiEdge : moiNode.getOutgoingDependencies() ) {
                if ( !nodeMemory.contains(moiNode.hashCode()) || !nodeMemory.contains(moiEdge.getSink().hashCode()) ) continue;
                Edge e = graph.addEdge(
                        "E"+i,
                        moiNode.hashCode()+"",
                        moiEdge.getSink().hashCode()+"",
                        true
                );
                setEdgeAttribute(e);

//                e.setAttribute("layout.weight", 3);
                i++;
//                Thread.sleep(500);
            }
        }

//        graph.addNode("A");
//        graph.addNode("B");
//        graph.addNode("C");
//        graph.addEdge("AB", "A", "B");
//        graph.addEdge("BC", "B", "C");
//        graph.addEdge("CA", "C", "A");

        graph.setAttribute("ui.quality", true);
        graph.setAttribute("ui.antialias", true);

        graph.setAttribute("layout.stabilization-limit", 0.999999);
//        graph.setAttribute("layout.quality", 3);
        graph.setAttribute("layout.force", 0.9);

        Viewer viewer = graph.display();
    }

    private static Set<Integer> nodeMemory = new HashSet<>();

    private static void addDependencies(Graph graph, Collection<MOIDependency> dependencyList, boolean outgoing) throws InterruptedException {
        for ( MOIDependency moiEdge : dependencyList ) {
            MOINode node = outgoing ? moiEdge.getSink() : moiEdge.getSource();
            if ( !nodeMemory.contains(node.hashCode()) ) {
                Node sn = graph.addNode(node.hashCode()+"");
                nodeMemory.add(node.hashCode());
                setNodeAttribute(sn, node.getNode().getOriginalLaTeX());

                int source = outgoing ? moiEdge.getSource().hashCode() : node.hashCode();
                int sink = outgoing ? node.hashCode() : moiEdge.getSink().hashCode();

                Edge e = graph.addEdge(
                        "E"+moiEdge.hashCode()+node.hashCode(),
                        source+"",
                        sink+"",
                        true
                );
                setEdgeAttribute(e);
                addDependencies(graph, node.getOutgoingDependencies(), true);
                addDependencies(graph, node.getIngoingDependencies(), false);
            }
        }
    }

    private static void setNodeAttribute(Node n, String label) {
        n.setAttribute("ui.label", label);
        n.setAttribute("ui.style", "fill-color: rgb(100,100,100); size: 10px; text-alignment: under; text-color: white; text-style: bold; text-background-mode: rounded-box; text-background-color: #222C; text-padding: 5px, 4px; text-offset: 0px, 5px;");
    }

    private static void setEdgeAttribute(Edge e) {
        e.setAttribute("ui.style", "arrow-shape: arrow; arrow-size: 5px; fill-color: #444;");
        e.setAttribute("layout.weight", 3);
    }
}
