package ameba.core.factories;

import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.SerializationUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 10/20/16.
 */
public class FactoryNode implements Serializable {
    /**
     * List of nodes nodeSettingsHashMap
     */
    HashMap<String, FactoryNodeSettings> nodeSettingsHashMap;
    /**
     * General
     */
    Random rndGen;

    ArrayList<String> bag = new ArrayList<>();
    ArrayList<String> bagTargetCol = new ArrayList<>();
    ;
    ArrayList<String> bagSourceCol = new ArrayList<>();


    /**
     * Default constructor.
     */
    public FactoryNode(long seed) {
        rndGen = new Random(seed);
        nodeSettingsHashMap = new HashMap<>();
    }

    /**
     * Constructor with all nodes nodeSettingsHashMap as map.
     *
     * @param nodeFactorySettingsHashMap Map of node's nodeSettingsHashMap.
     */
    public FactoryNode(HashMap<String, FactoryNodeSettings> nodeFactorySettingsHashMap, long seed) {
        rndGen = new Random(seed);
        nodeSettingsHashMap = nodeFactorySettingsHashMap;
    }

    public static FactoryNode build(long seed) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryNode.class.getClassLoader().getResourceAsStream("core/nodeFactorySettings.json"));
        FactoryNode factoryNode = new FactoryNode(seed);
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        return factoryNode;
    }

    public static FactoryNode build(String filePath, long seed) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(filePath));
        FactoryNode factoryNode = new FactoryNode(seed);
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        return factoryNode;
    }

    /**
     * @param settings Settings for specific node type.
     */
    public void addNodeSettings(FactoryNodeSettings settings) {
        if (!this.nodeSettingsHashMap.containsKey(settings.getType())) {
            this.nodeSettingsHashMap.put(settings.getType(), settings);
        }

    }

    public FactoryNode clone() {
        return (FactoryNode) SerializationUtils.clone(this);
    }

    public void loadSettings(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        for (int i = 0; i < node.size(); i++) {
            addNodeSettings(FactoryNodeSettings.create(node.get(i).toString()));
        }
        bag.clear();
        bagTargetCol.clear();
        bagSourceCol.clear();

        for (FactoryNodeSettings settings : nodeSettingsHashMap.values()) {
            if (settings.getProbability() > 0) {
                for (int i = 0; i < settings.getProbability(); i++) {
                    bag.add(settings.getType());
                }
                Node node1 = genNode(settings.getType());

                if (node1.getCollectorsTarget().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagTargetCol.add(settings.getType());
                    }
                }
                if (node1.getCollectorsSource().size() > 0 && node1.getCollectorsTarget().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagSourceCol.add(settings.getType());
                    }
                }
            }
        }
    }

    public HashMap<String, FactoryNodeSettings> getNodeSettingsHashMap() {
        return nodeSettingsHashMap;
    }

    public void setNodeSettingsHashMap(HashMap<String, FactoryNodeSettings> nodeSettingsHashMap) {
        this.nodeSettingsHashMap = nodeSettingsHashMap;
    }

    /**
     * Build node with randomly generated parameters with constraints derived from node factory nodeSettingsHashMap.
     *
     * @param nodeType
     * @return
     */
    public Node genNode(String nodeType) throws Exception {
        Node node = null;
        if (nodeSettingsHashMap.containsKey(nodeType)) {
            switch (nodeType) {
                case "Input":
                    node = new Input();
                    break;
                case "Output":
                    node = new Output();
                    break;
                case "Add":
                    node = new Add(
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[0],
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[1]);
                    break;
                case "NeuronStep":
                    node = new NeuronStep(
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[0],
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[1]);
                    break;
                case "NeuronLin":
                    node = new NeuronLin(
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[0],
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[1],
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "Constant":
                    node = new Constant(
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "Delay":
                    node = new Delay(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            genRndSignalInt(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsInt()[0][1]),
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "Derivative":
                    node = new Derivative(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "FilterLP":
                    node = new FilterLP(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "FilterHP":
                    node = new FilterHP(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "Divide":
                    node = new Divide(
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[0],
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[1]);
                    break;
                case "ExponentBase":
                    node = new ExponentBase(
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "ExponentIndex":
                    node = new ExponentIndex(
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "Multiply":
                    node = new Multiply(
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[0],
                            nodeSettingsHashMap.get(nodeType).getCollectorsTargetLimit()[1]);
                    break;
                case "Integral":
                    node = new Integral(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            genRndSignalDec(nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersInitLimitsDec()[0][1]),
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
            }
        }
        return node;
    }

    public boolean isConstantNodeAvailable() {
        return nodeSettingsHashMap.get("Constant").getProbability() > 0;
    }

    public Node genConstantNode() throws Exception {
        return genNode("Constant");
    }

    /**
     * Build node of rndGen type excluded InputDec, OutputDec node and nodes that aren't selected in nodeSettingsHashMap.
     *
     * @return Node of randomly generated type.
     */
    public Node genNodeRnd() throws Exception {
        return genNode(bag.get(rndGen.nextInt(bag.size())));
    }

    public Node genNodeRndCollectorTarget() throws Exception {
        return genNode(bagTargetCol.get(rndGen.nextInt(bagTargetCol.size())));
    }

    public Node genNodeRndCollectorSource() throws Exception {
        return genNode(bagSourceCol.get(rndGen.nextInt(bagSourceCol.size())));
    }

    public double genRndSignalDec(double min, double max) throws Exception {
        if (min > max)
            throw new Exception("Min value is greater than max value.");
        if (min == max) return min;
        return rndGen.nextDouble() * (max - min) + min;
    }

    public int genRndSignalInt(int min, int max) throws Exception {
        if (min > max)
            throw new Exception("Min value is greater than max value.");
        if (min == max) return min;
        return rndGen.nextInt(max - min) + min;
    }

    public boolean genRndSignalBin(boolean min, boolean max) throws Exception {
        if (min) return true;
        if (!max) return false;
        return rndGen.nextBoolean();
    }

    public ArrayList<String> getBag() {
        return bag;
    }

    public void setBag(ArrayList<String> bag) {
        this.bag = bag;
    }

    public ArrayList<String> getBagTargetCol() {
        return bagTargetCol;
    }

    public void setBagTargetCol(ArrayList<String> bagTargetCol) {
        this.bagTargetCol = bagTargetCol;
    }

    public ArrayList<String> getBagSourceCol() {
        return bagSourceCol;
    }

    public void setBagSourceCol(ArrayList<String> bagSourceCol) {
        this.bagSourceCol = bagSourceCol;
    }
}
