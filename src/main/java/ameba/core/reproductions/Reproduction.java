package ameba.core.reproductions;

import java.io.Serializable;

/**
 * Created by marko on 3/10/17.
 */
public class Reproduction implements Serializable {
    Integer probability;

    public Reproduction(Integer probability) {
        this.probability = probability;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

}
