package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstInt extends Node {

    public Switch2ConstInt(int par1, int[] par1Limits, int par2, int[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceInt(new CollectorSourceInt(this));

        setParamsInt(new int[]{par1});
        setParamsLimitsInt(new int[][]{par1Limits});
        setParamsInt(new int[]{par2});
        setParamsLimitsInt(new int[][]{par2Limits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceInt().get(0).setSignal(getParamsInt()[1]);
        } else {
            getCollectorsSourceInt().get(0).setSignal(getParamsInt()[0]);
        }
    }
}
