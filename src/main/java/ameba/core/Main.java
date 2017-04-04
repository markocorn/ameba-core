package ameba.core;

import ameba.core.evolution.Evolution;

import java.util.Scanner;

/**
 * Created by marko on 4/4/17.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Ameba started.");
        Evolution evolution = new Evolution("./src/main/resources/settings.json");
        evolution.init();
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
