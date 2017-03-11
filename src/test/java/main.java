import ameba.core.blocks.Cell;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core (copy)/src/main/resources/settings.json"));
        FactoryNode factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        FactoryEdge factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        FactoryCell factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);

        factoryCell.getCellFactorySettings().setNodeInpDec(1);
        factoryCell.getCellFactorySettings().setNodeInpInt(0);
        factoryCell.getCellFactorySettings().setNodeInpBin(0);
        factoryCell.getCellFactorySettings().setNodeOutDec(1);
        factoryCell.getCellFactorySettings().setNodeOutInt(0);
        factoryCell.getCellFactorySettings().setNodeOutBin(0);
        factoryCell.getCellFactorySettings().setNodeInitial(new Integer[]{3, 3});

        ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0))));
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0))));
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0))));
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0))));

        try {
            for (int i = 0; i < 10000; i++) {
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
                }
                for (ArrayList<Signal> out : outputs) {
                    for (Signal obj : out) {
                        System.out.print(obj.getValue());
                        System.out.print(":");
                    }
                    System.out.println();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
