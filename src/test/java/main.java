import ameba.core.evolution.Evolution;

/**
 * Created by marko on 10/24/16.
 */
public class main {
    public static void main(String[] args) throws Exception {
        Evolution evolution = new Evolution("./src/main/resources/settings.json");
        evolution.init();
        evolution.run();
    }
}
