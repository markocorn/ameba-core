package ameba.core.factories;

import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.crossCell.ICrossCell;
import ameba.core.reproductions.crossEdge.*;
import ameba.core.reproductions.crossNode.ICrossNode;
import ameba.core.reproductions.mutateCell.IMutateCell;
import ameba.core.reproductions.mutateEdge.*;
import ameba.core.reproductions.mutateNode.IMutateNode;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;
import ameba.core.reproductions.parametersOperations.genParCrossover.*;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueDec;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignDec;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueDec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by marko on 3/7/17.
 */
public class FactoryReproduction {
    ArrayList<Reproduction> reproductions;
    HashMap<String, IMutateEdgeDec> mutateEdges;
    HashMap<String, IMutateNode> mutateNodes;
    HashMap<String, IMutateCell> mutateCells;
    HashMap<String, ICrossEdgeDec> crossEdges;
    HashMap<String, ICrossNode> crossNodes;
    HashMap<String, ICrossCell> crossCells;

    ArrayList<String> bagMutateEdge;
    ArrayList<String> bagMutateNode;
    ArrayList<String> bagMutateCell;
    ArrayList<String> bagCrossEdge;
    ArrayList<String> bagCrossNode;
    ArrayList<String> bagCrossCell;

    public FactoryReproduction() {
        reproductions = new ArrayList<>();
        mutateEdges = new HashMap<>();
        mutateNodes = new HashMap<>();
        mutateCells = new HashMap<>();
        crossEdges = new HashMap<>();
        crossNodes = new HashMap<>();
        crossCells = new HashMap<>();

        this.bagMutateEdge = new ArrayList();
        this.bagMutateNode = new ArrayList();
        this.bagMutateCell = new ArrayList();
        this.bagCrossEdge = new ArrayList();
        this.bagCrossNode = new ArrayList();
        this.bagCrossCell = new ArrayList();
    }

    public void loadSettings(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        loadEdgeMutations(node);


    }

    private void loadEdgeMutations(JsonNode node) throws IOException {
        String name = "weightAddValueDecDec";
        WeightAddValueDec weightAddValueDecDec = new WeightAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightAddValueDecDec);
        reproductions.add(weightAddValueDecDec);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDecDec.getProbability(), name));

        name = "weightAddValueDecInt";
        WeightAddValueDec weightAddValueDecInt = new WeightAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightAddValueDecInt", weightAddValueDecInt);
        reproductions.add(weightAddValueDecInt);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDecInt.getProbability(), name));

        name = "weightAddValueDecBin";
        WeightAddValueDec weightAddValueDecBin = new WeightAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightAddValueDecBin", weightAddValueDecBin);
        reproductions.add(weightAddValueDecBin);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDecBin.getProbability(), name));

        name = "weightInverseDec";
        WeightInverse weightInverseDec = new WeightInverse(new InverseValue(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightInverseDec", weightInverseDec);
        reproductions.add(weightInverseDec);
        bagMutateEdge.addAll(Collections.nCopies(weightInverseDec.getProbability(), name));

        name = "weightMixSignDec";
        WeightMixSignDec weightMixSignDec = new WeightMixSignDec(new MixSignDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightMixSignDec", weightMixSignDec);
        reproductions.add(weightMixSignDec);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignDec.getProbability(), name));

        name = "weightMixSignInt";
        WeightMixSignDec weightMixSignInt = new WeightMixSignDec(new MixSignDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightMixSignInt", weightMixSignInt);
        reproductions.add(weightMixSignInt);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignInt.getProbability(), name));

        name = "weightRandValueDec";
        WeightRandValueDec weightRandValueDec = new WeightRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightRandValueDec", weightRandValueDec);
        reproductions.add(weightRandValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueDec.getProbability(), name));

        name = "weightRandValueInt";
        WeightRandValueDec weightRandValueInt = new WeightRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightRandValueInt", weightRandValueInt);
        reproductions.add(weightRandValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueInt.getProbability(), name));

        name = "weightRandValueBin";
        WeightRandValueDec weightRandValueBin = new WeightRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put("weightRandValueBin", weightRandValueBin);
        reproductions.add(weightRandValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDecBin.getProbability(), name));
    }

    private void loadEdgeCross(JsonNode node) throws IOException {
        String name = "weightCombineAddDec";
        WeightCombineAddDec cross = new WeightCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineAddInt";
        cross = new WeightCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineAddBin";
        cross = new WeightCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineDivDec";
        WeightCombineDiv cross1 = new WeightCombineDiv(new CombineDiv(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineMulDec";
        WeightCombineMulDec cross2 = new WeightCombineMulDec(new CombineMulDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineMulInt";
        cross2 = new WeightCombineMulDec(new CombineMulDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineMulSubDec";
        WeightCombineSubDec cross3 = new WeightCombineSubDec(new CombineSubDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineMulSubInt";
        cross3 = new WeightCombineSubDec(new CombineSubDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCopyValueDec cross4 = new WeightCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCopyValueInt";
        cross4 = new WeightCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCopyValueBin";
        cross4 = new WeightCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineAnd";
        WeightCombineAnd cross5 = new WeightCombineAnd(new CombineAnd(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));

        name = "weightCombineOr";
        WeightCombineOr cross6 = new WeightCombineOr(new CombineOr(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, cross);
        reproductions.add(cross);
        bagMutateEdge.addAll(Collections.nCopies(cross.getProbability(), name));
    }
}

