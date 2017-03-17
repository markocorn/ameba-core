package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.NodeMem;

/**
 * @author Marko
 */
public class DerivativeBin extends NodeMem {

    //Initial value
    private boolean initValue;
    private boolean signalOld;

    public DerivativeBin(boolean initial, Boolean par, Boolean[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        this.initValue = initial;
        signalOld = initial;
        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsBin(new Boolean[]{par});
        setParamsLimitsBin(new Boolean[][]{parLimits});
        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceBin().get(0).setSignal(getParamsBin()[0]);
        if (!signalOld == getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceBin().get(0).setSignal(!getParamsBin()[0]);
        }
        signalOld = getCollectorsTargetBin().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        getCollectorsSourceBin().get(0).setSignal(initValue);
        signalOld = initValue;
    }
}