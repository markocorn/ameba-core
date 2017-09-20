package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.NodeMem;

import java.util.Arrays;

/**
 * @author Marko
 */
public class DelayBin extends NodeMem {
    private int ind;
    private boolean[] buffer;
    //Initial value
    private boolean initValue;

    public DelayBin(boolean initial, Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        this.initValue = initial;
        getParamsInt().add(par);
        getParamsLimitsInt().add(parLimits);

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
        buffer = new boolean[par];
        Arrays.fill(buffer, initValue);
        ind = 0;
        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() {
        buffer[ind] = getCollectorsTargetBin().get(0).getSignal();
        ind++;
        if (ind >= buffer.length) ind = 0;
        if (ind >= buffer.length - 1) {
            getCollectorsSourceBin().get(0).setSignal(buffer[0]);
        } else {
            getCollectorsSourceBin().get(0).setSignal(buffer[ind + 1]);
        }

    }

    @Override
    public void clearNode() {
        rstNode();
        ind = 0;
        buffer = new boolean[getParamsInt().get(0)];
        Arrays.fill(buffer, initValue);
        getCollectorsSourceBin().get(0).setSignal(initValue);
    }
}

