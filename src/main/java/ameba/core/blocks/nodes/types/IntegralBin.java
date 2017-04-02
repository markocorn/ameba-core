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
public class IntegralBin extends NodeMem {
    private boolean initValue;
    private boolean signalOld;


    public IntegralBin(boolean initial, Boolean par, Boolean[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        this.initValue = initial;
        signalOld = initial;

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        getParamsBin().add(par);
        getParamsLimitsBin().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceBin().get(0).setSignal(!getParamsBin().get(0));
            signalOld = true;
        }
        if (getParamsBin().get(0)) {
            getCollectorsSourceBin().get(0).setSignal(!signalOld);
        } else {
            getCollectorsSourceBin().get(0).setSignal(signalOld);
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        getCollectorsSourceBin().get(0).setSignal(initValue);
        signalOld = initValue;
    }
}