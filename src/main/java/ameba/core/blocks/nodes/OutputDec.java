package ameba.core.blocks.nodes;

import ameba.core.blocks.Collector;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputDec extends Node implements INode {


    public OutputDec(int minInpEdgesDec, int maxInpEdgesDec) {
        Collector<Double> collector = new Collector<>(minInpEdgesDec, maxInpEdgesDec, 0, 0, this);
        setInpCollectorsDec(new ArrayList<Collector<Double>>(Arrays.asList(collector)));
    }

    @Override
    public void clcNode() {
        if (getInpCollectorsDec().get(0).isSignalReady()) {
            if (getInpCollectorsDec().get(0).getInpEdges().get(0).isSignalSend()) {
                setSignalDec(getInpCollectorsDec().get(0).getInpEdges().get(0).getSignal());
            }
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignalDec(0.0);
    }

    @Override
    public void rstNode() {
        setSignalReady(false);
    }
}