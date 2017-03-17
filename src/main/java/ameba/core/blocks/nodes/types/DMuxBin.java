package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class DMuxBin extends Node {

    public DMuxBin(int minOutCollectors, int maxOutCollectors, Boolean par, Boolean[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{minOutCollectors, maxOutCollectors});

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetBin(new CollectorTargetBin(this));
        for (int i = 0; i < maxOutCollectors; i++) {
            addCollectorSourceBin(new CollectorSourceBin(this));
        }

        setParamsBin(new Boolean[]{par});
        setParamsLimitsBin(new Boolean[][]{parLimits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int ind = getCollectorsTargetInt().get(0).getSignal();
        boolean value = getCollectorsTargetBin().get(0).getSignal();

        for (int i = 0; i < getCollectorsSourceBin().size(); i++) {
            if (i == ind) {
                getCollectorsSourceBin().get(i).setSignal(value);
            } else {
                getCollectorsSourceBin().get(i).setSignal(getParamsBin()[0]);
            }
        }
    }
}
