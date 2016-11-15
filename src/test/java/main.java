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
        Node del = new Delay(100, 0.0, 1);


        cell.addNode(inp);
        cell.addNode(out);
        cell.addNode(del);
        cell.addEdge(new Edge(inp, del, 1.0));
        cell.addEdge(new Edge(del, out, 1.0));


        //Simulation
        double[][] inpData = new double[][]{{1.0}, {1.0}, {1.0}, {1.0}, {1.0}, {1.0}, {1.0}, {1.0}, {1.0}, {1.0}};
        double[][] outData = cell.run(inpData);


        for (double[] o : outData) {
            System.out.println(0);
        }
    }
}
