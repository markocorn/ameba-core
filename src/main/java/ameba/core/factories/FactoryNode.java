package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 10/20/16.
 */
public class FactoryNode {
    /**
     * List of nodes nodeSettingsHashMap
     */
    HashMap<String, FactoryNodeSettings> nodeSettingsHashMap;
    /**
     * General
     */
    Random rndGen = new Random();

    ArrayList<String> bag = new ArrayList<>();
    ArrayList<String> bagInpColDec = new ArrayList<>();
    ArrayList<String> bagInpColInt = new ArrayList<>();
    ArrayList<String> bagInpColBin = new ArrayList<>();
    ArrayList<String> bagOutColDec = new ArrayList<>();
    ArrayList<String> bagOutColInt = new ArrayList<>();
    ArrayList<String> bagOutColBin = new ArrayList<>();

    ArrayList<String> bagColBinBin = new ArrayList<>();
    ArrayList<String> bagColBinDec = new ArrayList<>();
    ArrayList<String> bagColBinInt = new ArrayList<>();
    ArrayList<String> bagColIntBin = new ArrayList<>();
    ArrayList<String> bagColIntDec = new ArrayList<>();
    ArrayList<String> bagColIntInt = new ArrayList<>();
    ArrayList<String> bagColDecBin = new ArrayList<>();
    ArrayList<String> bagColDecDec = new ArrayList<>();
    ArrayList<String> bagColDecInt = new ArrayList<>();


    /**
     * Default constructor.
     */
    public FactoryNode() {
        nodeSettingsHashMap = new HashMap<>();
    }

    /**
     * Constructor with all nodes nodeSettingsHashMap as map.
     *
     * @param nodeFactorySettingsHashMap Map of node's nodeSettingsHashMap.
     */
    public FactoryNode(HashMap<String, FactoryNodeSettings> nodeFactorySettingsHashMap) {
        nodeSettingsHashMap = nodeFactorySettingsHashMap;
    }

