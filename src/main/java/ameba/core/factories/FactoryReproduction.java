package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.MultipleReproductions;
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
import org.apache.commons.lang.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 3/7/17.
 */
public class FactoryReproduction implements Serializable {
    ArrayList<Reproduction> reproductions;
    HashMap<String, IMutateEdge> mutateEdges;
    HashMap<String, IMutateNode> mutateNodes;
    HashMap<String, IMutateCell> mutateCells;
    HashMap<String, ICrossEdge> crossEdges;
    HashMap<String, ICrossNode> crossNodes;
    HashMap<String, ICrossCell> crossCells;
    MultipleReproductions multipleReproductions;

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
    String rep = "";
    String repGroup = "";

    public FactoryReproduction(FactoryEdge factoryEdge, FactoryNode factoryNode, FactoryCell factoryCell) {
        this.random = new Random();
        this.factoryEdge = factoryEdge;
        this.factoryNode = factoryNode;
        this.factoryCell = factoryCell;
        reproductions = new ArrayList<>();
        mutateEdges = new HashMap<>();
        mutateNodes = new HashMap<>();
        mutateCells = new HashMap<>();
        crossEdges = new HashMap<>();
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

    public static FactoryReproduction build() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryReproduction.class.getClassLoader().getResourceAsStream("reproductionFactorySettings.json"));
        FactoryReproduction factoryNode = new FactoryReproduction(FactoryEdge.build(), FactoryNode.build(), FactoryCell.build());
        factoryNode.loadSettings(jsonSettings.get("reproductionFactorySettings").toString());
        return factoryNode;
    }

    public static FactoryReproduction build(String filePathReproduction, String filePathCell, String filePathNode, String filePathEdge) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(filePathReproduction));
        FactoryReproduction factoryNode = new FactoryReproduction(FactoryEdge.build(filePathEdge), FactoryNode.build(filePathNode), FactoryCell.build(filePathCell, filePathNode, filePathEdge));
        factoryNode.loadSettings(jsonSettings.get("reproductionFactorySettings").toString());
        return factoryNode;
    }

    public FactoryReproduction clone() {
        return (FactoryReproduction) SerializationUtils.clone(this);
    }

    public Cell repCell(Cell parent1, Cell parent2) throws Exception {
        Cell child = parent1.clone();
        Cell p = parent2.clone();
        repGroup = bagReproductions.get(random.nextInt(bagReproductions.size()));
        switch (repGroup) {
            case "mutateEdge": {
                rep = bagMutateEdge.get(random.nextInt(bagMutateEdge.size()));
                ArrayList<? extends Edge> edges = child.getEdgesUnlockedWeight(mutateEdges.get(rep).getEdgeType());
                if (edges.size() > 0) {
                    Edge edge = edges.get(random.nextInt(edges.size()));
                    mutateEdges.get(rep).mutate(edge);
                } else
                    throw new Exception("Can't find unlocked edge of type: " + mutateEdges.get(rep).getEdgeType().toString() + " skipping this reproduction");
            }
            break;
            case "crossEdge": {
                rep = bagCrossEdge.get(random.nextInt(bagCrossEdge.size()));
                ArrayList<? extends Edge> edges = child.getEdgesUnlockedWeight(crossEdges.get(rep).getEdgeType());
                if (edges.size() > 1) {
                    Edge edge1 = edges.get(random.nextInt(edges.size()));
                    Edge edge2 = edges.get(random.nextInt(edges.size()));
                    crossEdges.get(rep).cross(edge1, edge2);
                }
            }
            break;
            case "mutateNode": {
                rep = bagMutateNode.get(random.nextInt(bagMutateNode.size()));
                ArrayList<Node> nodes = child.getInnerNodesParTypeUnlocked(mutateNodes.get(rep).getType());
                if (nodes.size() > 0) {
                    Node node = nodes.get(random.nextInt(nodes.size()));
                    mutateNodes.get(rep).mutate(node);
                }
            }
            break;
            case "crossNode": {
                rep = bagCrossNode.get(random.nextInt(bagCrossNode.size()));
                ArrayList<Node> nodes = child.getInnerNodesParTypeUnlocked(crossNodes.get(rep).getType());
                if (nodes.size() > 1) {
                    Node node1 = nodes.get(random.nextInt(nodes.size()));
                    Node node2 = nodes.get(random.nextInt(nodes.size()));
                    crossNodes.get(rep).cross(node1, node2);
                }
            }
            break;
            case "mutateCell":
                rep = bagMutateCell.get(random.nextInt(bagMutateCell.size()));
                if (child.hasLockedNodes()) {
                    ArrayList<String> bag = (ArrayList) bagMutateCell.clone();
                    bag.remove(Collections.singleton("randCell"));
                }
                mutateCells.get(rep).mutate(child);
                break;
            case "crossCell":
                rep = bagCrossCell.get(random.nextInt(bagCrossCell.size()));
                crossCells.get(rep).cross(child, p.clone());
                break;
            case "multipleReproductions":
                int n = random.nextInt(multipleReproductions.getLimits()[1] - multipleReproductions.getLimits()[0]) + multipleReproductions.getLimits()[0];

                for (int i = 0; i < n; i++) {
                    String rep = bagReproductions.get(random.nextInt(bagReproductions.size()));
                    while (rep.equals("multipleReproductions")) {
                        rep = bagReproductions.get(random.nextInt(bagReproductions.size()));
                    }
                    parent1 = child.clone();
                    child = repCell(parent1, parent2);
                }
                break;
        }
        child.lastRep = rep;
        if (child.checkCell().size() > 0) {
            throw new Exception("Cell no properly generated by reproduction methods");
        }
        return child;
    }

    public String getRepGroup() {
        return repGroup;
    }

    public String getRep() {
        return rep;
    }

    public Cell repMutateCell(Cell parent1, Cell parent2, String reproduction) throws Exception {
        Cell child = parent1.clone();
        mutateCells.get(reproduction).mutate(child);
        return child;
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
        loadMultipleReproductions(node);
    }

    private void loadEdgeMutations(JsonNode node) throws IOException {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("mutateEdge").get("probability").asInt(), "mutateEdge"));

        String name = "weightAddValueDec";
        WeightAddValueDec weightAddValueDec = new WeightAddValueDec(new AddValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightAddValueDec);
        reproductions.add(weightAddValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDec.getProbability(), name));

        name = "weightAddValueInt";
        WeightAddValueInt weightAddValueInt = new WeightAddValueInt(new AddValueInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightAddValueInt);
        reproductions.add(weightAddValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueInt.getProbability(), name));

        name = "weightAddValueBin";
        WeightAddValueBin weightAddValueBin = new WeightAddValueBin(new AddValueBin(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightAddValueBin);
        reproductions.add(weightAddValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueBin.getProbability(), name));

        name = "weightInverse";
        WeightInverse weightInverse = new WeightInverse(new InverseValue(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightInverse);
        reproductions.add(weightInverse);
        bagMutateEdge.addAll(Collections.nCopies(weightInverse.getProbability(), name));

        name = "weightMixSignDec";
        WeightMixSignDec weightMixSignDec = new WeightMixSignDec(new MixSignDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightMixSignDec);
        reproductions.add(weightMixSignDec);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignDec.getProbability(), name));

        name = "weightMixSignInt";
        WeightMixSignInt weightMixSignInt = new WeightMixSignInt(new MixSignInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightMixSignInt);
        reproductions.add(weightMixSignInt);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignInt.getProbability(), name));

        name = "weightRandValueDec";
        WeightRandValueDec weightRandValueDec = new WeightRandValueDec(new RandValueDec(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightRandValueDec);
        reproductions.add(weightRandValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueDec.getProbability(), name));

        name = "weightRandValueInt";
        WeightRandValueInt weightRandValueInt = new WeightRandValueInt(new RandValueInt(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightRandValueInt);
        reproductions.add(weightRandValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueInt.getProbability(), name));

        name = "weightRandValueBin";
        WeightRandValueBin weightRandValueBin = new WeightRandValueBin(new RandValueBin(ParOperationSettings.create(node.get(0).get("mutateEdge").get(name).toString())));
        mutateEdges.put(name, weightRandValueBin);
        reproductions.add(weightRandValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueBin.getProbability(), name));
    }

    private void loadEdgeCross(JsonNode node) throws IOException {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("crossEdge").get("probability").asInt(), "crossEdge"));

        String name = "weightCombineAddDec";
        WeightCombineAddDec weightCombineAddDec = new WeightCombineAddDec(new CombineAddDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineAddDec);
        reproductions.add(weightCombineAddDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineAddDec.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCombineAddInt weightCombineAddInt = new WeightCombineAddInt(new CombineAddInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineAddInt);
        reproductions.add(weightCombineAddInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineAddInt.getProbability(), name));

        name = "weightCombineAnd";
        WeightCombineAnd weightCombineAnd = new WeightCombineAnd(new CombineAnd(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineAnd);
        reproductions.add(weightCombineAnd);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineAnd.getProbability(), name));

        name = "weightCombineOr";
        WeightCombineOr weightCombineOr = new WeightCombineOr(new CombineOr(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineOr);
        reproductions.add(weightCombineOr);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineOr.getProbability(), name));

        name = "weightCombineDiv";
        WeightCombineDiv weightCombineDiv = new WeightCombineDiv(new CombineDiv(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineDiv);
        reproductions.add(weightCombineDiv);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineDiv.getProbability(), name));

        name = "weightCombineMulDec";
        WeightCombineMulDec weightCombineMulDec = new WeightCombineMulDec(new CombineMulDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineMulDec);
        reproductions.add(weightCombineMulDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineMulDec.getProbability(), name));

        name = "weightCombineMulInt";
        WeightCombineMulInt weightCombineMulInt = new WeightCombineMulInt(new CombineMulInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineMulInt);
        reproductions.add(weightCombineMulInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineMulInt.getProbability(), name));

        name = "weightCombineSubDec";
        WeightCombineSubDec weightCombineSubDec = new WeightCombineSubDec(new CombineSubDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineSubDec);
        reproductions.add(weightCombineSubDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineSubDec.getProbability(), name));

        name = "weightCombineSubInt";
        WeightCombineSubInt weightCombineSubInt = new WeightCombineSubInt(new CombineSubInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCombineSubInt);
        reproductions.add(weightCombineSubInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCombineSubInt.getProbability(), name));

        name = "weightCopyValueDec";
        WeightCopyValueDec weightCopyValueDec = new WeightCopyValueDec(new CopyValueDec(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCopyValueDec);
        reproductions.add(weightCopyValueDec);
        bagCrossEdge.addAll(Collections.nCopies(weightCopyValueDec.getProbability(), name));

        name = "weightCopyValueInt";
        WeightCopyValueInt weightCopyValueInt = new WeightCopyValueInt(new CopyValueInt(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
        crossEdges.put(name, weightCopyValueInt);
        reproductions.add(weightCopyValueInt);
        bagCrossEdge.addAll(Collections.nCopies(weightCopyValueInt.getProbability(), name));

        name = "weightCopyValueBin";
        WeightCopyValueBin weightCopyValueBin = new WeightCopyValueBin(new CopyValueBin(ParOperationSettings.create(node.get(0).get("crossEdge").get(name).toString())));
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

        name = "addNode4";
        AddNode4 addNode4 = new AddNode4(factoryNode, factoryCell, factoryEdge, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addNode4);
        reproductions.add(addNode4);
        bagMutateCell.addAll(Collections.nCopies(addNode4.getProbability(), name));

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

        name = "removeEdge";
        RemoveEdge removeEdge = new RemoveEdge(factoryNode, factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, removeEdge);
        reproductions.add(removeEdge);
        bagMutateCell.addAll(Collections.nCopies(removeEdge.getProbability(), name));

        name = "addEdge";
        AddEdge addEdge = new AddEdge(factoryNode, factoryCell, factoryEdge, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, addEdge);
        reproductions.add(addEdge);
        bagMutateCell.addAll(Collections.nCopies(addEdge.getProbability(), name));

        name = "moveSourceEdge";
        MoveSourceEdge moveSourceEdge = new MoveSourceEdge(factoryNode, factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, moveSourceEdge);
        reproductions.add(moveSourceEdge);
        bagMutateCell.addAll(Collections.nCopies(moveSourceEdge.getProbability(), name));

        name = "moveTargetEdge";
        MoveTargetEdge moveTargetEdge = new MoveTargetEdge(factoryNode, factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, moveTargetEdge);
        reproductions.add(moveTargetEdge);
        bagMutateCell.addAll(Collections.nCopies(moveTargetEdge.getProbability(), name));

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

        name = "removeNode2";
        RemoveNode2 removeNode2 = new RemoveNode2(factoryCell, node.get(0).get("mutateCell").get(name).get("probability").asInt());
        mutateCells.put(name, removeNode2);
        reproductions.add(removeNode2);
        bagMutateCell.addAll(Collections.nCopies(removeNode2.getProbability(), name));

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

    private void loadMultipleReproductions(JsonNode node) {
        bagReproductions.addAll(Collections.nCopies(node.get(0).get("multipleReproductions").get("probability").asInt(), "multipleReproductions"));

        multipleReproductions = new MultipleReproductions(
                node.get(0).get("multipleReproductions").get("probability").asInt(),
                new int[]{node.get(0).get("multipleReproductions").get("limits").get(0).asInt(), node.get(0).get("multipleReproductions").get("limits").get(1).asInt()});
        reproductions.add(multipleReproductions);
    }

}

