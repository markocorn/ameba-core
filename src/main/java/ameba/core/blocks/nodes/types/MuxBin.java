package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class MuxBin extends Node {


    public MuxBin(int minInpCollectors, int maxInpCollectors, boolean par, boolean[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetInt(new CollectorTargetInt(this));
        for (int i = 0; i < maxInpCollectors - 1; i++) {
            addCollectorTargetBin(new CollectorTargetBin(this));
        }
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsBin(new boolean[]{par});
        setParamsLimitsBin(new boolean[][]{parLimits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int ind = getCollectorsTargetInt().get(0).getSignal();
        boolean[] inp = new boolean[getCollectorsTargetConnectedBin().size()];
        for (int i = 0; i < getCollectorsTargetConnectedBin().size(); i++) {
            inp[i] = getCollectorsTargetConnectedBin().get(i).getSignal();
        }
        if (ind < inp.length && ind >= 0) {
            getCollectorsSourceBin().get(0).setSignal(inp[ind]);
        } else {
            getCollectorsSourceBin().get(0).setSignal(getParamsBin()[0]);
        }
    }
}
