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
        JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core/src/main/resources/settings.json"));
        FactoryNode factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        FactoryEdge factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        FactoryCell factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);

        FactoryReproduction factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell);
        factoryReproduction.loadSettings(jsonSettings.get("reproductionSettings").toString());

        IncubatorSettings incubatorSettings = mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class);
        Incubator incubator = new Incubator(factoryCell, incubatorSettings, new BestOf(), new FitnessAbsolute());

        incubator.populateInitial();
        double[][] inpDec = new double[][]{{0.0}, {1.0}, {2.0}, {3.0}, {4.0}, {5.0}, {6.0}, {7.0}, {8.0}, {9.0}};
        int[][] inpInt = new int[][]{{0}, {1}, {-1}, {2}, {-2}, {3}, {-3}, {4}, {-4}, {5}};
        boolean[][] inpBin = new boolean[][]{{false}, {false}, {true}, {true}, {true}, {false}, {false}, {true}, {true}, {true}};

        incubator.importData(inpDec);
        incubator.importData(inpInt);
        incubator.importData(inpBin);

        incubator.checkDataMatch();

        incubator.simPopulation();
        int t = 0;


    }
}
