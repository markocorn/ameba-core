package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstBin extends Node {

    public Switch2ConstBin(boolean par1, boolean[] par1Limits, boolean par2, boolean[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsBin(new boolean[]{par1});
        setParamsLimitsBin(new boolean[][]{par1Limits});
        setParamsBin(new boolean[]{par2});
        setParamsLimitsBin(new boolean[][]{par2Limits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceBin().get(0).setSignal(getParamsBin()[1]);
        } else {
            getCollectorsSourceBin().get(0).setSignal(getParamsBin()[0]);
        }
    }
}
