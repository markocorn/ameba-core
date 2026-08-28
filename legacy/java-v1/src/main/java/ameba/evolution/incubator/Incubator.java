package ameba.evolution.incubator;

import ameba.core.blocks.Cell;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryReproduction;
import ameba.evolution.blocks.StateModel;
import ameba.evolution.fitness.Fitness;
import ameba.evolution.selection.ISelect;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * Created by marko on 3/31/17.
 */
public class Incubator {
    private IncubatorSettings incubatorSettings;
    private ISelect selection;
    private Fitness fitness;
    private ArrayList<Cell> population;
    private FactoryCell factoryCell;
    private FactoryReproduction factoryReproduction;
    private Random random;
    private DataEvo data;
    private StateModel model;


    public Incubator(FactoryCell factoryCell, FactoryReproduction factoryReproduction, IncubatorSettings incubatorSettings, ISelect selection, Fitness fitness, DataEvo data, long seed) {
        this.factoryCell = factoryCell;
        this.factoryReproduction = factoryReproduction;
        this.incubatorSettings = incubatorSettings;
        this.selection = selection;
        this.fitness = fitness;
        this.data = data;
        population = new ArrayList<>();
        this.random = new Random(seed);
    }

    public void populateInitial() throws Exception {
        //Populate population
        int k = 0;
        for (int i = 0; i < incubatorSettings.getPopulationSize(); i++) {
            try {
                population.add(factoryCell.genCellRnd());
            } catch (Exception ex) {
                ex.printStackTrace();
                i--;
                k++;
                if (k > 1000) {
                    throw new Exception("Cell can't be properly connected. Must allow the generation of Constant nodes.");
                }
            }
        }
    }

    public StateModel getModel() {
        return model;
    }

    public void setModel(StateModel model) {
        this.model = model;
    }

    public void simPopulation() {
        if (this.model == null) {
            ArrayList<SimulateCell> sims = new ArrayList<>();
            for (Cell cell : population) {
                sims.add(new SimulateCell(fitness, data, cell, incubatorSettings.getPrefCellSize(), incubatorSettings.getWeightDown(), incubatorSettings.getWeightUp(), incubatorSettings.getWeightEdge()));
            }
            sims.parallelStream().forEach(SimulateCell::simulate);

        } else {
            ArrayList<SimulateCellControl> sims = new ArrayList<>();
            for (Cell cell : population) {
                sims.add(new SimulateCellControl(fitness, data, cell, incubatorSettings.getPrefCellSize(), incubatorSettings.getWeightDown(), incubatorSettings.getWeightUp(), incubatorSettings.getWeightEdge(), model.clone(), incubatorSettings.getWeightError(), incubatorSettings.getWeightActuator(), incubatorSettings.getWeightErrorDiff(), incubatorSettings.getWeightActuatorDiff()));
            }
            sims.parallelStream().forEach(SimulateCellControl::simulate);

        }
    }

    public void simBestPrint(String file) {
        SimulateCellControl sim = new SimulateCellControl(fitness, data, population.get(0), incubatorSettings.getPrefCellSize(), incubatorSettings.getWeightDown(), incubatorSettings.getWeightUp(), incubatorSettings.getWeightEdge(), model, incubatorSettings.getWeightError(), incubatorSettings.getWeightActuator(), incubatorSettings.getWeightErrorDiff(), incubatorSettings.getWeightActuatorDiff());
        String s = sim.simulateToJsonString();
        try {
            PrintWriter out = new PrintWriter(file);
            out.print(s);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sort() {
        Collections.sort(population, Comparator.comparingDouble(Cell::getFitnessValue));
    }

    public void reproduce() throws Exception {
        ArrayList<Cell> chosen = selection.select(population, incubatorSettings.getChosen());
        ArrayList<Cell> newGeneration = new ArrayList<>();
        for (int i = 0; i < incubatorSettings.getEliteSize(); i++) {
            newGeneration.add(population.get(i).clone());
        }

        for (int i = incubatorSettings.getEliteSize(); i < incubatorSettings.getPopulationSize(); i++) {
            try {
                newGeneration.add(factoryReproduction.repCell(chosen.get(random.nextInt(chosen.size())).clone(), chosen.get(random.nextInt(chosen.size()))).clone());
            } catch (Exception ex) {
                i--;
            }
        }
        population = newGeneration;
    }

    public ArrayList<Cell> getPopulation() {
        return population;
    }

    public void setPopulation(ArrayList<Cell> population) {
        this.population = population;
    }

    public IncubatorSettings getIncubatorSettings() {
        return incubatorSettings;
    }

    public void setIncubatorSettings(IncubatorSettings incubatorSettings) {
        this.incubatorSettings = incubatorSettings;
    }

    public ISelect getSelection() {
        return selection;
    }

    public void setSelection(ISelect selection) {
        this.selection = selection;
    }

    public Fitness getFitness() {
        return fitness;
    }

    public void setFitness(Fitness fitness) {
        this.fitness = fitness;
    }

    public FactoryCell getFactoryCell() {
        return factoryCell;
    }

    public void setFactoryCell(FactoryCell factoryCell) {
        this.factoryCell = factoryCell;
    }

    public FactoryReproduction getFactoryReproduction() {
        return factoryReproduction;
    }

    public void setFactoryReproduction(FactoryReproduction factoryReproduction) {
        this.factoryReproduction = factoryReproduction;
    }

    public DataEvo getData() {
        return data;
    }

    public void setData(DataEvo data) {
        this.data = data;
    }
}
