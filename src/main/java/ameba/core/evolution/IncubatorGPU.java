package ameba.core.evolution;

import ameba.core.blocks.Cell;
import ameba.core.evolution.fitness.Fitness;
import ameba.core.evolution.selections.ISelect;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryReproduction;
import com.aparapi.Kernel;
import com.aparapi.Range;

import java.util.ArrayList;

/**
 * Created by marko on 3. 04. 2017.
 */
public class IncubatorGPU extends Incubator {
    public IncubatorGPU(FactoryCell factoryCell, FactoryReproduction factoryReproduction, IncubatorSettings incubatorSettings, ISelect selection, Fitness fitness) {
        super(factoryCell, factoryReproduction, incubatorSettings, selection, fitness);
    }

    @Override
    public void simPopulation() {
        ArrayList<SimCell> sims = new ArrayList<>();
        for (Cell cell : population) {
            sims.add(new SimCell(fitness, dataDec, dataInt, dataBin, cell, incubatorSettings.getPrefCellSize(), incubatorSettings.getWeightDown(), incubatorSettings.getWeightUp()));
        }
        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int i = getGlobalId();
                sims.get(i).simulate();
            }
        };
        Range range = Range.create(population.size());
        kernel.execute(range);

        sims.parallelStream().forEach(SimCell::fitness);
    }
}
