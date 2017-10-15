package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstInt extends Node {

    public Switch2ConstInt(Integer par1, Integer[] par1Limits, Integer par2, Integer[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, 0, 2, 0);

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceInt(new CollectorSourceInt(this));

        getParamsInt().add(par1);
        getParamsLimitsInt().add(par1Limits);
        getParamsInt().add(par2);
        getParamsLimitsInt().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceInt().get(0).setSignal(getParamsInt().get(1));
        } else {
            getCollectorsSourceInt().get(0).setSignal(getParamsInt().get(0));
        }
    }
}
