import ameba.core.blocks.Cell;
import ameba.core.blocks.Edge;
import ameba.core.blocks.Node;
import ameba.core.blocks.nodes.Delay;
import ameba.core.blocks.nodes.Input;
import ameba.core.blocks.nodes.Output;

/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) {
        Cell cell = new Cell();
        Node inp = new Input();
        Node out = new Output();
        Node del = new Delay(100, 0.0, 2);


        cell.addNode(inp);
        cell.addNode(out);
        cell.addNode(del);
        cell.addEdge(new Edge(inp, del, 1.0));
        cell.addEdge(new Edge(del, out, 1.0));


        //Simulation
        double[][] inpData = new double[][]{{1.0}, {2.0}, {3.0}, {4.0}, {5.0}, {6.0}, {7.0}, {8.0}, {9.0}, {10.0}};
        double[][] outData = cell.run(inpData);


        for (double[] o : outData) {
            System.out.println(o[0]);
        }
    }
}
