import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.AddDec;
import ameba.core.blocks.nodes.types.DelayDec;
import ameba.core.blocks.nodes.types.InputDec;
import ameba.core.blocks.nodes.types.OutputDec;
import ameba.core.factories.FactoryCell;

/**
 * Created by marko on 4/10/17.
 */
public class Main {
    public static void main(String[] args) {
        try {
            FactoryCell factoryCell = FactoryCell.build();
//            FactoryReproduction factoryReproduction = FactoryReproduction.build();
//            for (int i = 0; i < 1000; i++) {
//                Cell cell = factoryCell.genCellRnd();
//                System.out.print(cell.getNodes().size() + " ");
//                Cell cell1 = factoryReproduction.repCell(cell, factoryCell.genCellRnd());
//                System.out.println(cell1.getNodes().size() + " " + cell1.lastRep);
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

//            Cell cell = new Cell(100);
//            Node inp1 = new InputDec();
//            Node inp2 = new InputDec();
//            Node out1 = new OutputDec();
//            Node out2 = new OutputDec();
//            Node node1 = new NeuronStep(2, 10);
//            Node node2 = new IntegralDec(0.0, 1.0, new Double[]{-10.0, 10.0});
//            Edge edge1 = new EdgeDec(inp1.getCollectorsSourceDec().get(0), node1.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge2 = new EdgeDec(node1.getCollectorsSourceDec().get(0), out1.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge3 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node2.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge4 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node1.getCollectorsTargetDec().get(1), 1.0);
//
//            Edge edge5 = new EdgeInt(inp2.getCollectorsSourceInt().get(0), out2.getCollectorsTargetInt().get(0), 1);
////            Edge edge6 = new EdgeBin(inp3.getCollectorsSourceBin().get(0), out3.getCollectorsTargetBin().get(0), false);
//
//
//            cell.addNode(inp1);
//            cell.addNode(inp2);
////            cell.addNode(inp3);
//            cell.addNode(out1);
//            cell.addNode(out2);
////            cell.addNode(out3);
//            cell.addNode(node1);
//            cell.addNode(node2);
//
//            cell.addEdge(edge1);
//            cell.addEdge(edge2);
//            cell.addEdge(edge3);
//            cell.addEdge(edge4);
//            cell.addEdge(edge5);
////            cell.addEdge(edge6);
//            cell.clearCell();
//
//            cell.checkCellPrint();

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
//            edge3.setLockTarget(true);
//            System.out.println(cell.getInnerNodes());
//
//            for (int i = 0; i < 1000; i++) {
//                cell = factoryReproduction.repCell(cell, factoryCell.genCellRnd());
//                System.out.println(cell.getEdges().get(2).getTarget().getNodeAttached());
////                System.out.println(((EdgeDec)cell.getEdges().get(0)).getWeight());
////                System.out.println(((EdgeInt)cell.getEdges().get(4)).getWeight());
////                System.out.println(((EdgeBin)cell.getEdges().get(5)).getWeight());
//            }
            //* Multivariable PI controller
            //*/
//            Cell cell = new Cell(100);
//            Node node1 = new InputDec();
//            Node node2 = new InputDec();
//            Node node3 = new OutputDec();
//            Node node4 = new OutputDec();
//            Node node5 = new AddDec(2, 10);
//            Node node6 = new AddDec(2, 10);
//            Node node7 = new IntegralDec(0.0,1.0,new Double[]{-10.0, 10.0});
//            Node node8 = new IntegralDec(0.0,1.0,new Double[]{-10.0, 10.0});
//            Node node9 = new IntegralDec(0.0,1.0,new Double[]{-10.0, 10.0});
//            Node node10 = new IntegralDec(0.0,1.0,new Double[]{-10.0, 10.0});
//
//            Edge edge1 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node5.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge2 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node7.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge3 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node8.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge4 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node6.getCollectorsTargetDec().get(1), 1.0);
//
//            Edge edge5 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node9.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge6 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node5.getCollectorsTargetDec().get(2), 1.0);
//            Edge edge7 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node10.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge8 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node6.getCollectorsTargetDec().get(3), 1.0);
//
//            Edge edge9 = new EdgeDec(node7.getCollectorsSourceDec().get(0), node5.getCollectorsTargetDec().get(1), 1.0);
//            Edge edge10 = new EdgeDec(node8.getCollectorsSourceDec().get(0), node6.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge11 = new EdgeDec(node9.getCollectorsSourceDec().get(0), node5.getCollectorsTargetDec().get(3), 1.0);
//            Edge edge12 = new EdgeDec(node10.getCollectorsSourceDec().get(0), node6.getCollectorsTargetDec().get(2), 1.0);
//
//            Edge edge13 = new EdgeDec(node5.getCollectorsSourceDec().get(0), node3.getCollectorsTargetDec().get(0), 1.0);
//            Edge edge14 = new EdgeDec(node6.getCollectorsSourceDec().get(0), node4.getCollectorsTargetDec().get(0), 1.0);
//
//            cell.addNode(node1);
//            cell.addNode(node2);
//            cell.addNode(node3);
//            cell.addNode(node4);
//            cell.addNode(node5);
//            cell.addNode(node6);
//            cell.addNode(node7);
//            cell.addNode(node8);
//            cell.addNode(node9);
//            cell.addNode(node10);
//
//            cell.addEdge(edge1);
//            cell.addEdge(edge2);
//            cell.addEdge(edge3);
//            cell.addEdge(edge4);
//            cell.addEdge(edge5);
//            cell.addEdge(edge6);
//            cell.addEdge(edge7);
//            cell.addEdge(edge8);
//            cell.addEdge(edge9);
//            cell.addEdge(edge10);
//            cell.addEdge(edge11);
//            cell.addEdge(edge12);
//            cell.addEdge(edge13);
//            cell.addEdge(edge14);
//
            Cell cell = new Cell(100);
            Node node1 = new InputDec();
            Node node2 = new InputDec();
            Node node3 = new OutputDec();
            Node node4 = new OutputDec();
            Node node5 = new AddDec(2, 10);
            Node node6 = new AddDec(2, 10);
            Node node7 = new DelayDec(0.0, 1, new Integer[]{0, 5});
            Node node8 = new DelayDec(0.0, 1, new Integer[]{0, 5});

            Edge edge1 = new EdgeDec(node1.getCollectorsSourceDec().get(0), node5.getCollectorsTargetDec().get(0), 1.0);
            Edge edge2 = new EdgeDec(node5.getCollectorsSourceDec().get(0), node3.getCollectorsTargetDec().get(0), 1.0);
            Edge edge3 = new EdgeDec(node2.getCollectorsSourceDec().get(0), node6.getCollectorsTargetDec().get(0), 1.0);
            Edge edge4 = new EdgeDec(node6.getCollectorsSourceDec().get(0), node4.getCollectorsTargetDec().get(0), 1.0);

            Edge edge5 = new EdgeDec(node5.getCollectorsSourceDec().get(0), node7.getCollectorsTargetDec().get(0), 1.0);
            Edge edge6 = new EdgeDec(node7.getCollectorsSourceDec().get(0), node5.getCollectorsTargetDec().get(1), 1.0);
            Edge edge7 = new EdgeDec(node6.getCollectorsSourceDec().get(0), node8.getCollectorsTargetDec().get(0), 1.0);


            cell.addNode(node1);
            cell.addNode(node2);
            cell.addNode(node3);
            cell.addNode(node4);
            cell.addNode(node5);
            cell.addNode(node6);
            cell.addNode(node7);
            cell.addNode(node8);


            cell.addEdge(edge1);
            cell.addEdge(edge2);
            cell.addEdge(edge3);
            cell.addEdge(edge4);
            cell.addEdge(edge5);
            cell.addEdge(edge6);
            cell.addEdge(edge7);


            cell.clearCell();
            cell.checkCellPrint();

//            System.out.println(cell.toJsonString());

            //Simulate cell
            double[][] inpDec = new double[][]{{1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}};

            for (int i = 0; i < inpDec.length; i++) {
                cell.importSignals(inpDec[i]);
                cell.runEvent();
                System.out.println(String.valueOf(cell.getExportedValuesDec()[0]) + " : " + String.valueOf(cell.getExportedValuesDec()[1]));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
