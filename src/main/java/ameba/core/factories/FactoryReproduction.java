package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.crossCell.AddNodes;
import ameba.core.reproductions.crossCell.ICrossCell;
import ameba.core.reproductions.crossCell.TransferNodes;
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
import java.util.Random;

/**
 * Created by marko on 3/7/17.
 */
public class FactoryReproduction {
    ArrayList<Reproduction> reproductions;
    HashMap<String, IMutateEdge> mutateEdges;
    HashMap<String, IMutateEdgeDec> mutateEdgesDec;
    HashMap<String, IMutateEdgeInt> mutateEdgesInt;
    HashMap<String, IMutateEdgeBin> mutateEdgesBin;
    HashMap<String, IMutateNode> mutateNodes;
    HashMap<String, IMutateCell> mutateCells;
    HashMap<String, ICrossEdge> crossEdges;
    HashMap<String, ICrossEdgeDec> crossEdgesDec;
    HashMap<String, ICrossEdgeInt> crossEdgesInt;
    HashMap<String, ICrossEdgeBin> crossEdgesBin;
    HashMap<String, ICrossNode> crossNodes;
    HashMap<String, ICrossCell> crossCells;

    ArrayList<String> bagReproductions;
    ArrayList<String> bagMutateEdge;
    ArrayList<String> bagMutateNode;
    ArrayList<String> bagMutateCell;
    ArrayList<String> bagCrossEdge;
    ArrayList<String> bagCrossNode;
    ArrayList<String> bagCrossCell;

    FactoryCell factoryCell;
    FactoryNode factoryNode;
    FactoryEdge factoryEdge;

    Random random;

    public FactoryReproduction(FactoryEdge factoryEdge, FactoryNode factoryNode, FactoryCell factoryCell) {
        this.random = new Random();
        this.factoryEdge = factoryEdge;
        this.factoryNode = factoryNode;
        this.factoryCell = factoryCell;
        reproductions = new ArrayList<>();
        mutateEdges = new HashMap<>();
        mutateEdgesDec = new HashMap<>();
        mutateEdgesInt = new HashMap<>();
        mutateEdgesBin = new HashMap<>();
        mutateNodes = new HashMap<>();
        mutateCells = new HashMap<>();
        crossEdges = new HashMap<>();
        crossEdgesDec = new HashMap<>();
        crossEdgesInt = new HashMap<>();
        crossEdgesBin = new HashMap<>();
        crossNodes = new HashMap<>();
        crossCells = new HashMap<>();

        this.bagReproductions = new ArrayList<>();
        this.bagMutateEdge = new ArrayList<>();
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
        loadEdgeCross(node);
        loadNodeMutations(node);
        loadNodeCross(node);
        loadCellMutations(node);
        loadCellCross(node);
    }

