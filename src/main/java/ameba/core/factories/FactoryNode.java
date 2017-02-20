package ameba.core.factories;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
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
        for (FactoryNodeSettings settings : settings.values()) {
            settings.setProbability(1);
            if (settings.getAvailable().equals(true)) {
                for (int i = 0; i < settings.getProbability(); i++) {
                    bag.add(settings.getType());
                }
                Node node1 = genNode(settings.getType());
                boolean[] included = new boolean[]{false, false, false};
                for (CollectorInp collectorInp : node1.getInpCollectors()) {
                    if (collectorInp.getType().isAssignableFrom(Double.class) && !included[0]) {
                        included[0] = true;
                        for (int i = 0; i < settings.getProbability(); i++) {
                            bagInpColDec.add(settings.getType());
                        }
                    }
                    if (collectorInp.getType().isAssignableFrom(Integer.class) && !included[1]) {
                        included[1] = true;
                        for (int i = 0; i < settings.getProbability(); i++) {
                            bagInpColInt.add(settings.getType());
                        }
                    }
                    if (collectorInp.getType().isAssignableFrom(Boolean.class) && !included[2]) {
                        included[2] = true;
                        for (int i = 0; i < settings.getProbability(); i++) {
                            bagInpColBin.add(settings.getType());
                        }

                    }
                }
                included = new boolean[]{false, false, false};
                for (CollectorOut collectorOut : node1.getOutCollectors()) {
                    if (collectorOut.getType().isAssignableFrom(Double.class) && !included[0]) {
                        included[0] = true;
                        for (int i = 0; i < settings.getProbability(); i++) {
                            bagOutColDec.add(settings.getType());
                        }
                    }
                    if (collectorOut.getType().isAssignableFrom(Integer.class) && !included[1]) {
                        included[1] = true;
                        for (int i = 0; i < settings.getProbability(); i++) {
                            bagOutColInt.add(settings.getType());
                        }
                    }
                    if (collectorOut.getType().isAssignableFrom(Boolean.class) && !included[2]) {
                        included[2] = true;
                        for (int i = 0; i < settings.getProbability(); i++) {
                            bagOutColBin.add(settings.getType());
                        }
                    }
                }
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
                    node = new Input(
                            Signal.createDouble());
                    break;
                case "InputInt":
                    node = new Input(
                            Signal.createInteger());
                    break;
                case "InputBin":
                    node = new Input(
                            Signal.createBoolean());
                    break;
                case "OutputDec":
                    node = new Output(
                            Signal.createDouble());
                    break;
                case "OutputInt":
                    node = new Output(
                            Signal.createInteger());
                    break;
                case "OutputBin":
                    node = new Output(
                            Signal.createBoolean());
                    break;
                case "AddDec":
                    node = new Add(
                            Signal.createDouble(),
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "AddInt":
                    node = new Add(
                            Signal.createInteger(),
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1]);
                    break;
                case "AndBin":
                    node = new AndBin(
                            settings.get(nodeType).getInpColLimitBin()[0],
                            settings.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "OrBin":
                    node = new AndBin(
                            settings.get(nodeType).getInpColLimitBin()[0],
                            settings.get(nodeType).getInpColLimitBin()[1]);
                    break;
                case "CompareDec":
                    node = new Compare(
                            Signal.createDouble(),
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareInt":
                    node = new Compare(
                            Signal.createInteger(),
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareConstDec":
                    node = new CompareConst(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]),
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "CompareConstInt":
                    node = new CompareConst(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[1]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[1][0], settings.get(nodeType).getParametersLimitsInt()[1][1]),
                            settings.get(nodeType).getParametersInt()[0]);
                    break;
                case "ConstantDec":
                    node = new Constant(
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "ConstantInt":
                    node = new Constant(
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "ConstantBin":
                    node = new Constant(
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "DelayDec":
                    node = new Delay(
                            Signal.createDouble(settings.get(nodeType).getInitialValueDec()),
                            settings.get(nodeType).getParametersInt()[0],
                            new int[]{settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DelayInt":
                    node = new Delay(
                            Signal.createInteger(settings.get(nodeType).getInitialValueInt()),
                            settings.get(nodeType).getParametersInt()[0],
                            new int[]{settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DelayBin":
                    node = new Delay(
                            Signal.createBoolean(settings.get(nodeType).getInitialValueBin()),
                            settings.get(nodeType).getParametersInt()[0],
                            new int[]{settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]});
                    break;
                case "DerivativeDec":
                    node = new Derivative(
                            Signal.createDouble(settings.get(nodeType).getInitialValueDec()),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "DerivativeInt":
                    node = new Derivative(
                            Signal.createInteger(settings.get(nodeType).getInitialValueInt()),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "DerivativeBin":
                    node = new Derivative(
                            Signal.createBoolean(settings.get(nodeType).getInitialValueBin()),
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "Divide":
                    node = new Divide(
                            Signal.createDouble(),
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "MultiplyDec":
                    node = new Multiply(
                            Signal.createDouble(),
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1]);
                    break;
                case "MultiplyInt":
                    node = new Multiply(
                            Signal.createInteger(),
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1]);
                    break;
                case "MuxDec":
                    node = new Mux(
                            settings.get(nodeType).getInpColLimitDec()[0],
                            settings.get(nodeType).getInpColLimitDec()[1],
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "MuxInt":
                    node = new Mux(
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1],
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "MuxBin":
                    node = new Mux(
                            settings.get(nodeType).getInpColLimitInt()[0],
                            settings.get(nodeType).getInpColLimitInt()[1],
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "DMuxDec":
                    node = new DMux(
                            settings.get(nodeType).getOutColLimitDec()[0],
                            settings.get(nodeType).getOutColLimitDec()[1],
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "DMuxInt":
                    node = new DMux(
                            settings.get(nodeType).getOutColLimitInt()[0],
                            settings.get(nodeType).getOutColLimitInt()[1],
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "DMuxBin":
                    node = new DMux(
                            settings.get(nodeType).getOutColLimitBin()[0],
                            settings.get(nodeType).getOutColLimitBin()[1],
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "IntegralDec":
                    node = new Integral(
                            Signal.createDouble(settings.get(nodeType).getInitialValueDec()),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "IntegralInt":
                    node = new Integral(
                            Signal.createInteger(settings.get(nodeType).getInitialValueInt()),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "IntegralBin":
                    node = new Integral(
                            Signal.createBoolean(settings.get(nodeType).getInitialValueBin()),
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "IntervalDec":
                    node = new Interval(
                            Signal.createDouble());
                    break;
                case "IntervalInt":
                    node = new Interval(
                            Signal.createInteger());
                    break;
                case "IntervalConstDec":
                    node = new IntervalConst(
                            Signal.createDouble(),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "IntervalConstInt":
                    node = new IntervalConst(
                            Signal.createInteger(),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "Interval2ConstDec":
                    node = new Interval2Const(
                            Signal.createDouble(),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[1]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[1][0], settings.get(nodeType).getParametersLimitsDec()[1][1]));
                    break;
                case "Interval2ConstInt":
                    node = new Interval2Const(
                            Signal.createInteger(),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[1]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[1][0], settings.get(nodeType).getParametersLimitsInt()[1][1]));
                    break;
                case "SwitchDec":
                    node = new Switch(
                            Signal.createDouble());
                    break;
                case "SwitchInt":
                    node = new Switch(
                            Signal.createInteger());
                    break;
                case "SwitchBin":
                    node = new Switch(
                            Signal.createBoolean());
                    break;
                case "SwitchConstDec":
                    node = new SwitchConst(
                            Signal.createDouble(),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]));
                    break;
                case "SwitchConstInt":
                    node = new SwitchConst(
                            Signal.createInteger(),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]));
                    break;
                case "SwitchConstBin":
                    node = new SwitchConst(
                            Signal.createBoolean(),
                            Signal.createBoolean(settings.get(nodeType).getParametersBin()[0]),
                            Signal.createBoolean(settings.get(nodeType).getParametersLimitsBin()[0][0], settings.get(nodeType).getParametersLimitsBin()[0][1]));
                    break;
                case "Switch2ConstDec":
                    node = new Switch2Const(
                            Signal.createDouble(),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[0]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[0][0], settings.get(nodeType).getParametersLimitsDec()[0][1]),
                            Signal.createDouble(settings.get(nodeType).getParametersDec()[1]),
                            Signal.createDouble(settings.get(nodeType).getParametersLimitsDec()[1][0], settings.get(nodeType).getParametersLimitsDec()[1][1]));
                    break;
                case "Switch2ConstInt":
                    node = new Switch2Const(
                            Signal.createInteger(),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[0]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[0][0], settings.get(nodeType).getParametersLimitsInt()[0][1]),
                            Signal.createInteger(settings.get(nodeType).getParametersInt()[1]),
                            Signal.createInteger(settings.get(nodeType).getParametersLimitsInt()[1][0], settings.get(nodeType).getParametersLimitsInt()[1][1]));
                    break;
                case "Switch2ConstBin":
                    node = new Switch2Const(
                            Signal.createBoolean(),
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
