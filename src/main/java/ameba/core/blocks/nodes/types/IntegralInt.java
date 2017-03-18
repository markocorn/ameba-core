package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.NodeMem;

/**
 * @author Marko
 */
public class IntegralInt extends NodeMem {
    private int initValue;
    private int signalOld;


    public IntegralInt(int initial, Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});
        this.initValue = initial;
        signalOld = initial;

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceInt(new CollectorSourceInt(this));

        getParamsInt().add(par);
        getParamsLimitsInt().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceInt().get(0).setSignal(getCollectorsTargetInt().get(0).getSignal() * getParamsInt().get(0) + signalOld);
        signalOld = getCollectorsSourceInt().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getCollectorsSourceInt().get(0).setSignal(initValue);
            signalOld = initValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}