package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class DMuxInt extends Node {

    public DMuxInt(int minOutCollectors, int maxOutCollectors, Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{2, 2}, new int[]{0, 0}, new int[]{0, 0}, new int[]{minOutCollectors, maxOutCollectors}, new int[]{0, 0});

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        for (int i = 0; i < maxOutCollectors; i++) {
            addCollectorSourceInt(new CollectorSourceInt(this));
        }

        getParamsInt().add(par);
        getParamsLimitsInt().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        int ind = getCollectorsTargetInt().get(0).getSignal();
        int value = getCollectorsTargetInt().get(0).getSignal();

        for (int i = 0; i < getCollectorsSourceInt().size(); i++) {
            if (i == ind) {
                getCollectorsSourceInt().get(i).setSignal(value);
            } else {
                getCollectorsSourceInt().get(i).setSignal(getParamsInt().get(0));
            }
        }
    }
}
