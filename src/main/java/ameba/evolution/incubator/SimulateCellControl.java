package ameba.evolution.incubator;

import ameba.core.blocks.Cell;
import ameba.evolution.blocks.StateModel;
import ameba.evolution.fitness.IFitness;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SimulateCellControl extends SimulateCell {
    StateModel model;
    double weightError;
    double weightActuator;
    double weightErrorDiff;
    double weightActuatorDiff;

    public SimulateCellControl(IFitness fitness, DataEvo data, Cell cell, int prefCellSize, double weightDown, double weightUp, double weightEdges, StateModel model, double weightError, double weightActuator, double weightErrorDiff, double weightActuatorDiff) {
        super(fitness, data, cell, prefCellSize, weightDown, weightUp, weightEdges);
        this.model = model;
        this.weightError = weightError;
        this.weightActuator = weightActuator;
        this.weightErrorDiff = weightErrorDiff;
        this.weightActuatorDiff = weightActuatorDiff;
    }

    @Override
    public void simulate() {
        cell.clearCell();
        cell.setFitnessValue(0.0);
        model.rst();

        double[] yd;
        double[] ydOld = new double[model.getC().length];
        double[] ud;
        double[] udOld = new double[model.getB().length];

        for (int i = 0; i < data.getInputs().length; i++) {
            cell.importSignals(StateModel.subArrays(data.getInputs()[i], model.getOutputs(data.getInputs()[i])));
            cell.addFitnessValue(weightError * fitness.clcFitness(data.getInputs()[i], model.getOutputs(data.getInputs()[i])));

            yd = model.getOutputs(data.getInputs()[i]);
            cell.addFitnessValue(weightErrorDiff * diff(yd, ydOld));
            ydOld = yd;

            try {
                cell.runEvent();
                //Limit actuator signals
                double[] u = cell.getExportedValues();
                for (int j = 0; j < u.length; j++) {
                    if (u[j] < model.getLimits()[j][0]) {
                        u[j] = model.getLimits()[j][0];
                    }
                    if (u[j] > model.getLimits()[j][1]) {
                        u[j] = model.getLimits()[j][1];
                    }
                }
                model.stepOde1(u);
                for (int j = 0; j < cell.getExportedValues().length; j++) {
                    cell.addFitnessValue(weightActuator * Math.pow(cell.getExportedValues()[j], 2));
                }

                ud = cell.getExportedValues();
                cell.addFitnessValue(weightActuatorDiff * diff(ud, udOld));
                udOld = ud;
            } catch (Exception ex) {
                ex.printStackTrace();
                cell.checkCellPrint();
                cell.runEvent();
            }
        }

        double fit;
        int diff = cell.getInnerNodes().size() - prefCellSize;
        if (diff > 0) {
            fit = diff * weightUp;
        } else {
            fit = -diff * weightDown;
        }
        cell.addFitnessValue(fit);
        cell.addFitnessValue(cell.getEdges().size() * weightEdges);
    }

    public double diff(double[] inp1, double[] inp2) {
        double o = 0.0;
        for (int i = 0; i < inp1.length; i++) {
            o += Math.pow(inp1[i] - inp2[i], 2);
        }
        return o;
    }

    public String simulateToJsonString() {
        cell.clearCell();
        cell.setFitnessValue(0.0);
        model.rst();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode references = mapper.createArrayNode();
        ArrayNode outputs = mapper.createArrayNode();
        ArrayNode actuator = mapper.createArrayNode();
        ArrayNode errors = mapper.createArrayNode();

        for (int i = 0; i < data.getInputs().length; i++) {
            cell.importSignals(StateModel.subArrays(data.getInputs()[i], model.getOutputs(data.getInputs()[i])));
            cell.addFitnessValue(fitness.clcFitness(data.getInputs()[i], model.getOutputs(data.getInputs()[i])));
            try {
                cell.runEvent();
                //Limit actuator signals
                double[] y = cell.getExportedValues();
                for (int j = 0; j < y.length; j++) {
                    if (y[j] < model.getLimits()[j][0]) {
                        y[j] = model.getLimits()[j][0];
                    }
                    if (y[j] > model.getLimits()[j][1]) {
                        y[j] = model.getLimits()[j][1];
                    }
                }
                model.stepOde1(y);
                model.stepOde1(cell.getExportedValues());
                ArrayNode row = mapper.createArrayNode();
                for (int j = 0; j < data.getInputs()[i].length; j++) {
                    row.add(data.getInputs()[i][j]);
                }
                references.add(row);
                row = mapper.createArrayNode();
                for (int j = 0; j < data.getInputs()[i].length; j++) {
                    row.add(data.getInputs()[i][j] - model.getOutputs(data.getInputs()[i])[j]);
                }
                errors.add(row);
                row = mapper.createArrayNode();
                for (int j = 0; j < data.getInputs()[i].length; j++) {
                    row.add(model.getOutputs(data.getInputs()[i])[j]);
                }
                outputs.add(row);
                row = mapper.createArrayNode();
                for (int j = 0; j < data.getInputs()[i].length; j++) {
                    row.add(cell.getExportedValues()[j]);
                }
                actuator.add(row);
            } catch (Exception ex) {
                ex.printStackTrace();
                cell.checkCellPrint();
                cell.runEvent();
            }
        }
        ObjectNode out = mapper.createObjectNode();
        out.put("references", references);
        out.put("errors", errors);
        out.put("actuators", actuator);
        out.put("outputs", outputs);

        return out.toString();
    }
}
