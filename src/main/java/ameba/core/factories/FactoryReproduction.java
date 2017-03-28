package ameba.core.factories;

import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.crossCell.ICrossCell;
import ameba.core.reproductions.crossEdge.*;
import ameba.core.reproductions.crossNode.*;
import ameba.core.reproductions.mutateCell.*;
import ameba.core.reproductions.mutateEdge.*;
import ameba.core.reproductions.mutateNode.*;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;
import ameba.core.reproductions.parametersOperations.genParCrossover.*;
import ameba.core.reproductions.parametersOperations.genParMutation.*;
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
    HashMap<String, IMutateEdgeDec> mutateEdgesDec;
    HashMap<String, IMutateEdgeInt> mutateEdgesInt;
    HashMap<String, IMutateEdgeBin> mutateEdgesBin;
    HashMap<String, IMutateNode> mutateNodes;
    HashMap<String, IMutateCell> mutateCells;
    HashMap<String, ICrossEdgeDec> crossEdgesDec;
    HashMap<String, ICrossEdgeInt> crossEdgesInt;
    HashMap<String, ICrossEdgeBin> crossEdgesBin;
    HashMap<String, ICrossNode> crossNodes;
    HashMap<String, ICrossCell> crossCells;

    ArrayList<String> bagMutateEdgeDec;
    ArrayList<String> bagMutateEdgeInt;
    ArrayList<String> bagMutateEdgeBin;
    ArrayList<String> bagMutateNode;
    ArrayList<String> bagMutateCell;
    ArrayList<String> bagCrossEdgeDec;
    ArrayList<String> bagCrossEdgeInt;
    ArrayList<String> bagCrossEdgeBin;
    ArrayList<String> bagCrossNode;
    ArrayList<String> bagCrossCell;

    FactoryCell factoryCell;
    FactoryNode factoryNode;
    FactoryEdge factoryEdge;

    public FactoryReproduction(FactoryEdge factoryEdge, FactoryNode factoryNode, FactoryCell factoryCell) {
        this.factoryEdge = factoryEdge;
        this.factoryNode = factoryNode;
        this.factoryCell = factoryCell;
        reproductions = new ArrayList<>();
        mutateEdgesDec = new HashMap<>();
        mutateEdgesInt = new HashMap<>();
        mutateEdgesBin = new HashMap<>();
        mutateNodes = new HashMap<>();
        mutateCells = new HashMap<>();
        crossEdgesDec = new HashMap<>();
        crossEdgesInt = new HashMap<>();
        crossEdgesBin = new HashMap<>();
        crossNodes = new HashMap<>();
        crossCells = new HashMap<>();

        this.bagMutateEdgeDec = new ArrayList();
        this.bagMutateEdgeInt = new ArrayList();
        this.bagMutateEdgeBin = new ArrayList();
        this.bagMutateNode = new ArrayList();
        this.bagMutateCell = new ArrayList();
        this.bagCrossEdgeDec = new ArrayList();
        this.bagCrossEdgeInt = new ArrayList();
        this.bagCrossEdgeBin = new ArrayList();
        this.bagCrossNode = new ArrayList();
        this.bagCrossCell = new ArrayList();
    }

    public void loadSettings(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        loadEdgeMutations(node);
        loadEdgeCross(node);
        loadNodeMutations(node);
        loadNodeCross(node);
        loadCellMutations(node);
    }

    private void loadEdgeMutations(JsonNode node) throws IOException {
        String name = "weightAddValueDec";
        WeightAddValueDec weightAddValueDec = new WeightAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put(name, weightAddValueDec);
        reproductions.add(weightAddValueDec);
        bagMutateEdgeDec.addAll(Collections.nCopies(weightAddValueDec.getProbability(), name));

        name = "weightAddValueInt";
        WeightAddValueInt weightAddValueInt = new WeightAddValueInt(new AddValueInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesInt.put("weightAddValueInt", weightAddValueInt);
        reproductions.add(weightAddValueInt);
        bagMutateEdgeInt.addAll(Collections.nCopies(weightAddValueInt.getProbability(), name));

        name = "weightAddValueBin";
        WeightAddValueBin weightAddValueBin = new WeightAddValueBin(new AddValueBin(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesBin.put("weightAddValueBin", weightAddValueBin);
        reproductions.add(weightAddValueBin);
        bagMutateEdgeBin.addAll(Collections.nCopies(weightAddValueBin.getProbability(), name));

        name = "weightInverse";
        WeightInverse weightInverse = new WeightInverse(new InverseValue(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put("weightInverse", weightInverse);
        reproductions.add(weightInverse);
        bagMutateEdgeDec.addAll(Collections.nCopies(weightInverse.getProbability(), name));

        name = "weightMixSignDec";
        WeightMixSignDec weightMixSignDec = new WeightMixSignDec(new MixSignDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put("weightMixSignDec", weightMixSignDec);
        reproductions.add(weightMixSignDec);
        bagMutateEdgeDec.addAll(Collections.nCopies(weightMixSignDec.getProbability(), name));

        name = "weightMixSignInt";
        WeightMixSignInt weightMixSignInt = new WeightMixSignInt(new MixSignInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesInt.put("weightMixSignInt", weightMixSignInt);
        reproductions.add(weightMixSignInt);
        bagMutateEdgeInt.addAll(Collections.nCopies(weightMixSignInt.getProbability(), name));

        name = "weightRandValueDec";
        WeightRandValueDec weightRandValueDec = new WeightRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put("weightRandValueDec", weightRandValueDec);
        reproductions.add(weightRandValueDec);
        bagMutateEdgeDec.addAll(Collections.nCopies(weightRandValueDec.getProbability(), name));

        name = "weightRandValueInt";
        WeightRandValueInt weightRandValueInt = new WeightRandValueInt(new RandValueInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesInt.put("weightRandValueInt", weightRandValueInt);
        reproductions.add(weightRandValueInt);
        bagMutateEdgeInt.addAll(Collections.nCopies(weightRandValueInt.getProbability(), name));

        name = "weightRandValueBin";
        WeightRandValueBin weightRandValueBin = new WeightRandValueBin(new RandValueBin(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesBin.put("weightRandValueBin", weightRandValueBin);
        reproductions.add(weightRandValueBin);
        bagMutateEdgeBin.addAll(Collections.nCopies(weightRandValueBin.getProbability(), name));
    }

    private void loadEdgeCross(JsonNode node) throws IOException {
        String name = "weightCombineAddDec";
        WeightCombineAddDec weightCombineAddDec = new WeightCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineAddDec);
        reproductions.add(weightCombineAddDec);
        bagCrossEdgeDec.addAll(Collections.nCopies(weightCombineAddDec.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCombineAddInt weightCombineAddInt = new WeightCombineAddInt(new CombineAddInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCombineAddInt);
        reproductions.add(weightCombineAddInt);
        bagCrossEdgeInt.addAll(Collections.nCopies(weightCombineAddInt.getProbability(), name));

        name = "weightCombineAnd";
        WeightCombineAnd weightCombineAnd = new WeightCombineAnd(new CombineAnd(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesBin.put(name, weightCombineAnd);
        reproductions.add(weightCombineAnd);
        bagCrossEdgeBin.addAll(Collections.nCopies(weightCombineAnd.getProbability(), name));

        name = "weightCombineOr";
        WeightCombineOr weightCombineOr = new WeightCombineOr(new CombineOr(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesBin.put(name, weightCombineOr);
        reproductions.add(weightCombineOr);
        bagCrossEdgeBin.addAll(Collections.nCopies(weightCombineOr.getProbability(), name));

        name = "weightCombineDiv";
        WeightCombineDiv weightCombineDiv = new WeightCombineDiv(new CombineDiv(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineDiv);
        reproductions.add(weightCombineDiv);
        bagCrossEdgeDec.addAll(Collections.nCopies(weightCombineDiv.getProbability(), name));

        name = "weightCombineMulDec";
        WeightCombineMulDec weightCombineMulDec = new WeightCombineMulDec(new CombineMulDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineMulDec);
        reproductions.add(weightCombineMulDec);
        bagCrossEdgeDec.addAll(Collections.nCopies(weightCombineMulDec.getProbability(), name));

        name = "weightCombineMulInt";
        WeightCombineMulInt weightCombineMulInt = new WeightCombineMulInt(new CombineMulInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCombineMulInt);
        reproductions.add(weightCombineMulInt);
        bagCrossEdgeInt.addAll(Collections.nCopies(weightCombineMulInt.getProbability(), name));

        name = "weightCombineMulSubDec";
        WeightCombineSubDec weightCombineSubDec = new WeightCombineSubDec(new CombineSubDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineSubDec);
        reproductions.add(weightCombineSubDec);
        bagCrossEdgeDec.addAll(Collections.nCopies(weightCombineSubDec.getProbability(), name));

        name = "weightCombineMulSubInt";
        WeightCombineSubInt weightCombineSubInt = new WeightCombineSubInt(new CombineSubInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCombineSubInt);
        reproductions.add(weightCombineSubInt);
        bagCrossEdgeInt.addAll(Collections.nCopies(weightCombineSubInt.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCopyValueDec weightCopyValueDec = new WeightCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCopyValueDec);
        reproductions.add(weightCopyValueDec);
        bagCrossEdgeDec.addAll(Collections.nCopies(weightCopyValueDec.getProbability(), name));

        name = "weightCopyValueInt";
        WeightCopyValueInt weightCopyValueInt = new WeightCopyValueInt(new CopyValueInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCopyValueInt);
        reproductions.add(weightCopyValueInt);
        bagCrossEdgeInt.addAll(Collections.nCopies(weightCopyValueInt.getProbability(), name));

        name = "weightCopyValueBin";
        WeightCopyValueBin weightCopyValueBin = new WeightCopyValueBin(new CopyValueBin(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesBin.put(name, weightCopyValueBin);
        reproductions.add(weightCopyValueBin);
        bagCrossEdgeBin.addAll(Collections.nCopies(weightCopyValueBin.getProbability(), name));
    }

    private void loadNodeMutations(JsonNode node) throws IOException {
        String name = "parAddValueInt";
        ParAddValueInt parAddValueInt = new ParAddValueInt(new AddValueInt(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parAddValueInt);
        reproductions.add(parAddValueInt);
        bagMutateNode.addAll(Collections.nCopies(parAddValueInt.getProbability(), name));

        name = "parAddValueDec";
        ParAddValueDec parAddValueDec = new ParAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parAddValueDec);
        reproductions.add(parAddValueDec);
        bagMutateNode.addAll(Collections.nCopies(parAddValueDec.getProbability(), name));

        name = "parInverse";
        ParInverse parInverse = new ParInverse(new InverseValue(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parInverse);
        reproductions.add(parInverse);
        bagMutateNode.addAll(Collections.nCopies(parInverse.getProbability(), name));

        name = "parMixSignDec";
        ParMixSignDec parMixSignDec = new ParMixSignDec(new MixSignDec(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parMixSignDec);
        reproductions.add(parMixSignDec);
        bagMutateNode.addAll(Collections.nCopies(parMixSignDec.getProbability(), name));

        name = "parMixSignInt";
        ParMixSignInt parMixSignInt = new ParMixSignInt(new MixSignInt(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parMixSignInt);
        reproductions.add(parMixSignInt);
        bagMutateNode.addAll(Collections.nCopies(parMixSignInt.getProbability(), name));

        name = "parRandValueBin";
        ParRandValueBin parRandValueBin = new ParRandValueBin(new RandValueBin(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parRandValueBin);
        reproductions.add(parRandValueBin);
        bagMutateNode.addAll(Collections.nCopies(parRandValueBin.getProbability(), name));

        name = "parRandValueInt";
        ParRandValueInt parRandValueInt = new ParRandValueInt(new RandValueInt(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parRandValueInt);
        reproductions.add(parRandValueInt);
        bagMutateNode.addAll(Collections.nCopies(parRandValueInt.getProbability(), name));

        name = "parRandValueDec";
        ParRandValueDec parRandValueDec = new ParRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateNode").get(name).toString())));
        mutateNodes.put(name, parRandValueDec);
        reproductions.add(parRandValueDec);
        bagMutateNode.addAll(Collections.nCopies(parRandValueDec.getProbability(), name));
    }

    private void loadNodeCross(JsonNode node) throws IOException {
        String name = "parCombineAddDec";
        ParCombineAddDec parCombineAddDec = new ParCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineAddDec);
        reproductions.add(parCombineAddDec);
        bagCrossNode.addAll(Collections.nCopies(parCombineAddDec.getProbability(), name));

        name = "parCombineAddInt";
        ParCombineAddInt parCombineAddInt = new ParCombineAddInt(new CombineAddInt(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineAddInt);
        reproductions.add(parCombineAddInt);
        bagCrossNode.addAll(Collections.nCopies(parCombineAddInt.getProbability(), name));

        name = "parCombineAnd";
        ParCombineAnd parCombineAnd = new ParCombineAnd(new CombineAnd(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineAnd);
        reproductions.add(parCombineAnd);
        bagCrossNode.addAll(Collections.nCopies(parCombineAnd.getProbability(), name));

        name = "parCombineOr";
        ParCombineOr parCombineOr = new ParCombineOr(new CombineOr(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineOr);
        reproductions.add(parCombineOr);
        bagCrossNode.addAll(Collections.nCopies(parCombineOr.getProbability(), name));

        name = "parCombineDiv";
        ParCombineDiv parCombineDiv = new ParCombineDiv(new CombineDiv(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineDiv);
        reproductions.add(parCombineDiv);
        bagCrossNode.addAll(Collections.nCopies(parCombineDiv.getProbability(), name));

        name = "parCombineMulDec";
        ParCombineMulDec parCombineMulDec = new ParCombineMulDec(new CombineMulDec(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineMulDec);
        reproductions.add(parCombineMulDec);
        bagCrossNode.addAll(Collections.nCopies(parCombineMulDec.getProbability(), name));

        name = "parCombineMulInt";
        ParCombineMulInt parCombineMulInt = new ParCombineMulInt(new CombineMulInt(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineMulInt);
        reproductions.add(parCombineMulInt);
        bagCrossNode.addAll(Collections.nCopies(parCombineMulInt.getProbability(), name));

        name = "parCombineSubDec";
        ParCombineSubDec parCombineSubDec = new ParCombineSubDec(new CombineSubDec(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineSubDec);
        reproductions.add(parCombineSubDec);
        bagCrossNode.addAll(Collections.nCopies(parCombineSubDec.getProbability(), name));

        name = "parCombineSubInt";
        ParCombineSubInt parCombineSubInt = new ParCombineSubInt(new CombineSubInt(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCombineSubInt);
        reproductions.add(parCombineSubInt);
        bagCrossNode.addAll(Collections.nCopies(parCombineSubInt.getProbability(), name));

        name = "parCopyValueDec";
        ParCopyValueDec parCopyValueDec = new ParCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCopyValueDec);
        reproductions.add(parCopyValueDec);
        bagCrossNode.addAll(Collections.nCopies(parCopyValueDec.getProbability(), name));

        name = "parCopyValueInt";
        ParCopyValueInt parCopyValueInt = new ParCopyValueInt(new CopyValueInt(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCopyValueInt);
        reproductions.add(parCopyValueInt);
        bagCrossNode.addAll(Collections.nCopies(parCopyValueInt.getProbability(), name));

        name = "parCopyValueBin";
        ParCopyValueBin parCopyValueBin = new ParCopyValueBin(new CopyValueBin(ParOperationSettings.create(node.get(0).get("crossNode").get(name).toString())));
        crossNodes.put(name, parCopyValueBin);
        reproductions.add(parCopyValueBin);
        bagCrossNode.addAll(Collections.nCopies(parCopyValueBin.getProbability(), name));
    }

    private void loadCellMutations(JsonNode node) throws IOException {
        String name = "addNode1";
        AddNode1 addNode1 = new AddNode1(factoryNode, factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addNode1);
        reproductions.add(addNode1);
        bagMutateCell.addAll(Collections.nCopies(addNode1.getProbability(), name));

        name = "addNode2";
        AddNode2 addNode2 = new AddNode2(factoryNode, factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addNode2);
        reproductions.add(addNode2);
        bagMutateCell.addAll(Collections.nCopies(addNode2.getProbability(), name));

        name = "addNode3";
        AddNode3 addNode3 = new AddNode3(factoryNode, factoryCell, factoryEdge, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addNode3);
        reproductions.add(addNode3);
        bagMutateCell.addAll(Collections.nCopies(addNode3.getProbability(), name));

        name = "addNodesGroup";
        AddNodesGroup addNodesGroup = new AddNodesGroup(factoryNode, factoryCell, factoryEdge,
                node.get(0).get("mutateCell").get(name).get("minNodes").asInt(),
                node.get(0).get("mutateCell").get(name).get("maxNodes").asInt(),
                node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addNodesGroup);
        reproductions.add(addNodesGroup);
        bagMutateCell.addAll(Collections.nCopies(addNodesGroup.getProbability(), name));
    }
}

