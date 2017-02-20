import ameba.core.blocks.Cell;
import ameba.core.blocks.Edge;
import ameba.core.blocks.Signal;
import ameba.core.blocks.nodes.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by marko on 10/24/16.
 */
public class main2 {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonSettings = mapper.readTree(new File("/home/marko/IdeaProjects/ameba-core/src/main/resources/settings.json"));
//            FactoryEdge edgeFactory = new FactoryEdge(new FactoryEdgeSettings(jsonSettings.get("edgeFactorySettings").toString()));
//            FactoryNode nodeFactory = new FactoryNode(FactoryNodeSettings.genNodeFactorySettingsHashMap(jsonSettings.get("nodeFactorySettings").toString()));
//            FactoryCell cellFactory = new FactoryCell(new FactoryCellSettings(jsonSettings.get("cellFactorySettings").toString()), nodeFactory, edgeFactory);

//            Node node100 = new Input(Signal.createDouble());
//            Node node101 = new Output(Signal.createBoolean());
//            Node node102 = new Compare(Signal.createDouble(),0,10,2);
//            Node node103 = new Input(Signal.createDouble());
////
//            Edge edge100 = new Edge(node100.getOutCollectors().get(0), node102.getInpCollectorsConn().get(0), Signal.createDouble(1.0));
//            Edge edge101 = new Edge(node102.getOutCollectors().get(0), node101.getInpCollectorsConn().get(0), Signal.createBoolean());
//            Edge edge102 = new Edge(node103.getOutCollectors().get(0), node102.getInpCollectorsConn().get(1), Signal.createDouble(1.0));
//
//
////**********************************************************************************************************

