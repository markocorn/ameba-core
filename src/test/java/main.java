import ameba.core.blocks.Cell;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core/src/main/resources/settings.json"));
            EdgeFactory edgeFactory = new EdgeFactory(new EdgeFactorySettings(jsonSettings.get("edgeFactorySettings").toString()));
            NodeFactory nodeFactory = new NodeFactory(NodeFactorySettings.genNodeFactorySettingsHashMap(jsonSettings.get("nodeFactorySettings").toString()));
            CellFactory factory = new CellFactory(new CellFactorySettings(jsonSettings.get("cellFactorySettings").toString()), nodeFactory, edgeFactory);
            Cell cell1 = factory.genCellRnd();
            System.out.println(CellFactory.verifyCellConnections(cell1));


            System.out.println("Program ending");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
