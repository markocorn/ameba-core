package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstInt extends Node {

    public SwitchConstInt(int par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceInt(new CollectorSourceInt(this));

        getParamsInt().add(par);
        getParamsLimitsInt().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        try {
            if (!getCollectorsTargetBin().get(0).getSignal()) {
                getCollectorsSourceInt().get(0).setSignal(getCollectorsTargetInt().get(0).getSignal());
            } else {
                //Be carefully allays initiate get method for inputs to set send flag of input nodes.
                getCollectorsTargetInt().get(0).getSignal();
                getCollectorsSourceInt().get(0).setSignal(getParamsInt().get(0));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
