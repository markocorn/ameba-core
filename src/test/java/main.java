import ameba.core.blocks.Cell;
import ameba.core.blocks.conectivity.Signal;
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
        factoryCell.getCellFactorySettings().setNodeInitial(new Integer[]{1, 1});

        ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0))));
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0))));
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0))));
        inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0))));

        try {
            Cell cell = factoryCell.genCellRnd();

            ArrayList<ArrayList<Signal>> outputs = cell.run(inputs);

            for (ArrayList<Signal> out : outputs) {
                for (Signal obj : out) {
                    System.out.print(obj.getValue());
                    System.out.print(":");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("Program ending");
        } catch (Exception e) {
            e.printStackTrace();
        }


//    }


//            FactoryEdge edgeFactory = new FactoryEdge(new FactoryEdgeSettings(jsonSettings.get("edgeFactorySettings").toString()));
//            FactoryNode factoryNode = new FactoryNode(NodeFactorySettings.genNodeFactorySettingsHashMap(jsonSettings.get("nodeFactorySettings").toString()));
//            FactoryCell cellFactory = new FactoryCell(new FactoryCellSettings(jsonSettings.get("cellFactorySettings").toString()), factoryNode, edgeFactory);
//            Cell cell1 = cellFactory.genCellRnd();

    }
}
