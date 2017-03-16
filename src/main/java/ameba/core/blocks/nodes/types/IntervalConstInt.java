package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalConstInt extends Node {

    public IntervalConstInt(int par, int[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{2, 2}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsInt(new int[]{par});
        setParamsLimitsInt(new int[][]{parLimits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int[] inputs = new int[]{getCollectorsTargetInt().get(0).getSignal(), getCollectorsTargetInt().get(1).getSignal(), getParamsInt()[0]};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(true);
        } else getCollectorsSourceBin().get(0).setSignal(false);
    }
}
