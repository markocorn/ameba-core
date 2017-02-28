import ameba.core.blocks.Cell;
import ameba.core.blocks.Signal;
import ameba.core.factories.*;
import ameba.core.reproductions.mutateCell.AddNode1;
import ameba.core.reproductions.mutateCell.AddNode2;
import ameba.core.reproductions.mutateCell.AddNode3;
import ameba.core.reproductions.mutateCell.ReplaceNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by marko on 10/24/16.
 */
public class rep1 {
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
        factoryCell.getCellFactorySettings().setNodeInitial(new Integer[]{1, 1});

        ReplaceNode replaceNode = new ReplaceNode(factoryNode, factoryCell, factoryEdge);
        AddNode1 addNode1 = new AddNode1(factoryNode, factoryCell, factoryEdge);
        AddNode2 addNode2 = new AddNode2(factoryNode, factoryCell, factoryEdge);
        AddNode3 addNode3 = new AddNode3(factoryNode, factoryCell, factoryEdge);

        ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(i), Signal.createInteger(i - 5), Signal.createBoolean(true))));
        }

        try {
            Cell cell = factoryCell.genCellRnd();
            Cell cell1 = cell;
            for (int i = 0; i < 50; i++) {
                System.out.println(i + " Run number");

                try {
//                    cell1 = replaceNode.mutate(cell);
                    cell1 = addNode3.mutate(cell1);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    cell1 = addNode3.mutate(cell1);
                }

                String test = FactoryCell.checkCell(cell1);
                System.out.println(test);
                if (!test.equals("")) {
                    int t = 0;
                }
                ArrayList<ArrayList<Signal>> outputs = new ArrayList<>();
                try {
                    outputs = cell1.run(inputs);
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
