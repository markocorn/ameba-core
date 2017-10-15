package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class DMuxDec extends Node {

    public DMuxDec(int minOutCollectors, int maxOutCollectors, Double par, Double[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{minOutCollectors, maxOutCollectors}, new int[]{0, 0}, new int[]{0, 0}, 1, 0, 0);

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetDec(new CollectorTargetDec(this));
        for (int i = 0; i < maxOutCollectors; i++) {
            addCollectorSourceDec(new CollectorSourceDec(this));
        }

        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        int ind = getCollectorsTargetInt().get(0).getSignal();
        double value = getCollectorsTargetDec().get(0).getSignal();

        for (int i = 0; i < getCollectorsSourceDec().size(); i++) {
            if (i == ind) {
                getCollectorsSourceDec().get(i).setSignal(value);
            } else {
                getCollectorsSourceDec().get(i).setSignal(getParamsDec().get(0));
            }
        }
    }
}
