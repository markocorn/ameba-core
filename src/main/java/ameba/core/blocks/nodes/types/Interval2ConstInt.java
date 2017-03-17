package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Interval2ConstInt extends Node {

    public Interval2ConstInt(Integer par1, Integer[] par1Limits, Integer par2, Integer[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsInt(new Integer[]{par1});
        setParamsLimitsInt(new Integer[][]{par1Limits});
        setParamsInt(new Integer[]{par2});
        setParamsLimitsInt(new Integer[][]{par2Limits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getParamsInt()[0] <= getCollectorsTargetInt().get(0).getSignal() && getCollectorsTargetInt().get(0).getSignal() <= getParamsInt()[1]) {
            getCollectorsSourceBin().get(0).setSignal(true);
        } else getCollectorsSourceBin().get(0).setSignal(false);
    }
}
