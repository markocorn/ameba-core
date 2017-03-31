package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.NodeMem;

/**
 * @author Marko
 */
public class IntegralDec extends NodeMem {
    private double initValue;
    private double signalOld;


    public IntegralDec(double initial, Double par, Double[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});
        this.initValue = initial;
        signalOld = initial;

        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceDec(new CollectorSourceDec(this));

        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(getCollectorsTargetDec().get(0).getSignal() * getParamsDec().get(0) + signalOld);
        signalOld = getCollectorsSourceDec().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getCollectorsSourceDec().get(0).setSignal(initValue);
            signalOld = initValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}