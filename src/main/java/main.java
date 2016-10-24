import blocks.Cell;
import blocks.nodes.Delay;
import blocks.nodes.Input;
import blocks.nodes.Output;

/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) {
        Cell cell = new Cell();
        cell.addInputNode(new Input());
        cell.addOutputNode(new Output());
        cell.addNode(new Delay(1000, 0.0, 1));
        System.out.println("Hello World");
    }
}
