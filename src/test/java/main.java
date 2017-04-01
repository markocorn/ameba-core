import ameba.core.blocks.Cell;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Arrays;


/**
 * Created by marko on 10/24/16.
 */
public class main {
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
        factoryCell.getCellFactorySettings().setNodeInitial(new Integer[]{5, 5});

        double[][] inpDec = new double[][]{{0.0}, {1.0}, {2.0}, {3.0}, {4.0}, {5.0}, {6.0}, {7.0}, {8.0}, {9.0}};
        int[][] inpInt = new int[][]{{0}, {1}, {-1}, {2}, {-2}, {3}, {-3}, {4}, {-4}, {5}};
        boolean[][] inpBin = new boolean[][]{{false}, {false}, {true}, {true}, {true}, {false}, {false}, {true}, {true}, {true}};

        double[][] outDec = new double[10][1];
        int[][] outInt = new int[10][1];
        boolean[][] outBin = new boolean[10][1];

        try {
            for (int i = 0; i < 10000; i++) {
                System.out.println(i + " Run number");
                Cell cell = factoryCell.genCellRnd();
                try {
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
    }
}
