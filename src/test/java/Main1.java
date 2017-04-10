import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryReproduction;

/**
 * Created by marko on 4/10/17.
 */
public class Main1 {
    public static void main(String[] args) {
        try {
            FactoryCell factoryCell = FactoryCell.build();
            FactoryReproduction factoryReproduction = FactoryReproduction.build();
            int t = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
