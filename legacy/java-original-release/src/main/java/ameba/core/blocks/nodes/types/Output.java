package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class Output extends Node {

    double value;

    public Output() throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, 0, 0, 0);
        addCollectorTarget(new CollectorTarget(this));
        value = 0.0;
    }

    @Override
    public void clcNode() {
        value = getCollectorsTarget().get(0).getSignal();
//        System.out.print(value+" ");
        if (Double.isNaN(value)) {
            value = 0.0;
        }
        if (value == Double.POSITIVE_INFINITY) {
            value = Double.MAX_VALUE;
        }

        if (value == Double.NEGATIVE_INFINITY) {
            value = Double.MIN_VALUE;
        }
    }

    public double exportSignal() {
        return value;
    }

    @Override
    public void clearNode() {
        super.rstNode();
        clcCollectorsTargetConnected();
        value = 0.0;
    }
}