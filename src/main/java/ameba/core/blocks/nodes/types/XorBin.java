package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

public class XorBin extends Node {

    public XorBin(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetBin(new CollectorTargetBin(this));
        }
        addCollectorSourceBin(new CollectorSourceBin(this));
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSourceBin().get(0).setSignal(true);
        for (int i = 1; i < getCollectorsTargetConnectedBinSim().size(); i++) {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTargetConnectedBinSim().get(i - 1).getSignal() ^ getCollectorsTargetConnectedBinSim().get(i).getSignal());
        }
    }
}

