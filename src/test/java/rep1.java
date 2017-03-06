import ameba.core.blocks.Cell;
import ameba.core.blocks.Signal;
import ameba.core.factories.*;
import ameba.core.reproductions.mutateCell.*;
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

        ReplaceNode replaceNode = new ReplaceNode(factoryNode, factoryCell, factoryEdge);
        AddNode1 addNode1 = new AddNode1(factoryNode, factoryCell);
        AddNode2 addNode2 = new AddNode2(factoryNode, factoryCell);
        AddNode3 addNode3 = new AddNode3(factoryNode, factoryCell, factoryEdge);
        RemoveNode removeNode = new RemoveNode(factoryCell);
        RemoveNode1 removeNode1 = new RemoveNode1(factoryCell);
        SwitchEdges1 switchEdges1 = new SwitchEdges1();
        SwitchEdges2 switchEdges2 = new SwitchEdges2();
        RemoveNodesGroup removeNodesGroup = new RemoveNodesGroup(factoryCell, 5);
        AddNodesGroup addNodesGroup = new AddNodesGroup(factoryNode, factoryCell, factoryEdge, 2);

        ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(i), Signal.createInteger(i - 5), Signal.createBoolean(true))));
        }

        try {
            Cell cell = factoryCell.genCellRnd();
            Cell cell1 = cell;
            for (int i = 0; i < 1000; i++) {
                System.out.println(i + " Run number");

                try {
//                    cell1 = replaceNode.mutate(cell);
                    cell1 = addNodesGroup.mutate(factoryCell.genCellRnd());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    cell1 = addNodesGroup.mutate(cell1);
                }

//                String test = cell1.checkCell();
//                System.out.println(test);
//                if (!test.equals("")) {
//                    int t = 0;
//                }
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
