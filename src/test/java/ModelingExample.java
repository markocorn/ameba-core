import ameba.evolution.evolution.Evolution;
import ameba.evolution.incubator.DataEvo;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by marko on 4/10/17.
 */
public class ModelingExample {
    public static void main(String[] args) throws Exception {
        Random rnd = new Random();
        long seed = rnd.nextLong();
        System.out.println("Main started.");

        //Load default training data from resources
        DataEvo dataEvo = DataEvo.create(new String(Files.readAllBytes(Paths.get("engine.json"))));

        //Load default evolution object
        Evolution evolution = Evolution.build(dataEvo, seed);

        //Change cell factory settings for cell generation
        evolution.getFactoryCell().getCellFactorySettings().setNodeInp(2);
        evolution.getFactoryCell().getCellFactorySettings().setNodeOut(2);

        evolution.initRun();

        Thread t = new Thread(evolution);
        t.start();
        Scanner reader = new Scanner(System.in);
        while (true) {
            String cmd = reader.next();
            switch (cmd) {
                case "s":
                    t.interrupt();
                    break;
                case "e":
                    System.out.println("Exit program");
                    System.exit(0);
                    break;
                case "c":
                    System.out.println("Continue execution");
                    t = new Thread(evolution);
                    t.start();
            }
        }
    }
}
