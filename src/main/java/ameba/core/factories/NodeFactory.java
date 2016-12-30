package ameba.core.factories;

import ameba.core.blocks.Node;
import ameba.core.blocks.nodes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 10/20/16.
 */
public class NodeFactory {
    /**
     * List of nodes settings
     */
    HashMap<String, NodeFactorySettings> settings;
    /**
     * General
     */
    Random rndGen = new Random();

    /**
     * Default constructor.
     */
    public NodeFactory() {
        settings = new HashMap<String, NodeFactorySettings>();
    }

    /**
     * Constructor with all nodes settings as map.
     *
     * @param nodeFactorySettingsHashMap Map of node's settings.
     */
    public NodeFactory(HashMap<String, NodeFactorySettings> nodeFactorySettingsHashMap) {
        settings = nodeFactorySettingsHashMap;
    }

    /**
     * @param settings Settings for specific node type.
     */
    public void addNodeSettings(NodeFactorySettings settings) {
        if (!this.settings.containsKey(settings.getNodeType())) {
            this.settings.put(settings.getNodeType(), settings);
        }

    }

    /**
     * Build node with randomly generated parameters with constraints derived from node factory settings.
     *
     * @param nodeType
     * @return
     */
    public Node genNode(String nodeType) {
        Node node = null;
        if (settings.containsKey(nodeType)) {
            switch (nodeType) {
                case "Add":
                    node = new Add(settings.get(nodeType).getMinInputEdges(), settings.get(nodeType).getMaxInputEdges(), settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges());
                    break;
                case "Constant":
                    node = new Constant(settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges(), genDecimalInitial(nodeType, 0));
                    break;
                case "Delay":
                    node = new Delay(settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges(), settings.get(nodeType).getInitialValue(), genIntegerInitial(nodeType, 0));
                    break;
                case "Derivative":
                    node = new Derivative(settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges(), settings.get(nodeType).getInitialValue(), genDecimalInitial(nodeType, 0));
                    break;
                case "Divide":
                    node = new Divide(settings.get(nodeType).getMinInputEdges(), settings.get(nodeType).getMaxInputEdges(), settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxInputEdges());
                    break;
                case "Input":
                    node = new Input(settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges());
                    break;
                case "Integral":
                    node = new Integral(settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges(), settings.get(nodeType).getInitialValue(), genDecimalInitial(nodeType, 0));
                    break;
                case "Multiply":
                    node = new Multiply(settings.get(nodeType).getMinInputEdges(), settings.get(nodeType).getMaxInputEdges(), settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges());
                    break;
                case "Output":
                    node = new Output(settings.get(nodeType).getMinOutputEdges(), settings.get(nodeType).getMaxOutputEdges());
                    break;
            }
        }
        return node;
    }

    /**
     * Build node of rndGen type excluded Input, Output node and nodes that aren't selected in settings.
     *
     * @return Node of randomly generated type.
     */
    public Node genNodeRnd() {
        ArrayList<String> nodes = new ArrayList<>();
        for (NodeFactorySettings settings : settings.values()) {
            if (settings.isAvailable()) {
                nodes.add(settings.getNodeType());
            }
        }
        return genNode(nodes.get(rndGen.nextInt(nodes.size())));
    }

    /**
     * Get list of allowed nodes.
     *
     * @return List of allowed nodes.
     */
    public ArrayList<String> getAllowedNodes() {
        ArrayList<String> nodes = new ArrayList<>();
        for (NodeFactorySettings settings : settings.values()) {
            if (settings.isAvailable()) {
                nodes.add(settings.getNodeType());
            }
        }
        return nodes;
    }

    /**
     * Get list of nodes that are allowed for usage and their maximum input edges is greater or equal to first input variable and their minimum output edges is greater or equal to second input variable.
     *
     * @param numInputs
     * @param numOutputs
     * @return
     */
    public ArrayList<String> getAllowedNodes(int numInputs, int numOutputs) {
        ArrayList<String> nodes = new ArrayList<>();
        for (NodeFactorySettings settings : settings.values()) {
            if (settings.isAvailable() && settings.getMaxInputEdges() >= numInputs && settings.getMaxOutputEdges() >= numOutputs) {
                nodes.add(settings.getNodeType());
            }
        }
        return nodes;
    }

    /**
     * Get list of nodes that are allowed for usage and their maximum input edges is greater or equal to first input variable and their minimum output edges is greater or equal to second input variable and is not of type of third input variable.
     *
     * @param numInputs
     * @param numOutputs
     * @param nodeType
     * @return
     */
    public ArrayList<String> getAllowedNodes(int numInputs, int numOutputs, String nodeType) {
        ArrayList<String> nodes = new ArrayList<>();
        for (NodeFactorySettings settings : settings.values()) {
            if (settings.isAvailable() &&
                    settings.getMaxInputEdges() >= numInputs &&
                    settings.getMaxOutputEdges() >= numOutputs &&
                    !settings.getNodeType().equals(nodeType)) {
                nodes.add(settings.getNodeType());
            }
        }
        return nodes;
    }

    /**
     * Generate rndGen decimal number with initial interval settings constrains.
     *
     * @param nodeType       Type of node from which constrains settings derive.
     * @param parameterIndex Index of the constrain interval.
     * @return Randomly generated decimal value.
     */
    private double genDecimalInitial(String nodeType, int parameterIndex) {
        double parameter = 0;
        if (settings.containsKey(nodeType)) {
            parameter = rndGen.nextDouble() *
                    (settings.get(nodeType).getInitialDecimalParametersLimits()[parameterIndex][1] - settings.get(nodeType).getInitialDecimalParametersLimits()[parameterIndex][0])
                    + settings.get(nodeType).getInitialDecimalParametersLimits()[parameterIndex][0];
        }
        return parameter;
    }

    /**
     * Generate rndGen integer number with initial interval settings constrains.
     *
     * @param nodeType       Type of node from which constrains settings derive.
     * @param parameterIndex Index of the constrain interval.
     * @return Randomly generated integer value.
     */
    private int genIntegerInitial(String nodeType, int parameterIndex) {
        int parameter = 0;
        if (settings.containsKey(nodeType)) {
            parameter = rndGen.nextInt(settings.get(nodeType).getInitialIntegerParametersLimits()[parameterIndex][1] - settings.get(nodeType).getInitialIntegerParametersLimits()[parameterIndex][0])
                    + settings.get(nodeType).getInitialIntegerParametersLimits()[parameterIndex][0];
        }
        return parameter;
    }
}