    private void loadEdgeMutations(JsonNode node) throws IOException {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("mutateEdge").get("probability").asInt(), "mutateEdges"));

        String name = "weightAddValueDec";
        WeightAddValueDec weightAddValueDec = new WeightAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put(name, weightAddValueDec);
        mutateEdges.put(name, weightAddValueDec);
        reproductions.add(weightAddValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDec.getProbability(), name));

        name = "weightAddValueInt";
        WeightAddValueInt weightAddValueInt = new WeightAddValueInt(new AddValueInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesInt.put(name, weightAddValueInt);
        mutateEdges.put(name, weightAddValueInt);
        reproductions.add(weightAddValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueInt.getProbability(), name));

        name = "weightAddValueBin";
        WeightAddValueBin weightAddValueBin = new WeightAddValueBin(new AddValueBin(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesBin.put(name, weightAddValueBin);
        mutateEdges.put(name, weightAddValueBin);
        reproductions.add(weightAddValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueBin.getProbability(), name));

        name = "weightInverse";
        WeightInverse weightInverse = new WeightInverse(new InverseValue(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put(name, weightInverse);
        mutateEdges.put(name, weightInverse);
        reproductions.add(weightInverse);
        bagMutateEdge.addAll(Collections.nCopies(weightInverse.getProbability(), name));

        name = "weightMixSignDec";
        WeightMixSignDec weightMixSignDec = new WeightMixSignDec(new MixSignDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put(name, weightMixSignDec);
        mutateEdges.put(name, weightMixSignDec);
        reproductions.add(weightMixSignDec);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignDec.getProbability(), name));

        name = "weightMixSignInt";
        WeightMixSignInt weightMixSignInt = new WeightMixSignInt(new MixSignInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesInt.put(name, weightMixSignInt);
        mutateEdges.put(name, weightMixSignInt);
        reproductions.add(weightMixSignInt);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignInt.getProbability(), name));

        name = "weightRandValueDec";
        WeightRandValueDec weightRandValueDec = new WeightRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesDec.put(name, weightRandValueDec);
        mutateEdges.put(name, weightRandValueDec);
        reproductions.add(weightRandValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueDec.getProbability(), name));

        name = "weightRandValueInt";
        WeightRandValueInt weightRandValueInt = new WeightRandValueInt(new RandValueInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesInt.put(name, weightRandValueInt);
        mutateEdges.put(name, weightRandValueInt);
        reproductions.add(weightRandValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueInt.getProbability(), name));

        name = "weightRandValueBin";
        WeightRandValueBin weightRandValueBin = new WeightRandValueBin(new RandValueBin(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdgesBin.put(name, weightRandValueBin);
        mutateEdges.put(name, weightRandValueBin);
        reproductions.add(weightRandValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueBin.getProbability(), name));
    }

    private void loadEdgeCross(JsonNode node) throws IOException {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("crossEdge").get("probability").asInt(), "crossEdge"));

        String name = "weightCombineAddDec";
        WeightCombineAddDec weightCombineAddDec = new WeightCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineAddDec);
        crossEdges.put(name, weightCombineAddDec);
        reproductions.add(weightCombineAddDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineAddDec.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCombineAddInt weightCombineAddInt = new WeightCombineAddInt(new CombineAddInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCombineAddInt);
        crossEdges.put(name, weightCombineAddInt);
        reproductions.add(weightCombineAddInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineAddInt.getProbability(), name));

        name = "weightCombineAnd";
        WeightCombineAnd weightCombineAnd = new WeightCombineAnd(new CombineAnd(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesBin.put(name, weightCombineAnd);
        crossEdges.put(name, weightCombineAnd);
        reproductions.add(weightCombineAnd);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineAnd.getProbability(), name));

        name = "weightCombineOr";
        WeightCombineOr weightCombineOr = new WeightCombineOr(new CombineOr(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesBin.put(name, weightCombineOr);
        crossEdges.put(name, weightCombineOr);
        reproductions.add(weightCombineOr);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineOr.getProbability(), name));

        name = "weightCombineDiv";
        WeightCombineDiv weightCombineDiv = new WeightCombineDiv(new CombineDiv(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineDiv);
        crossEdges.put(name, weightCombineDiv);
        reproductions.add(weightCombineDiv);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineDiv.getProbability(), name));

        name = "weightCombineMulDec";
        WeightCombineMulDec weightCombineMulDec = new WeightCombineMulDec(new CombineMulDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineMulDec);
        crossEdges.put(name, weightCombineMulDec);
        reproductions.add(weightCombineMulDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineMulDec.getProbability(), name));

        name = "weightCombineMulInt";
        WeightCombineMulInt weightCombineMulInt = new WeightCombineMulInt(new CombineMulInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCombineMulInt);
        crossEdges.put(name, weightCombineMulInt);
        reproductions.add(weightCombineMulInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineMulInt.getProbability(), name));

        name = "weightCombineMulSubDec";
        WeightCombineSubDec weightCombineSubDec = new WeightCombineSubDec(new CombineSubDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCombineSubDec);
        crossEdges.put(name, weightCombineSubDec);
        reproductions.add(weightCombineSubDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineSubDec.getProbability(), name));

        name = "weightCombineMulSubInt";
        WeightCombineSubInt weightCombineSubInt = new WeightCombineSubInt(new CombineSubInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCombineSubInt);
        crossEdges.put(name, weightCombineSubInt);
        reproductions.add(weightCombineSubInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineSubInt.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCopyValueDec weightCopyValueDec = new WeightCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesDec.put(name, weightCopyValueDec);
        crossEdges.put(name, weightCopyValueDec);
        reproductions.add(weightCopyValueDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCopyValueDec.getProbability(), name));

        name = "weightCopyValueInt";
        WeightCopyValueInt weightCopyValueInt = new WeightCopyValueInt(new CopyValueInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesInt.put(name, weightCopyValueInt);
        crossEdges.put(name, weightCopyValueInt);
        reproductions.add(weightCopyValueInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCopyValueInt.getProbability(), name));

        name = "weightCopyValueBin";
        WeightCopyValueBin weightCopyValueBin = new WeightCopyValueBin(new CopyValueBin(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdgesBin.put(name, weightCopyValueBin);
        crossEdges.put(name, weightCopyValueBin);
        reproductions.add(weightCopyValueBin);
        bagCrossEdge.addAll(Collections.nCopies(weightCopyValueBin.getProbability(), name));
    }

    private void loadNodeMutations(JsonNode node) throws IOException {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("mutateNode").get("probability").asInt(), "mutateNode"));

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
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("crossNode").get("probability").asInt(), "crossNode"));

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
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("mutateCell").get("probability").asInt(), "mutateCell"));

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
                new int[]{node.get(0).get("mutateCell").get(name).get("nodesLimit").get(0).asInt(), node.get(0).get("mutateCell").get(name).get("nodesLimit").get(1).asInt()},
                node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addNodesGroup);
        reproductions.add(addNodesGroup);
        bagMutateCell.addAll(Collections.nCopies(addNodesGroup.getProbability(), name));

        name = "randCell";
        RandCell randCell = new RandCell(factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, randCell);
        reproductions.add(randCell);
        bagMutateCell.addAll(Collections.nCopies(randCell.getProbability(), name));

        name = "removeNode";
        RemoveNode removeNode = new RemoveNode(factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, removeNode);
        reproductions.add(removeNode);
        bagMutateCell.addAll(Collections.nCopies(removeNode.getProbability(), name));

        name = "removeNode1";
        RemoveNode1 removeNode1 = new RemoveNode1(factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, removeNode1);
        reproductions.add(removeNode1);
        bagMutateCell.addAll(Collections.nCopies(removeNode1.getProbability(), name));

        name = "removeNodesGroup";
        RemoveNodesGroup removeNodesGroup = new RemoveNodesGroup(factoryCell,
                new int[]{node.get(0).get("mutateCell").get(name).get("nodesLimit").get(0).asInt(), node.get(0).get("mutateCell").get(name).get("nodesLimit").get(1).asInt()},
                node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, removeNodesGroup);
        reproductions.add(removeNodesGroup);
        bagMutateCell.addAll(Collections.nCopies(removeNodesGroup.getProbability(), name));

        name = "replaceNode";
        ReplaceNode replaceNode = new ReplaceNode(factoryNode, factoryCell, factoryEdge, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, replaceNode);
        reproductions.add(replaceNode);
        bagMutateCell.addAll(Collections.nCopies(replaceNode.getProbability(), name));

        name = "switchEdgesSources";
        SwitchEdgesSources switchEdgesSources = new SwitchEdgesSources(node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, switchEdgesSources);
        reproductions.add(switchEdgesSources);
        bagMutateCell.addAll(Collections.nCopies(switchEdgesSources.getProbability(), name));

        name = "switchEdgesTargets";
        SwitchEdgesTargets switchEdgesTargets = new SwitchEdgesTargets(node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, switchEdgesTargets);
        reproductions.add(switchEdgesTargets);
        bagMutateCell.addAll(Collections.nCopies(switchEdgesTargets.getProbability(), name));
    }

    private void loadCellCross(JsonNode node) throws IOException {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("crossCell").get("probability").asInt(), "crossCell"));

        String name = "addNodes";
        AddNodes addNodes = new AddNodes(factoryNode, factoryCell, factoryEdge,
                new int[]{node.get(0).get("crossCell").get(name).get("nodesLimit").get(0).asInt(), node.get(0).get("crossCell").get(name).get("nodesLimit").get(1).asInt()},
                node.get(0).get("crossCell").get(name).get("probability").asInt());
        crossCells.put(name, addNodes);
        reproductions.add(addNodes);
        bagCrossCell.addAll(Collections.nCopies(addNodes.getProbability(), name));

        name = "transferNodes";
        TransferNodes transferNodes = new TransferNodes(factoryNode, factoryCell, factoryEdge,
                new int[]{node.get(0).get("crossCell").get(name).get("nodesLimitRemove").get(0).asInt(), node.get(0).get("crossCell").get(name).get("nodesLimitRemove").get(1).asInt()},
                new int[]{node.get(0).get("crossCell").get(name).get("nodesLimitAdd").get(0).asInt(), node.get(0).get("crossCell").get(name).get("nodesLimitAdd").get(1).asInt()},
                node.get(0).get("crossCell").get(name).get("probability").asInt());
        crossCells.put(name, transferNodes);
        reproductions.add(transferNodes);
        bagCrossCell.addAll(Collections.nCopies(transferNodes.getProbability(), name));
    }

    public Cell repCell(Cell parent) throws Exception {
        Cell child = parent.clone();
        String repGroup = bagReproductions.get(random.nextInt(bagReproductions.size()));
        switch (repGroup) {
            case "mutateEdges": {
                String rep = bagMutateEdge.get(random.nextInt(bagMutateEdge.size()));
                Edge edge = child.getEdges(mutateEdges.get(rep).getEdgeType()).get(random.nextInt(child.getEdges(mutateEdges.get(rep).getEdgeType()).size()));
                switch (edge.getType()) {
                    case BOOLEAN:
                        edge = mutateEdgesBin.get(rep).mutate((EdgeBin) edge);
                        break;
                    case INTEGER:
                        edge = mutateEdgesInt.get(rep).mutate((EdgeInt) edge);
                        break;
                    case DECIMAL:
                        edge = mutateEdgesDec.get(rep).mutate((EdgeDec) edge);
                        break;
                }
                int t = 0;
            }
            break;
            case "crossEdge":
                String rep = bagCrossEdge.get(random.nextInt(bagCrossEdge.size()));
                Edge edge1 = child.getEdges(crossEdges.get(rep).getEdgeType()).get(random.nextInt(child.getEdges(crossEdges.get(rep).getEdgeType()).size()));
                Edge edge2 = child.getEdges(crossEdges.get(rep).getEdgeType(), edge1).get(random.nextInt(child.getEdges(crossEdges.get(rep).getEdgeType(), edge1).size()));
                switch (edge1.getType()) {
                    case BOOLEAN:
                        edge1 = crossEdgesBin.get(rep).cross((EdgeBin) edge1, (EdgeBin) edge2);
                        break;
                    case INTEGER:
                        edge1 = crossEdgesInt.get(rep).cross((EdgeInt) edge1, (EdgeInt) edge2);
                        break;
                    case DECIMAL:
                        edge1 = crossEdgesDec.get(rep).cross((EdgeDec) edge1, (EdgeDec) edge2);
                        break;
                }
                int t = 0;
                break;
            case "mutateNode":
                break;
            case "crossNode":
                break;
            case "mutateCell":
                break;
            case "crossCell":
                break;
        }
        return null;
    }
}

