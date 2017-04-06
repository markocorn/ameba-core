package ameba.core;

import ameba.core.evolution.DataEvo;
import ameba.core.evolution.Evolution;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Created by marko on 4/4/17.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Ameba started.");
        String data = "";
        String settings = "";
        DataEvo dataEvo;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-data")) data = args[i + 1];
            if (args[i].equals("-settings")) settings = args[i + 1];
        }
        if (data.equals("")) {
            throw new Exception("No data specified. Can't perform evolution.");
        } else {
            dataEvo = DataEvo.create(new String(Files.readAllBytes(Paths.get(data))));
        }

        Evolution evolution;
        if (settings.equals("")) {
            System.out.println("No settings specified. Default settings will be loaded.");
            evolution = Evolution.build(dataEvo);
        } else {
            evolution = new Evolution(dataEvo, settings);
            System.out.println("Settings from user loaded");
        }

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
