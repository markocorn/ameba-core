package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstDec extends Node {

    public SwitchConstDec(Double par, Double[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceDec(new CollectorSourceDec(this));

        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceDec().get(0).setSignal(getCollectorsTargetDec().get(0).getSignal());
        } else {
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTargetDec().get(0).getSignal();
            getCollectorsSourceDec().get(0).setSignal(getParamsDec().get(0));
        }
    }
}