            ArrayList<ArrayList<Signal>> inputs = new ArrayList<>();
            Cell cell = new Cell(100);
            int run = 10;
            switch (run) {
                case 0:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(true))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(true))));

                    Node node100 = new Input(Signal.createBoolean());
                    Node node101 = new Output(Signal.createDouble());
                    Node node102 = new Switch2Const(Signal.createDouble(), Signal.createDouble(1.0), new Signal[]{Signal.createDouble(1.0), Signal.createDouble(10.0)}, Signal.createDouble(10.0), new Signal[]{Signal.createDouble(1.0), Signal.createDouble(10.0)});
                    Edge edge100 = new Edge(node100.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createBoolean());
                    Edge edge101 = new Edge(node102.getOutCollectors().get(0), node101.getInpCollectors().get(0), Signal.createDouble(1.0));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    break;
                case 1:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(false), Signal.createDouble(11.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(true), Signal.createDouble(21.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(false), Signal.createDouble(31.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(true), Signal.createDouble(41.0))));

                    node100 = new Input(Signal.createBoolean());
                    node101 = new Input(Signal.createDouble());
                    node102 = new Output(Signal.createDouble());
                    Node node103 = new SwitchConst(Signal.createDouble(), Signal.createDouble(1.0), new Signal[]{Signal.createDouble(1.0), Signal.createDouble(10.0)});
                    edge100 = new Edge(node100.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createBoolean());
                    edge101 = new Edge(node101.getOutCollectors().get(0), node103.getInpCollectors().get(1), Signal.createDouble(1.0));
                    Edge edge102 = new Edge(node103.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createDouble(1.0));


                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    break;
                case 2:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(false), Signal.createDouble(1.0), Signal.createDouble(11.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(true), Signal.createDouble(2.0), Signal.createDouble(21.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(false), Signal.createDouble(3.0), Signal.createDouble(31.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createBoolean(true), Signal.createDouble(4.0), Signal.createDouble(41.0))));

                    node100 = new Input(Signal.createBoolean());
                    node101 = new Input(Signal.createDouble());
                    node102 = new Input(Signal.createDouble());
                    node103 = new Output(Signal.createDouble());
                    Node node104 = new Switch(Signal.createDouble());

                    edge100 = new Edge(node100.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createBoolean());
                    edge101 = new Edge(node101.getOutCollectors().get(0), node104.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge102 = new Edge(node102.getOutCollectors().get(0), node104.getInpCollectors().get(2), Signal.createDouble(1.0));
                    Edge edge103 = new Edge(node104.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createDouble(1.0));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    break;
                case 3:

                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0), Signal.createDouble(2.0), Signal.createDouble(3.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0), Signal.createDouble(2.0), Signal.createDouble(3.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0), Signal.createDouble(2.0), Signal.createDouble(3.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0), Signal.createDouble(2.0), Signal.createDouble(3.0))));

                    node100 = new Input(Signal.createDouble());
                    node101 = new Input(Signal.createDouble());
                    node102 = new Input(Signal.createDouble());
                    node103 = new Output(Signal.createBoolean());
                    node104 = new Interval(Signal.createDouble());

                    edge100 = new Edge(node100.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node101.getOutCollectors().get(0), node104.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge102 = new Edge(node102.getOutCollectors().get(0), node104.getInpCollectors().get(2), Signal.createDouble(1.0));
                    edge103 = new Edge(node104.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createBoolean());

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    break;
                case 4:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0), Signal.createDouble(2.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0), Signal.createDouble(2.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0), Signal.createDouble(2.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0), Signal.createDouble(2.0))));

                    node100 = new Input(Signal.createDouble());
                    node101 = new Input(Signal.createDouble());
                    node102 = new Output(Signal.createBoolean());
                    node103 = new IntervalConst(Signal.createDouble(), Signal.createDouble(3.0), Signal.createDouble(0.0, 4.0));

                    edge100 = new Edge(node100.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node101.getOutCollectors().get(0), node103.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge103 = new Edge(node103.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createBoolean());

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge103);
                    break;
                case 5:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0))));

                    node100 = new Input(Signal.createDouble());
                    node101 = new Output(Signal.createBoolean());
                    node102 = new Interval2Const(Signal.createDouble(), Signal.createDouble(2.0), Signal.createDouble(0.0, 4.0), Signal.createDouble(4.0), Signal.createDouble(0.0, 4.0));

                    edge100 = new Edge(node100.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node102.getOutCollectors().get(0), node101.getInpCollectors().get(0), Signal.createBoolean());

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    break;
                case 6:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0), Signal.createInteger(10), Signal.createBoolean(true))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0), Signal.createInteger(100), Signal.createBoolean(true))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0), Signal.createInteger(1000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(8.0), Signal.createInteger(10000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(8.0), Signal.createInteger(10000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(-8.0), Signal.createInteger(10000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(8.0), Signal.createInteger(10000), Signal.createBoolean(false))));

                    node100 = new Input(Signal.createDouble());
                    node101 = new Input(Signal.createInteger());
                    node102 = new Input(Signal.createBoolean());
                    node103 = new Output(Signal.createDouble());
                    node104 = new Output(Signal.createInteger());
                    Node node105 = new Output(Signal.createBoolean());
                    Node node106 = new Derivative(Signal.createDouble(0.0), Signal.createDouble(1.0), Signal.createDouble(0.0, 4.0));
                    Node node107 = new Derivative(Signal.createInteger(0), Signal.createInteger(1), Signal.createInteger(0, 4));
                    Node node108 = new Derivative(Signal.createBoolean(false), Signal.createBoolean(false), Signal.createBoolean(false, true));

                    edge100 = new Edge(node100.getOutCollectors().get(0), node106.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node106.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge102 = new Edge(node101.getOutCollectors().get(0), node107.getInpCollectors().get(0), Signal.createInteger(1));
                    edge103 = new Edge(node107.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createInteger(1));
                    Edge edge104 = new Edge(node102.getOutCollectors().get(0), node108.getInpCollectors().get(0), Signal.createBoolean(false));
                    Edge edge105 = new Edge(node108.getOutCollectors().get(0), node105.getInpCollectors().get(0), Signal.createBoolean(false));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);
                    cell.addNode(node105);
                    cell.addNode(node106);
                    cell.addNode(node107);
                    cell.addNode(node108);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    cell.addEdge(edge104);
                    cell.addEdge(edge105);
                    break;
                case 7:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0), Signal.createInteger(10), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0), Signal.createInteger(100), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0), Signal.createInteger(1000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(8.0), Signal.createInteger(10000), Signal.createBoolean(true))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(8.0), Signal.createInteger(10000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(-8.0), Signal.createInteger(10000), Signal.createBoolean(false))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(8.0), Signal.createInteger(10000), Signal.createBoolean(false))));

                    node100 = new Input(Signal.createDouble());
                    node101 = new Input(Signal.createInteger());
                    node102 = new Input(Signal.createBoolean());
                    node103 = new Output(Signal.createDouble());
                    node104 = new Output(Signal.createInteger());
                    node105 = new Output(Signal.createBoolean());
                    node106 = new Integral(Signal.createDouble(0.0), Signal.createDouble(1.0), Signal.createDouble(0.0, 4.0));
                    node107 = new Integral(Signal.createInteger(0), Signal.createInteger(1), Signal.createInteger(0, 4));
                    node108 = new Integral(Signal.createBoolean(false), Signal.createBoolean(false), Signal.createBoolean(false, true));

                    edge100 = new Edge(node100.getOutCollectors().get(0), node106.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node106.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge102 = new Edge(node101.getOutCollectors().get(0), node107.getInpCollectors().get(0), Signal.createInteger(1));
                    edge103 = new Edge(node107.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createInteger(1));
                    edge104 = new Edge(node102.getOutCollectors().get(0), node108.getInpCollectors().get(0), Signal.createBoolean(false));
                    edge105 = new Edge(node108.getOutCollectors().get(0), node105.getInpCollectors().get(0), Signal.createBoolean(false));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);
                    cell.addNode(node105);
                    cell.addNode(node106);
                    cell.addNode(node107);
                    cell.addNode(node108);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    cell.addEdge(edge104);
                    cell.addEdge(edge105);
                    break;
                case 8:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0), Signal.createInteger(1))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0), Signal.createInteger(2))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0), Signal.createInteger(3))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0), Signal.createInteger(4))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(5.0), Signal.createInteger(5))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(6.0), Signal.createInteger(6))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(7.0), Signal.createInteger(7))));

                    node100 = new Input(Signal.createDouble());
                    node101 = new Input(Signal.createInteger());
                    node102 = new Output(Signal.createDouble());
                    node103 = new Output(Signal.createInteger());
                    node104 = new Multiply(Signal.createDouble(), 0, 10);
                    node105 = new Multiply(Signal.createInteger(), 0, 10);

                    edge100 = new Edge(node100.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node100.getOutCollectors().get(0), node104.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge102 = new Edge(node104.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge103 = new Edge(node101.getOutCollectors().get(0), node105.getInpCollectors().get(0), Signal.createInteger(1));
                    edge104 = new Edge(node101.getOutCollectors().get(0), node105.getInpCollectors().get(1), Signal.createInteger(1));
                    edge105 = new Edge(node105.getOutCollectors().get(0), node103.getInpCollectors().get(0), Signal.createInteger(1));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);
                    cell.addNode(node105);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    cell.addEdge(edge104);
                    cell.addEdge(edge105);
                    break;
                case 9:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(1.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(2.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(3.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(4.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(5.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(6.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createDouble(7.0))));

                    node100 = new Input(Signal.createDouble());
                    node102 = new Output(Signal.createDouble());
                    node104 = new Divide(Signal.createDouble(), 0, 10);

                    edge100 = new Edge(node100.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge101 = new Edge(node100.getOutCollectors().get(0), node104.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge102 = new Edge(node104.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createDouble(1.0));

                    cell.addNode(node100);
                    cell.addNode(node102);
                    cell.addNode(node104);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);

                    break;
                case 10:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(0), Signal.createDouble(1.0), Signal.createDouble(10.0), Signal.createDouble(100.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(1), Signal.createDouble(2.0), Signal.createDouble(20.0), Signal.createDouble(200.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(1), Signal.createDouble(3.0), Signal.createDouble(30.0), Signal.createDouble(300.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(2), Signal.createDouble(4.0), Signal.createDouble(40.0), Signal.createDouble(400.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(2), Signal.createDouble(5.0), Signal.createDouble(50.0), Signal.createDouble(500.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(1), Signal.createDouble(6.0), Signal.createDouble(60.0), Signal.createDouble(600.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(-9), Signal.createDouble(7.0), Signal.createDouble(70.0), Signal.createDouble(700.0))));

                    node100 = new Input(Signal.createInteger());
                    node101 = new Input(Signal.createDouble());
                    node102 = new Input(Signal.createDouble());
                    node103 = new Input(Signal.createDouble());
                    node104 = new Output(Signal.createDouble());
                    node105 = new Mux(0, 10, Signal.createDouble(0.5), Signal.createDouble(-0.5, 4.0));

                    edge100 = new Edge(node100.getOutCollectors().get(0), node105.getInpCollectors().get(0), Signal.createInteger(1));
                    edge101 = new Edge(node101.getOutCollectors().get(0), node105.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge102 = new Edge(node102.getOutCollectors().get(0), node105.getInpCollectors().get(2), Signal.createDouble(1.0));
                    edge103 = new Edge(node103.getOutCollectors().get(0), node105.getInpCollectors().get(3), Signal.createDouble(1.0));
                    edge104 = new Edge(node105.getOutCollectors().get(0), node104.getInpCollectors().get(0), Signal.createDouble(1.0));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);
                    cell.addNode(node105);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    cell.addEdge(edge104);
                    break;
                case 11:
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(0), Signal.createDouble(1.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(1), Signal.createDouble(2.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(2), Signal.createDouble(3.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(0), Signal.createDouble(4.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(1), Signal.createDouble(5.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(2), Signal.createDouble(6.0))));
                    inputs.add(new ArrayList(Arrays.asList(Signal.createInteger(9), Signal.createDouble(7.0))));

                    node100 = new Input(Signal.createInteger());
                    node101 = new Input(Signal.createDouble());
                    node102 = new Output(Signal.createDouble());
                    node103 = new Output(Signal.createDouble());
                    node104 = new Output(Signal.createDouble());
                    node105 = new DMux(0, 10, Signal.createDouble(0.5), Signal.createDouble(-0.5, 4.0));

                    edge100 = new Edge(node100.getOutCollectors().get(0), node105.getInpCollectors().get(0), Signal.createInteger(1));
                    edge101 = new Edge(node101.getOutCollectors().get(0), node105.getInpCollectors().get(1), Signal.createDouble(1.0));
                    edge102 = new Edge(node105.getOutCollectors().get(0), node102.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge103 = new Edge(node105.getOutCollectors().get(1), node103.getInpCollectors().get(0), Signal.createDouble(1.0));
                    edge104 = new Edge(node105.getOutCollectors().get(2), node104.getInpCollectors().get(0), Signal.createDouble(1.0));

                    cell.addNode(node100);
                    cell.addNode(node101);
                    cell.addNode(node102);
                    cell.addNode(node103);
                    cell.addNode(node104);
                    cell.addNode(node105);

                    cell.addEdge(edge100);
                    cell.addEdge(edge101);
                    cell.addEdge(edge102);
                    cell.addEdge(edge103);
                    cell.addEdge(edge104);
                    break;
                case 12:
                    break;
                case 13:
                    break;
                case 14:
                    break;
                case 15:
                    break;
                case 16:
                    break;
                case 17:
                    break;
                case 18:
                    break;
                case 19:
                    break;

            }
            ArrayList<ArrayList<Signal>> outputs = cell.run(inputs);

            for (ArrayList<Signal> out : outputs) {
                for (Signal obj : out) {
                    System.out.print(obj.getValue());
                    System.out.print(":");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("Program ending");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
