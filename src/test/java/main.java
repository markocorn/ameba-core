//import ameba.core.blocks.Cell;
//import ameba.core.blocks.connections.Edge;
//import ameba.core.blocks.nodes.Node;
//import ameba.core.factories.*;
//import ameba.core.reproductions.mutateCell.ReplaceNode;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import java.io.File;
//import java.io.IOException;
//
///**
// * Created by marko on 10/24/16.
// */
//public class main {
//    public static void main(String[] args) {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core/src/main/resources/settings.json"));
//            EdgeFactory edgeFactory = new EdgeFactory(new EdgeFactorySettings(jsonSettings.get("edgeFactorySettings").toString()));
//            NodeFactory nodeFactory = new NodeFactory(NodeFactorySettings.genNodeFactorySettingsHashMap(jsonSettings.get("nodeFactorySettings").toString()));
//            CellFactory cellFactory = new CellFactory(new CellFactorySettings(jsonSettings.get("cellFactorySettings").toString()), nodeFactory, edgeFactory);
//            Cell cell1 = cellFactory.genCellRnd();
//            System.out.println(CellFactory.verifyCellConnections(cell1));
//
//            Node inp1 = nodeFactory.genNode("InputDec");
//            Node inp2 = nodeFactory.genNode("InputDec");
//            Node inp3 = nodeFactory.genNode("InputDec");
//            Node out1 = nodeFactory.genNode("OutputDec");
//            Node out2 = nodeFactory.genNode("OutputDec");
//            Node out3 = nodeFactory.genNode("OutputDec");
//
//            Node add = nodeFactory.genNode("Add");
//
//            Edge edge1 = edgeFactory.genEdge(inp1, add);
//            Edge edge2 = edgeFactory.genEdge(inp2, add);
//            Edge edge3 = edgeFactory.genEdge(inp3, add);
//            Edge edge4 = edgeFactory.genEdge(add, out1);
//            Edge edge5 = edgeFactory.genEdge(add, out2);
//            Edge edge6 = edgeFactory.genEdge(add, out3);
//
//
//            Cell cell = new Cell();
//            cell.addNode(inp1);
//            cell.addNode(inp2);
//            cell.addNode(inp3);
//            cell.addNode(add);
//            cell.addNode(out1);
//            cell.addNode(out2);
//            cell.addNode(out3);
//            cell.addEdge(edge1);
//            cell.addEdge(edge2);
//            cell.addEdge(edge3);
//            cell.addEdge(edge4);
//            cell.addEdge(edge5);
//            cell.addEdge(edge6);
//
//
//            ReplaceNode replaceNode = new ReplaceNode(nodeFactory, cellFactory, edgeFactory);
//            Cell cell2 = replaceNode.mutate(cell);
//
//            System.out.println("Program ending");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
