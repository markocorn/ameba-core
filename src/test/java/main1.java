import ameba.core.blocks.Cell;
import ameba.core.blocks.Signal;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by marko on 10/24/16.
 */
public class main1 {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core (copy)/src/main/resources/settings.json"));
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
        factoryCell.getCellFactorySettings().setNodeInitial(new Integer[]{1, 100});


        Random random = new Random();

        ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(random.nextDouble() * 20.0 - 10.0), Signal.createInteger(random.nextInt(20) - 10), Signal.createBoolean(random.nextBoolean()))));
        }

        try {
            for (int i = 0; i < 1000; i++) {
                System.out.println(i + " Run number");
                Cell cell = factoryCell.genCellRnd();
//                String test = cell.checkCell();
//                System.out.println(test);
//                if (!test.equals("")) {
//                    int t = 0;
//                }
                ArrayList<ArrayList<Signal>> outputs = new ArrayList<>();
                try {
                    outputs = cell.run(inputs);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    outputs = cell.run(inputs);
                }
                for (ArrayList<Signal> out : outputs) {
                    for (Signal obj : out) {
                        try {
                            System.out.print(obj.getValue());
                            System.out.print(":");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            outputs = cell.run(inputs);
                        }
                    }
                    System.out.println();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
