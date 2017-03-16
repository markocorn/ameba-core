package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchBin extends Node {

    public SwitchBin() throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{3, 3}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTargetBin().get(1).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTargetBin().get(2).getSignal();
        } else {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTargetBin().get(2).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTargetBin().get(1).getSignal();
        }
    }
}
