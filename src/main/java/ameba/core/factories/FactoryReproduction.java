package ameba.core.factories;

import ameba.core.reproductions.crossCell.ICrossCell;
import ameba.core.reproductions.crossEdge.ICrossEdge;
import ameba.core.reproductions.crossNode.ICrossNode;
import ameba.core.reproductions.mutateCell.IMutateCell;
import ameba.core.reproductions.mutateEdge.*;
import ameba.core.reproductions.mutateNode.IMutateNode;
import ameba.core.reproductions.parametersOperations.RepParSettings;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValue;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSign;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValue;
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
    HashMap<String, IMutateEdge> mutateEdges;
    HashMap<String, IMutateNode> mutateNodes;
    HashMap<String, IMutateCell> mutateCells;
    HashMap<String, ICrossEdge> crossEdges;
    HashMap<String, ICrossNode> crossNodes;
    HashMap<String, ICrossCell> crossCells;

    ArrayList<String> bagMutateEdge;
    ArrayList<String> bagMutateNode;
    ArrayList<String> bagMutateCell;
    ArrayList<String> bagCrossEdge;
    ArrayList<String> bagCrossNode;
    ArrayList<String> bagCrossCell;

    public FactoryReproduction() {
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
        WeightAddValue weightAddValueDec = new WeightAddValue(new AddValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightAddValueDec").toString())));
        mutateEdges.put("weightAddValueDec", weightAddValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueDec.getSettings().getProbability(), "weightAddValueDec"));

        WeightAddValue weightAddValueInt = new WeightAddValue(new AddValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightAddValueInt").toString())));
        mutateEdges.put("weightAddValueInt", weightAddValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueInt.getSettings().getProbability(), "weightAddValueInt"));

        WeightAddValue weightAddValueBin = new WeightAddValue(new AddValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightAddValueBin").toString())));
        mutateEdges.put("weightAddValueBin", weightAddValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueBin.getSettings().getProbability(), "weightAddValueBin"));

        WeightInverse weightInverseDec = new WeightInverse(new InverseValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightInverseDec").toString())));
        mutateEdges.put("weightInverseDec", weightInverseDec);
        bagMutateEdge.addAll(Collections.nCopies(weightInverseDec.getSettings().getProbability(), "weightInverseDec"));

        WeightMixSign weightMixSignDec = new WeightMixSign(new MixSign(RepParSettings.create(node.get(0).get("mutateEdge").get("weightMixSignDec").toString())));
        mutateEdges.put("weightMixSignDec", weightMixSignDec);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignDec.getSettings().getProbability(), "weightMixSignDec"));

        WeightMixSign weightMixSignInt = new WeightMixSign(new MixSign(RepParSettings.create(node.get(0).get("mutateEdge").get("weightMixSignInt").toString())));
        mutateEdges.put("weightMixSignInt", weightMixSignInt);
        bagMutateEdge.addAll(Collections.nCopies(weightMixSignInt.getSettings().getProbability(), "weightMixSignInt"));

        WeightRandValue weightRandValueDec = new WeightRandValue(new RandValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightRandValueDec").toString())));
        mutateEdges.put("weightRandValueDec", weightRandValueDec);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueDec.getSettings().getProbability(), "weightRandValueDec"));

        WeightRandValue weightRandValueInt = new WeightRandValue(new RandValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightRandValueInt").toString())));
        mutateEdges.put("weightRandValueInt", weightRandValueInt);
        bagMutateEdge.addAll(Collections.nCopies(weightRandValueInt.getSettings().getProbability(), "weightRandValueInt"));

        WeightRandValue weightRandValueBin = new WeightRandValue(new RandValue(RepParSettings.create(node.get(0).get("mutateEdge").get("weightRandValueBin").toString())));
        mutateEdges.put("weightRandValueBin", weightRandValueBin);
        bagMutateEdge.addAll(Collections.nCopies(weightAddValueBin.getSettings().getProbability(), "weightRandValueBin"));
    }
}

