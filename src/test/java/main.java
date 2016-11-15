import ameba.core.Factories.NodeFactory;
import ameba.core.blocks.Cell;
import ameba.core.blocks.Edge;
import ameba.core.blocks.Node;
import ameba.core.blocks.nodes.Delay;
import ameba.core.blocks.nodes.Input;
import ameba.core.blocks.nodes.Output;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) {
        Cell cell = new Cell();
        Node inp = new Input();
        Node out = new Output();
        Node del = new Delay(100, 0.0, 1);


        cell.addNode(new Input());
        cell.addNode(new Output());
        cell.addNode(del);
        cell.addEdge(new Edge(inp, del, 1.0));
        cell.addEdge(new Edge(del, out, 1.0));


        try {
            System.out.println(NodeFactory.toString(new Input()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        int a=10;
        while(a>1){
            System.out.println("ojnwćh");
            a=a-1;

        }
    }
}
