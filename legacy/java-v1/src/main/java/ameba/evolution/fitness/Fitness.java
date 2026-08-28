package ameba.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class Fitness implements IFitness {
    Double[] weight;

    public Fitness(Double weight[]) {
        this.weight = weight;
    }

    @Override
    public double clcFitness(double[] results, double[] resultsRef) {
        return 0;
    }
}
