import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.InputDec;
import ameba.core.blocks.nodes.types.IntegralDec;
import ameba.core.blocks.nodes.types.NeuronStep;
import ameba.core.blocks.nodes.types.OutputDec;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryReproduction;

/**
 * Created by marko on 4/10/17.
 */
public class Main {
    public static void main(String[] args) {
        try {
            FactoryCell factoryCell = FactoryCell.build();
            FactoryReproduction factoryReproduction = FactoryReproduction.build();
//            for (int i = 0; i < 1000; i++) {
//                factoryReproduction.repCell(factoryCell.genCellRnd(), factoryCell.genCellRnd());
//                System.out.println(i);
//
//            }
//            for (int i = 0; i < 1000; i++) {
//                factoryCell.getCellJson(factoryCell.genCellRnd().toJsonString());
//                System.out.println(factoryCell.genCellRnd().toJsonString());
//            }

//            Cell cell = new Cell(100);
//            Node inp1 = new InputDec();
//            Node inp2 = new InputInt();
//            Node inp3 = new InputBin();
//            Node out1 = new OutputDec();
//            Node out2 = new OutputInt();
//            Node out3 = new OutputBin();
//            Node node1 = new NeuronStep(2, 10);
//            Node node2 = new IntegralDec(0.0, 1.0, new Double[]{-10.0, 10.0});
//            Edge edge1 = new EdgeDec(inp1.getCollectorsSourceDec().get(0), node1.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge2 = new EdgeDec(node1.getCollectorsSourceDec().get(0), out1.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge3 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node2.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge4 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node1.getCollectorsTargetDec().get(1), 1.0);
//
//            Edge edge5 = new EdgeInt(inp2.getCollectorsSourceInt().get(0), out2.getCollectorsTargetInt().get(0), 1);
//            Edge edge6 = new EdgeBin(inp3.getCollectorsSourceBin().get(0), out3.getCollectorsTargetBin().get(0), false);
//
//
//            cell.addNode(inp1);
//            cell.addNode(inp2);
//            cell.addNode(inp3);
//            cell.addNode(out1);
//            cell.addNode(out2);
//            cell.addNode(out3);
//            cell.addNode(node1);
//            cell.addNode(node2);
//
//            cell.addEdge(edge1);
//            cell.addEdge(edge2);
//            cell.addEdge(edge3);
//            cell.addEdge(edge4);
//            cell.addEdge(edge5);
//            cell.addEdge(edge6);
//            cell.clearCell();
//
//            cell.checkCellPrint();

            Cell cell = new Cell(100);
            Node inp1 = new InputDec();
            Node inp2 = new InputDec();
            Node out1 = new OutputDec();
            Node out2 = new OutputDec();
            Node node1 = new NeuronStep(2, 10);
            Node node2 = new IntegralDec(0.0, 1.0, new Double[]{-10.0, 10.0});
            Edge edge1 = new EdgeDec(inp1.getCollectorsSourceDec().get(0), node1.getCollectorsTargetDec().get(0), 1.0);
            Edge edge2 = new EdgeDec(node1.getCollectorsSourceDec().get(0), out1.getCollectorsTargetDec().get(0), 1.0);
            Edge edge3 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node2.getCollectorsTargetDec().get(0), 1.0);
            Edge edge4 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node1.getCollectorsTargetDec().get(1), 1.0);

            Edge edge5 = new EdgeInt(inp2.getCollectorsSourceInt().get(0), out2.getCollectorsTargetInt().get(0), 1);
//            Edge edge6 = new EdgeBin(inp3.getCollectorsSourceBin().get(0), out3.getCollectorsTargetBin().get(0), false);


            cell.addNode(inp1);
            cell.addNode(inp2);
//            cell.addNode(inp3);
            cell.addNode(out1);
            cell.addNode(out2);
//            cell.addNode(out3);
            cell.addNode(node1);
            cell.addNode(node2);

            cell.addEdge(edge1);
            cell.addEdge(edge2);
            cell.addEdge(edge3);
            cell.addEdge(edge4);
            cell.addEdge(edge5);
//            cell.addEdge(edge6);
            cell.clearCell();

            cell.checkCellPrint();

//            //Simulate cell
//            double[][] inpDec = new double[][]{{1.0}, {1.0}, {1.0}, {1.0}};
//            int[][] inpInt = new int[][]{{1}, {1}, {1}, {1}};
//            boolean[][] inpBin = new boolean[][]{{true}, {true}, {true}, {true}};
//
//            for (int i = 0; i < inpDec.length; i++) {
//                cell.importSignals(inpDec[i], inpInt[i], inpBin[i]);
//                cell.runEvent();
//                System.out.println(cell.getExportedValuesDec()[0] + " : " + cell.getExportedValuesInt()[0] + " : " + cell.getExportedValuesBin()[0]);
//            }
//            edge1.setLockWeight(true);
//            edge5.setLockWeight(true);
//            edge6.setLockWeight(true);
//            node2.setLockDec(0,true);
//            edge3.setLockSource(true);
            edge3.setLockTarget(true);
            System.out.println(cell.getInnerNodes());

            for (int i = 0; i < 1000; i++) {
                cell = factoryReproduction.repCell(cell, factoryCell.genCellRnd());
                System.out.println(cell.getEdges().get(2).getTarget().getNodeAttached());
//                System.out.println(((EdgeDec)cell.getEdges().get(0)).getWeight());
//                System.out.println(((EdgeInt)cell.getEdges().get(4)).getWeight());
//                System.out.println(((EdgeBin)cell.getEdges().get(5)).getWeight());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
