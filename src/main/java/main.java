import Factories.NodeFactory;
import blocks.Cell;
import blocks.nodes.Delay;
import blocks.nodes.Input;
import blocks.nodes.Output;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.glass.ui.SystemClipboard;

/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) {
        Cell cell = new Cell();
        cell.addInputNode(new Input());
        cell.addOutputNode(new Output());
        cell.addNode(new Delay(1000, 0.0, 1));

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
