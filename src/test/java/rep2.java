import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

/**
 * Created by marko on 10/24/16.
 */
public class rep2 {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core/src/main/resources/settings.json"));
        FactoryNode factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        FactoryEdge factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        FactoryCell factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);

        FactoryReproduction factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell);
        factoryReproduction.loadSettings(jsonSettings.get("reproductionSettings").toString());

        for (int i = 0; i < 100000; i++) {
            factoryReproduction.repCell(factoryCell.genCellRnd(), factoryCell.genCellRnd());
            System.out.println(i + " : " + factoryReproduction.getRep());
        }
        int t = 0;

    }
}
