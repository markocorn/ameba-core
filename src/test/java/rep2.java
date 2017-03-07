import ameba.core.factories.*;
import ameba.core.reproductions.parametersOperations.RepParSettings;
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

        RepParSettings repParSettings = new RepParSettings(new Double[]{-0.01, 0.01}, new Integer[]{-1, 1}, new Boolean[]{false, true},
                new Double[]{-1000.0, 1000.0}, new Integer[]{-1000, 1000}, new Boolean[]{false, true}, 100.0);
        repParSettings.getStringJson();

    }
}
