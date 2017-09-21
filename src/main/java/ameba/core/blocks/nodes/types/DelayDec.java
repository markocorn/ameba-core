package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.NodeMem;

import java.util.Arrays;

/**
 * @author Marko
 */
public class DelayDec extends NodeMem {
    private int ind;
    private double[] buffer;
    //Initial value
    private double initValue;

    public DelayDec(double initial, Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});
        this.initValue = initial;
        getParamsInt().add(par);
        if (parLimits[0] < 1) parLimits[0] = 1;
        getParamsLimitsInt().add(parLimits);

        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceDec(new CollectorSourceDec(this));
        buffer = new double[par];
        Arrays.fill(buffer, initValue);
        ind = 0;
        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() {
        buffer[ind] = getCollectorsTargetDec().get(0).getSignal();
        ind++;
        if (ind >= buffer.length) ind = 0;
        if (ind >= buffer.length - 1) {
            getCollectorsSourceDec().get(0).setSignal(buffer[0]);
        } else {
            getCollectorsSourceDec().get(0).setSignal(buffer[ind + 1]);
        }

    }

    @Override
    public void clearNode() {
        rstNode();
        ind = 0;
        buffer = new double[getParamsInt().get(0)];
        Arrays.fill(buffer, initValue);
        getCollectorsSourceDec().get(0).setSignal(initValue);
    }
}

