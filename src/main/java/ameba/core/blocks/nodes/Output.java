package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.Node;

public class Output extends Node {
    /**
     * Construct Output object
     */
    public Output(int minOutputEdges, int maxOutputEdges) {
        super(1, 1, 0, 0);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInputEdges().size() > 0) {
            if (getInputEdges().get(0).isSignalReady() && !getInputEdges().get(0).isSignalSend()) {
                setSignal(getInputEdges().get(0).getSignal());
                setSignalReady(true);
            }
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignal(0.0);
    }

    @Override
    public void rstNode() {
        setSignalReady(false);
    }
}