package ameba.core.reproductions;

public class MultipleReproductions extends Reproduction {
    private int limits[];

    public MultipleReproductions(Integer probability, int[] limits) {
        super(probability);
        this.limits = limits;
    }

    public int[] getLimits() {
        return limits;
    }

    public void setLimits(int[] limits) {
        this.limits = limits;
    }
}
