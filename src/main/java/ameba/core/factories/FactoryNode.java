package ameba.core.factories;

import ameba.core.blocks.Signal;
import ameba.core.blocks.nodes.*;
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
     * List of nodes settings
     */
    HashMap<String, FactoryNodeSettings> settings;
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
        settings = new HashMap<String, FactoryNodeSettings>();
    }

    /**
     * Constructor with all nodes settings as map.
     *
     * @param nodeFactorySettingsHashMap Map of node's settings.
     */
    public FactoryNode(HashMap<String, FactoryNodeSettings> nodeFactorySettingsHashMap) {
        settings = nodeFactorySettingsHashMap;
    }

    /**
     * @param settings Settings for specific node type.
     */
    public void addNodeSettings(FactoryNodeSettings settings) {
        if (!this.settings.containsKey(settings.getType())) {
            this.settings.put(settings.getType(), settings);
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

        for (FactoryNodeSettings settings : settings.values()) {
            settings.setProbability(1);
            if (settings.getAvailable().equals(true)) {
                for (int i = 0; i < settings.getProbability(); i++) {
                    bag.add(settings.getType());
                }
                Node node1 = genNode(settings.getType());

                if (node1.getInpCollectors(Boolean.class).size() > 0 && node1.getOutCollectors(Boolean.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColBinBin.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Boolean.class).size() > 0 && node1.getOutCollectors(Double.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColBinDec.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Boolean.class).size() > 0 && node1.getOutCollectors(Integer.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColBinInt.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Integer.class).size() > 0 && node1.getOutCollectors(Boolean.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColIntBin.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Integer.class).size() > 0 && node1.getOutCollectors(Double.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColIntDec.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Integer.class).size() > 0 && node1.getOutCollectors(Integer.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColIntInt.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Double.class).size() > 0 && node1.getOutCollectors(Boolean.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColDecBin.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Double.class).size() > 0 && node1.getOutCollectors(Double.class).size() > 0) {
                    for (int i = 0; i < settings.getProbability(); i++) {
                        bagColDecDec.add(settings.getType());
                    }
                }
                if (node1.getInpCollectors(Double.class).size() > 0 && node1.getOutCollectors(Integer.class).size() > 0) {
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

    public HashMap<String, FactoryNodeSettings> getSettings() {
        return settings;
    }

    /**
     * Build node with randomly generated parameters with constraints derived from node factory settings.
     *
     * @param nodeType
     * @return
     */
    public Node genNode(String nodeType) throws Exception {
        Node node = null;
        if (settings.containsKey(nodeType)) {
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
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1]);
                    break;

                case "AddInt":
                    node = new AddInt(
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1]);
                    break;
                case "AndBin":
                    node = new AndBin(
                            settings.get(nodeType).getInpColLimitBin()[0],
                            settings.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "OrBin":
                    node = new OrBin(
                            settings.get(nodeType).getInpColLimitBin()[0],
                            settings.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "XrrBin":
                    node = new XorBin(
                            settings.get(nodeType).getInpColLimitBin()[0],
                            settings.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "CompareDec":
                    node = new CompareDec(
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareInt":
                    node = new CompareInt(
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareConstDec":
                    node = new CompareConstDec(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]),
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareConstInt":
                    node = new CompareConstInt(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[1]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[1][0], settings.get(nodeType).getParametersLimitsInt()[1][1]),
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "ConstantDec":
                    node = new ConstantDec(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "ConstantInt":
                    node = new ConstantInt(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "ConstantBin":
                    node = new ConstantBin(
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "DelayDec":
                    node = new DelayDec(
                            Signal.createDouble(settings.get(nodeType).getInitialValueDec()),
                            settings.get(nodeType).getParametersInt()[0],
                            new int[]{settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DelayInt":
                    node = new DelayInt(
                            Signal.createInteger(settings.get(nodeType).getInitialValueInt()),
                            settings.get(nodeType).getParametersInt()[0],
                            new int[]{settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DelayBin":
                    node = new DelayBin(
                            Signal.createBoolean(settings.get(nodeType).getInitialValueBin()),
                            settings.get(nodeType).getParametersInt()[0],
                            new int[]{settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DerivativeDec":
                    node = new DerivativeDec(
                            Signal.createDouble(settings.get(nodeType).getInitialValueDec()),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "DerivativeInt":
                    node = new DerivativeInt(
                            Signal.createInteger(settings.get(nodeType).getInitialValueInt()),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "DerivativeBin":
                    node = new DerivativeBin(
                            Signal.createBoolean(settings.get(nodeType).getInitialValueBin()),
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "Divide":
                    node = new Divide(
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "MultiplyDec":
                    node = new MultiplyDec(
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "MultiplyInt":
                    node = new MultiplyInt(
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1]);
                    break;
                case "MuxDec":
                    node = new MuxDec(
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1],
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "MuxInt":
                    node = new MuxInt(
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1],
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "MuxBin":
                    node = new MuxBin(
                            settings.get(nodeType).getInpColLimitBin()[0],
                            settings.get(nodeType).getInpColLimitBin()[1],
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "DMuxDec":
                    node = new DMuxDec(
                            settings.get(nodeType).getOutColLimitDec()[0],
                            settings.get(nodeType).getOutColLimitDec()[1],
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "DMuxInt":
                    node = new DMuxInt(
                            settings.get(nodeType).getOutColLimitInt()[0],
                            settings.get(nodeType).getOutColLimitInt()[1],
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "DMuxBin":
                    node = new DMuxBin(
                            settings.get(nodeType).getOutColLimitBin()[0],
                            settings.get(nodeType).getOutColLimitBin()[1],
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "IntegralDec":
                    node = new IntegralDec(
                            Signal.createDouble(settings.get(nodeType).getInitialValueDec()),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "IntegralInt":
                    node = new IntegralInt(
                            Signal.createInteger(settings.get(nodeType).getInitialValueInt()),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "IntegralBin":
                    node = new IntegralBin(
                            Signal.createBoolean(settings.get(nodeType).getInitialValueBin()),
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "IntervalDec":
                    node = new IntervalDec();
                    break;
                case "IntervalInt":
                    node = new IntervalInt();
                    break;
                case "IntervalConstDec":
                    node = new IntervalConstDec(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "IntervalConstInt":
                    node = new IntervalConstInt(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "Interval2ConstDec":
                    node = new Interval2ConstDec(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[1]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[1][0], settings.get(nodeType).getParametersLimitsDec()[1][1]));
                    break;
                case "Interval2ConstInt":
                    node = new Interval2ConstInt(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[1]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[1][0], settings.get(nodeType).getParametersLimitsInt()[1][1]));
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
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "SwitchConstInt":
                    node = new SwitchConstInt(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "SwitchConstBin":
                    node = new SwitchConstBin(
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "Switch2ConstDec":
                    node = new Switch2ConstDec(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[1]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[1][0], settings.get(nodeType).getParametersLimitsDec()[1][1]));
                    break;
                case "Switch2ConstInt":
                    node = new Switch2ConstInt(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[1]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[1][0], settings.get(nodeType).getParametersLimitsInt()[1][1]));
                    break;
                case "Switch2ConstBin":
                    node = new Switch2ConstBin(
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]),
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[1]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[1][0], settings.get(nodeType).getParametersLimitsBin()[1][1]));
                    break;

            }
        }
        return node;
    }

    public Node genNodeRndPar(String nodeType) throws Exception {
        Node node = genNode(nodeType);
        //Randomize parameters
        for (int i = 0; i < node.getParams().size(); i++) {
            node.getParams().set(i, genRndSignal(node.getParamsLimits().get(i)[0], node.getParamsLimits().get(i)[1]));
        }
        node.clearNode();
        return node;
    }

    public boolean isConstantAvaliable(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return settings.get("ConstantDec").getAvailable();
        if (type.isAssignableFrom(Integer.class)) return settings.get("ConstantInt").getAvailable();
        if (type.isAssignableFrom(Boolean.class)) return settings.get("ConstantBin").getAvailable();
        throw new Exception("Unknown type of Constant node: " + type.getSimpleName());
    }

    public Node genConstant(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) {
            Node node = genNode("ConstantDec");
            //Randomize parameters
            for (int i = 0; i < node.getParams().size(); i++) {
                node.getParams().set(i, genRndSignal(node.getParamsLimits().get(i)[0], node.getParamsLimits().get(i)[1]));
            }
            node.clearNode();
            return node;
        }
        if (type.isAssignableFrom(Integer.class)) {
            Node node = genNode("ConstantInt");
            //Randomize parameters
            for (int i = 0; i < node.getParams().size(); i++) {
                node.getParams().set(i, genRndSignal(node.getParamsLimits().get(i)[0], node.getParamsLimits().get(i)[1]));
            }
            node.clearNode();
            return node;
        }
        if (type.isAssignableFrom(Boolean.class)) {
            Node node = genNode("ConstantBin");
            //Randomize parameters
            for (int i = 0; i < node.getParams().size(); i++) {
                node.getParams().set(i, genRndSignal(node.getParamsLimits().get(i)[0], node.getParamsLimits().get(i)[1]));
            }
            node.clearNode();
            return node;
        }
        throw new Exception("Unknown type of Constant node: " + type.getSimpleName());
    }

    /**
     * Build node of rndGen type excluded InputDec, OutputDec node and nodes that aren't selected in settings.
     *
     * @return Node of randomly generated type.
     */
    public Node genNodeRnd() throws Exception {
        return genNodeRndPar(bag.get(rndGen.nextInt(bag.size())));
    }

    public Node genNodeRndInpColType(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) {
            return genNodeRndPar(bagInpColDec.get(rndGen.nextInt(bagInpColDec.size())));
        }
        if (type.isAssignableFrom(Integer.class)) {
            return genNodeRndPar(bagInpColInt.get(rndGen.nextInt(bagInpColInt.size())));
        }
        if (type.isAssignableFrom(Boolean.class)) {
            return genNodeRndPar(bagInpColBin.get(rndGen.nextInt(bagInpColBin.size())));
        }
        throw new Exception();
    }

    public Node genNodeRndOutColType(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) {
            return genNodeRndPar(bagOutColDec.get(rndGen.nextInt(bagOutColDec.size())));
        }
        if (type.isAssignableFrom(Integer.class)) {
            return genNodeRndPar(bagOutColInt.get(rndGen.nextInt(bagOutColInt.size())));
        }
        if (type.isAssignableFrom(Boolean.class)) {
            return genNodeRndPar(bagOutColBin.get(rndGen.nextInt(bagOutColBin.size())));
        }
        throw new Exception();
    }

    public Node genNodeRndColType(Class typeInp, Class typeOut) throws Exception {
        if (typeInp.isAssignableFrom(Boolean.class) && typeOut.isAssignableFrom(Boolean.class)) {
            return genNodeRndPar(bagColBinBin.get(rndGen.nextInt(bagColBinBin.size())));
        }
        if (typeInp.isAssignableFrom(Boolean.class) && typeOut.isAssignableFrom(Integer.class)) {
            return genNodeRndPar(bagColBinInt.get(rndGen.nextInt(bagColBinInt.size())));
        }
        if (typeInp.isAssignableFrom(Boolean.class) && typeOut.isAssignableFrom(Double.class)) {
            return genNodeRndPar(bagColBinDec.get(rndGen.nextInt(bagColBinDec.size())));
        }

        if (typeInp.isAssignableFrom(Integer.class) && typeOut.isAssignableFrom(Boolean.class)) {
            return genNodeRndPar(bagColIntBin.get(rndGen.nextInt(bagColIntBin.size())));
        }
        if (typeInp.isAssignableFrom(Integer.class) && typeOut.isAssignableFrom(Integer.class)) {
            return genNodeRndPar(bagColIntInt.get(rndGen.nextInt(bagColIntInt.size())));
        }
        if (typeInp.isAssignableFrom(Integer.class) && typeOut.isAssignableFrom(Double.class)) {
            return genNodeRndPar(bagColIntDec.get(rndGen.nextInt(bagColIntDec.size())));
        }

        if (typeInp.isAssignableFrom(Double.class) && typeOut.isAssignableFrom(Boolean.class)) {
            return genNodeRndPar(bagColDecBin.get(rndGen.nextInt(bagColDecBin.size())));
        }
        if (typeInp.isAssignableFrom(Double.class) && typeOut.isAssignableFrom(Integer.class)) {
            return genNodeRndPar(bagColDecInt.get(rndGen.nextInt(bagColDecInt.size())));
        }
        if (typeInp.isAssignableFrom(Double.class) && typeOut.isAssignableFrom(Double.class)) {
            return genNodeRndPar(bagColDecDec.get(rndGen.nextInt(bagColDecDec.size())));
        }
        throw new Exception("Type does not exists.");
    }


    public Signal genRndSignal(Signal min, Signal max) throws Exception {
        if (min.gettClass().isAssignableFrom(Double.class) && max.gettClass().isAssignableFrom(Double.class)) {
            if (min.getValueDouble() > max.getValueDouble())
                throw new Exception("Min value is greater than max value.");
            if (min.getValueDouble().equals(max.getValueDouble())) return Signal.createDouble(min.getValueDouble());
            return Signal.createDouble(rndGen.nextDouble() * (max.getValueDouble() - min.getValueDouble()) + min.getValueDouble());
        }
        if (min.gettClass().isAssignableFrom(Integer.class) && max.gettClass().isAssignableFrom(Integer.class)) {
            if (min.getValueInteger() > max.getValueInteger())
                throw new Exception("Min value is greater than max value.");
            if (min.getValueInteger().equals(max.getValueInteger())) return Signal.createInteger(min.getValueInteger());
            return Signal.createInteger(rndGen.nextInt(max.getValueInteger() - min.getValueInteger()) + min.getValueInteger());
        }
        if (min.gettClass().isAssignableFrom(Boolean.class) && max.gettClass().isAssignableFrom(Boolean.class)) {
            if (min.equals(true)) return Signal.createBoolean(true);
            if (max.equals(false)) return Signal.createBoolean(false);
            return Signal.createBoolean(rndGen.nextBoolean());
        }
        throw new Exception("Random number cant be generated for type:" + min.gettClass().getSimpleName() + " and type: " + max.gettClass().getSimpleName());
    }
}
