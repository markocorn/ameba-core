package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstInt extends Node {

    public SwitchConstInt(int par, int[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceInt(new CollectorSourceInt(this));

        setParamsInt(new int[]{par});
        setParamsLimitsInt(new int[][]{parLimits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceInt().get(0).setSignal(getCollectorsTargetInt().get(1).getSignal());
        } else {
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTargetInt().get(1).getSignal();
            getCollectorsSourceInt().get(0).setSignal(getParamsInt()[0]);
        }
    }
}