    public static FactoryNode build() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryNode.class.getClassLoader().getResourceAsStream("nodeFactorySettings.json"));
        FactoryNode factoryNode = new FactoryNode();
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

    public void loadSettings(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        for (int i = 0; i < node.size(); i++) {
            addNodeSettings(FactoryNodeSettings.create(node.get(i).toString()));
        }
        bag.clear();
        bagInpColDec.clear();
        bagInpColInt.clear();
        bagInpColBin.clear();
        bagOutColDec.clear();
        bagOutColInt.clear();
        bagOutColBin.clear();

        bagColBinBin.clear();
        bagColBinDec.clear();
        bagColBinInt.clear();
        bagColIntBin.clear();
        bagColIntDec.clear();
        bagColIntInt.clear();
        bagColDecBin.clear();
        bagColDecDec.clear();
        bagColDecInt.clear();

        for (FactoryNodeSettings settings : nodeSettingsHashMap.values()) {
            settings.setProbability(1);
            if (settings.getAvailable().equals(true)) {
                for (int i = 0; i < settings.getProbability(); i++) {
                    bag.add(settings.getType());
                }
                Node node1 = genNode(settings.getType());

                if (node1.getCollectorsTargetBin().size() > 0 && node1.getCollectorsSourceBin().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColBinBin.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetBin().size() > 0 && node1.getCollectorsSourceDec().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColBinDec.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetBin().size() > 0 && node1.getCollectorsSourceInt().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColBinInt.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetInt().size() > 0 && node1.getCollectorsSourceBin().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColIntBin.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetInt().size() > 0 && node1.getCollectorsSourceDec().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColIntDec.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetInt().size() > 0 && node1.getCollectorsSourceInt().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColIntInt.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetDec().size() > 0 && node1.getCollectorsSourceBin().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColDecBin.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetDec().size() > 0 && node1.getCollectorsSourceDec().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColDecDec.add(settings.getType());
                    }
                }
                if (node1.getCollectorsTargetDec().size() > 0 && node1.getCollectorsSourceInt().size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColDecInt.add(settings.getType());
                    }
                }

                bagInpColDec.addAll(bagColDecBin);
                bagInpColDec.addAll(bagColDecInt);
                bagInpColDec.addAll(bagColDecDec);

                bagInpColInt.addAll(bagColIntBin);
                bagInpColInt.addAll(bagColIntInt);
                bagInpColInt.addAll(bagColIntDec);

                bagInpColBin.addAll(bagColBinBin);
                bagInpColBin.addAll(bagColBinInt);
                bagInpColBin.addAll(bagColBinDec);

                bagOutColDec.addAll(bagColBinDec);
                bagOutColDec.addAll(bagColIntDec);
                bagOutColDec.addAll(bagColDecDec);

                bagOutColInt.addAll(bagColBinInt);
                bagOutColInt.addAll(bagColIntInt);
                bagOutColInt.addAll(bagColDecInt);

                bagOutColBin.addAll(bagColBinBin);
                bagOutColBin.addAll(bagColIntBin);
                bagOutColBin.addAll(bagColDecBin);
            }
        }
    }

    public HashMap<String, FactoryNodeSettings> getNodeSettingsHashMap() {
        return nodeSettingsHashMap;
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
                case "InputDec":
                    node = new InputDec();
                    break;

                case "InputInt":
                    node = new InputInt();
                    break;

                case "InputBin":
                    node = new InputBin();
                    break;

                case "OutputDec":
                    node = new OutputDec();
                    break;

                case "OutputInt":
                    node = new OutputInt();
                    break;

                case "OutputBin":
                    node = new OutputBin();
                    break;
                case "AddDec":
                    node = new AddDec(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[1]);
                    break;

                case "AddInt":
                    node = new AddInt(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitInt()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitInt()[1]);
                    break;
                case "AndBin":
                    node = new AndBin(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "OrBin":
                    node = new OrBin(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "XrrBin":
                    node = new XorBin(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "CompareDec":
                    node = new CompareDec(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareInt":
                    node = new CompareInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareConstDec":
                    node = new CompareConstDec(
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareConstInt":
                    node = new CompareConstInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[1],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[1][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[1][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0]);
                    break;
                case "ConstantDec":
                    node = new ConstantDec(
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "ConstantInt":
                    node = new ConstantInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "ConstantBin":
                    node = new ConstantBin(
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]});
                    break;
                case "DelayDec":
                    node = new DelayDec(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DelayInt":
                    node = new DelayInt(
                            nodeSettingsHashMap.get(nodeType).getInitialValueInt(),
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DelayBin":
                    node = new DelayBin(
                            nodeSettingsHashMap.get(nodeType).getInitialValueBin(),
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DerivativeDec":
                    node = new DerivativeDec(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "DerivativeInt":
                    node = new DerivativeInt(
                            nodeSettingsHashMap.get(nodeType).getInitialValueInt(),
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DerivativeBin":
                    node = new DerivativeBin(
                            nodeSettingsHashMap.get(nodeType).getInitialValueBin(),
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]});
                    break;
                case "Divide":
                    node = new Divide(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "MultiplyDec":
                    node = new MultiplyDec(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "MultiplyInt":
                    node = new MultiplyInt(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitInt()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitInt()[1]);
                    break;
                case "MuxDec":
                    node = new MuxDec(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitDec()[1],
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "MuxInt":
                    node = new MuxInt(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitInt()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitInt()[1],
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "MuxBin":
                    node = new MuxBin(
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[0],
                            nodeSettingsHashMap.get(nodeType).getInpColLimitBin()[1],
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]});
                    break;
                case "DMuxDec":
                    node = new DMuxDec(
                            nodeSettingsHashMap.get(nodeType).getOutColLimitDec()[0],
                            nodeSettingsHashMap.get(nodeType).getOutColLimitDec()[1],
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "DMuxInt":
                    node = new DMuxInt(
                            nodeSettingsHashMap.get(nodeType).getOutColLimitInt()[0],
                            nodeSettingsHashMap.get(nodeType).getOutColLimitInt()[1],
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DMuxBin":
                    node = new DMuxBin(
                            nodeSettingsHashMap.get(nodeType).getOutColLimitBin()[0],
                            nodeSettingsHashMap.get(nodeType).getOutColLimitBin()[1],
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]});
                    break;
                case "IntegralDec":
                    node = new IntegralDec(
                            nodeSettingsHashMap.get(nodeType).getInitialValueDec(),
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "IntegralInt":
                    node = new IntegralInt(
                            nodeSettingsHashMap.get(nodeType).getInitialValueInt(),
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "IntegralBin":
                    node = new IntegralBin(
                            nodeSettingsHashMap.get(nodeType).getInitialValueBin(),
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]});
                    break;
                case "IntervalDec":
                    node = new IntervalDec();
                    break;
                case "IntervalInt":
                    node = new IntervalInt();
                    break;
                case "IntervalConstDec":
                    node = new IntervalConstDec(
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "IntervalConstInt":
                    node = new IntervalConstInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "Interval2ConstDec":
                    node = new Interval2ConstDec(
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[1],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[1][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[1][1]});
                    break;
                case "Interval2ConstInt":
                    node = new Interval2ConstInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[1],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[1][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[1][1]});
                    break;
                case "SwitchDec":
                    node = new SwitchDec();
                    break;
                case "SwitchInt":
                    node = new SwitchInt();
                    break;
                case "SwitchBin":
                    node = new SwitchBin();
                    break;
                case "SwitchConstDec":
                    node = new SwitchConstDec(
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]});
                    break;
                case "SwitchConstInt":
                    node = new SwitchConstInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "SwitchConstBin":
                    node = new SwitchConstBin(
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]});
                    break;
                case "Switch2ConstDec":
                    node = new Switch2ConstDec(
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[0],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[0][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersDec()[1],
                            new Double[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[1][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsDec()[1][1]});
                    break;
                case "Switch2ConstInt":
                    node = new Switch2ConstInt(
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[0],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[0][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersInt()[1],
                            new Integer[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[1][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsInt()[1][1]});
                    break;
                case "Switch2ConstBin":
                    node = new Switch2ConstBin(
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[0],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[0][1]},
                            nodeSettingsHashMap.get(nodeType).getParametersBin()[1],
                            new Boolean[]{nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[1][0], nodeSettingsHashMap.get(nodeType).getParametersLimitsBin()[1][1]});
                    break;

            }
        }
        return node;
    }

    public Node genNodeRndPar(String nodeType) throws Exception {
        Node node = genNode(nodeType);
        //Randomize parameters Dec
        for (int i = 0; i < node.getParamsDec().size(); i++) {
            node.getParamsDec().set(i, genRndSignalDec(node.getParamsLimitsDec().get(i)[0], node.getParamsLimitsDec().get(i)[1]));
        }
        //Randomize parameters Int
        for (int i = 0; i < node.getParamsInt().size(); i++) {
            node.getParamsInt().set(i, genRndSignalInt(node.getParamsLimitsInt().get(i)[0], node.getParamsLimitsInt().get(i)[1]));
        }
        //Randomize parameters Bin
        for (int i = 0; i < node.getParamsBin().size(); i++) {
            node.getParamsBin().set(i, genRndSignalBin(node.getParamsLimitsBin().get(i)[0], node.getParamsLimitsBin().get(i)[1]));
        }
        node.clearNode();
        return node;
    }

    public boolean isConstantNodeAvailable(Cell.Signal type) {
        switch (type) {
            case DECIMAL:
                return nodeSettingsHashMap.get("ConstantDec").getAvailable();
            case INTEGER:
                return nodeSettingsHashMap.get("ConstantInt").getAvailable();
            case BOOLEAN:
                return nodeSettingsHashMap.get("ConstantBin").getAvailable();
            default:
                return false;
        }
    }

    public Node genConstantNode(Cell.Signal type) throws Exception {
        switch (type) {
            case DECIMAL:
                return genNodeRndPar("ConstantDec");
            case INTEGER:
                return genNodeRndPar("ConstantInt");
            case BOOLEAN:
                return genNodeRndPar("ConstantBin");
            default:
                return null;
        }
    }

    /**
     * Build node of rndGen type excluded InputDec, OutputDec node and nodes that aren't selected in nodeSettingsHashMap.
     *
     * @return Node of randomly generated type.
     */
    public Node genNodeRnd() throws Exception {
        return genNodeRndPar(bag.get(rndGen.nextInt(bag.size())));
    }

    public Node genNodeRndCollectorTargetType(Cell.Signal type) throws Exception {
        if (type.equals(Cell.Signal.DECIMAL)) {
            return genNodeRndPar(bagInpColDec.get(rndGen.nextInt(bagInpColDec.size())));
        }
        if (type.equals(Cell.Signal.INTEGER)) {
            return genNodeRndPar(bagInpColInt.get(rndGen.nextInt(bagInpColInt.size())));
        }
        if (type.equals(Cell.Signal.BOOLEAN)) {
            return genNodeRndPar(bagInpColBin.get(rndGen.nextInt(bagInpColBin.size())));
        }
        throw new Exception();
    }

    public Node genNodeRndCollectorSourceType(Cell.Signal type) throws Exception {
        if (type.equals(Cell.Signal.DECIMAL)) {
            return genNodeRndPar(bagOutColDec.get(rndGen.nextInt(bagOutColDec.size())));
        }
        if (type.equals(Cell.Signal.INTEGER)) {
            return genNodeRndPar(bagOutColInt.get(rndGen.nextInt(bagOutColInt.size())));
        }
        if (type.equals(Cell.Signal.BOOLEAN)) {
            return genNodeRndPar(bagOutColBin.get(rndGen.nextInt(bagOutColBin.size())));
        }
        throw new Exception();
    }

    public Node genNodeRndCollectorsType(Cell.Signal typeInp, Cell.Signal typeOut) throws Exception {
        if (typeInp.equals(Cell.Signal.BOOLEAN) && typeOut.equals(Cell.Signal.BOOLEAN)) {
            return genNodeRndPar(bagColBinBin.get(rndGen.nextInt(bagColBinBin.size())));
        }
        if (typeInp.equals(Cell.Signal.BOOLEAN) && typeOut.equals(Cell.Signal.INTEGER)) {
            return genNodeRndPar(bagColBinInt.get(rndGen.nextInt(bagColBinInt.size())));
        }
        if (typeInp.equals(Cell.Signal.BOOLEAN) && typeOut.equals(Cell.Signal.DECIMAL)) {
            return genNodeRndPar(bagColBinDec.get(rndGen.nextInt(bagColBinDec.size())));
        }

        if (typeInp.equals(Cell.Signal.INTEGER) && typeOut.equals(Cell.Signal.BOOLEAN)) {
            return genNodeRndPar(bagColIntBin.get(rndGen.nextInt(bagColIntBin.size())));
        }
        if (typeInp.equals(Cell.Signal.INTEGER) && typeOut.equals(Cell.Signal.INTEGER)) {
            return genNodeRndPar(bagColIntInt.get(rndGen.nextInt(bagColIntInt.size())));
        }
        if (typeInp.equals(Cell.Signal.INTEGER) && typeOut.equals(Cell.Signal.DECIMAL)) {
            return genNodeRndPar(bagColIntDec.get(rndGen.nextInt(bagColIntDec.size())));
        }

        if (typeInp.equals(Cell.Signal.DECIMAL) && typeOut.equals(Cell.Signal.BOOLEAN)) {
            return genNodeRndPar(bagColDecBin.get(rndGen.nextInt(bagColDecBin.size())));
        }
        if (typeInp.equals(Cell.Signal.DECIMAL) && typeOut.equals(Cell.Signal.INTEGER)) {
            return genNodeRndPar(bagColDecInt.get(rndGen.nextInt(bagColDecInt.size())));
        }
        if (typeInp.equals(Cell.Signal.DECIMAL) && typeOut.equals(Cell.Signal.DECIMAL)) {
            return genNodeRndPar(bagColDecDec.get(rndGen.nextInt(bagColDecDec.size())));
        }
        throw new Exception("Type does not exists.");
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
}
