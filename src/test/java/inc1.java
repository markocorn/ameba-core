import ameba.core.evolution.Incubator;
import ameba.core.evolution.IncubatorSettings;
import ameba.core.evolution.fitness.FitnessAbsolute;
import ameba.core.evolution.selections.BestOf;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

/**
 * Created by marko on 10/24/16.
 */
public class inc1 {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File("./src/main/resources/settings.json"));
        FactoryNode factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        FactoryEdge factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        FactoryCell factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);

        FactoryReproduction factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell);
        factoryReproduction.loadSettings(jsonSettings.get("reproductionSettings").toString());

        //Prepare data
        JsonNode dataJson = mapper.readTree(new File("./src/main/resources/data.json"));

        double[][] inpDec = new double[dataJson.get("inpDec").size()][dataJson.get("inpDec").get(0).size()];
        double[][] outDec = new double[dataJson.get("outDec").size()][dataJson.get("outDec").get(0).size()];
        for (int i = 0; i < inpDec.length; i++) {
            for (int j = 0; j < inpDec[i].length; j++) {
                inpDec[i][j] = dataJson.get("inpDec").get(i).get(j).asDouble();
            }
            for (int j = 0; j < outDec[i].length; j++) {
                outDec[i][j] = dataJson.get("outDec").get(i).get(j).asDouble();
            }
        }

        IncubatorSettings incubatorSettings = mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class);

        Incubator incubator = new Incubator(factoryCell, factoryReproduction, incubatorSettings, new BestOf(), new FitnessAbsolute(outDec, null, null, 10.0, 10.0, 10.0));


        incubator.importData(inpDec);


        for (int i = 0; i < 1000; i++) {
            incubator.simPopulation();
            incubator.reproduce();
            System.out.println(i + ": fitness: " + incubator.getPopulation().get(0).getFitnessValue());
            System.out.println("cell size: " + incubator.getPopulation().get(0).getInnerNodes().size());
        }

        int t = 0;
    }
}
