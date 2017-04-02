import ameba.core.blocks.Cell;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Arrays;

/**
 * Created by marko on 10/24/16.
 */
public class rep3 {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core/src/main/resources/settings.json"));
        FactoryNode factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        FactoryEdge factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        FactoryCell factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);


        factoryCell.getCellFactorySettings().setNodeInpDec(1);
        factoryCell.getCellFactorySettings().setNodeInpInt(1);
        factoryCell.getCellFactorySettings().setNodeInpBin(1);
        factoryCell.getCellFactorySettings().setNodeOutDec(1);
        factoryCell.getCellFactorySettings().setNodeOutInt(1);
        factoryCell.getCellFactorySettings().setNodeOutBin(1);
        factoryCell.getCellFactorySettings().setNodeInitial(new Integer[]{2, 2});

//        TransferNodes transferNodes = new TransferNodes(factoryNode, factoryCell, factoryEdge, 5);

        double[][] inpDec = new double[][]{{0.0}, {1.0}, {2.0}, {3.0}, {4.0}, {5.0}, {6.0}, {7.0}, {8.0}, {9.0}};
        int[][] inpInt = new int[][]{{0}, {1}, {-1}, {2}, {-2}, {3}, {-3}, {4}, {-4}, {5}};
        boolean[][] inpBin = new boolean[][]{{false}, {false}, {true}, {true}, {true}, {false}, {false}, {true}, {true}, {true}};

        double[][] outDec = new double[10][1];
        int[][] outInt = new int[10][1];
        boolean[][] outBin = new boolean[10][1];

//        Cell cell = new Cell(10);
//        cell.addNode(new InputDec());
//        cell.addNode(new InputInt());
//        cell.addNode(new InputBin());
//        cell.addNode(new OutputDec());
//        cell.addNode(new OutputInt());
//        cell.addNode(new OutputBin());
//
//        cell.addNode(factoryNode.genNode("SwitchConstDec"));
//        cell.addNode(factoryNode.genNode("IntegralInt"));
//
//        cell.addEdge(new EdgeDec(cell.getNodes().get(0).getCollectorsSourceDec().get(0),cell.getNodes().get(6).getCollectorsTargetDec().get(0),1.0));
//        cell.addEdge(new EdgeDec(cell.getNodes().get(6).getCollectorsSourceDec().get(0),cell.getNodes().get(3).getCollectorsTargetDec().get(0),1.0));
//        cell.addEdge(new EdgeInt(cell.getNodes().get(1).getCollectorsSourceInt().get(0),cell.getNodes().get(7).getCollectorsTargetInt().get(0),1));
//        cell.addEdge(new EdgeInt(cell.getNodes().get(7).getCollectorsSourceInt().get(0),cell.getNodes().get(4).getCollectorsTargetInt().get(0),1));
//        cell.addEdge(new EdgeBin(cell.getNodes().get(1).getCollectorsSourceBin().get(0),cell.getNodes().get(7).getCollectorsTargetBin().get(0),false));

        try {
            for (int i = 0; i < 10000; i++) {
                System.out.println(i + " Run number");
                Cell cell = factoryCell.genCellRnd();
                try {
//                    cell = transferNodes.cross(cell, cell.clone());
//                    cell = removeNodesGroup.mutate(cell);
                    if (cell.checkCell().size() > 0) {
                        int t = 0;
                    }
                    cell.checkCellPrint();
                    for (int j = 0; j < inpDec.length; j++) {
//                        cell.runEvent(inpDec[j], inpInt[j], inpBin[j]);
                        outBin[j] = cell.getExportedValuesBin();
                        outInt[j] = cell.getExportedValuesInt();
                        outDec[j] = cell.getExportedValuesDec();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                for (int k = 0; k < outBin.length; k++) {
                    System.out.println(Arrays.toString(outDec[k]) + " : " + Arrays.toString(outInt[k]) + " : " + Arrays.toString(outBin[k]));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(i), Signal.createInteger(i - 5), Signal.createBoolean(true))));
//        }
//
//        try {
//            Cell cell = factoryCell.genCellRnd();
//            Cell cell1 = cell;
//            for (int i = 0; i < 100; i++) {
//                System.out.println(i + " Run number");
//
//                try {
////                    cell1 = replaceNode.mutate(cell);
////                    cell1 = addNodesGroup.mutate(factoryCell.genCellRnd());
//                    cell1 = addNodesGroup.mutate(cell1);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    cell1 = addNodesGroup.mutate(cell1);
//                }
//
//                ArrayList<String> test = cell1.checkCell();
//                System.out.println(test);
//                ArrayList<ArrayList<Signal>> outputs = new ArrayList<>();
//                try {
//                    outputs = cell1.run(inputs);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//                for (ArrayList<Signal> out : outputs) {
//                    for (Signal obj : out) {
//                        System.out.print(obj.getValue());
//                        System.out.print(":");
//                    }
//                    System.out.println();
//                }
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
