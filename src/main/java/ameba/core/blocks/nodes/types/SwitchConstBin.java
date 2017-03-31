package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstBin extends Node {

    public SwitchConstBin(Boolean par, Boolean[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{2, 2}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        getParamsBin().add(par);
        getParamsLimitsBin().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (!getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTargetBin().get(1).getSignal());
        } else {
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTargetBin().get(1).getSignal();
            getCollectorsSourceBin().get(0).setSignal(getParamsBin().get(0));
        }
    }
}
