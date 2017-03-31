package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstBin extends Node {

    public Switch2ConstBin(Boolean par1, Boolean[] par1Limits, Boolean par2, Boolean[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        getParamsBin().add(par1);
        getParamsLimitsBin().add(par1Limits);
        getParamsBin().add(par2);
        getParamsLimitsBin().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceBin().get(0).setSignal(getParamsBin().get(1));
        } else {
            getCollectorsSourceBin().get(0).setSignal(getParamsBin().get(0));
        }
    }
}
