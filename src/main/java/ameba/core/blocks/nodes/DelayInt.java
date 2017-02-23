package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marko
 */
public class DelayInt extends NodeMem {

    private ArrayList<Signal> buffer;
    //Initial value
    private Signal initValue;

    public DelayInt(Signal initial, int par, int[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        this.initValue = initial;
        getParams().add(Signal.createInteger(par));
        getParamsLimits().add(new Signal[]{Signal.createInteger(parLimits[0]), Signal.createInteger(parLimits[1])});

        addInpCollector(new CollectorInp(initial.clone(), this));
        addOutCollector(new CollectorOut(initial.clone(), this));
        buffer = new ArrayList(Collections.nCopies(par, initial));
        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        buffer.add(getInpCollectorsInt().get(0).getSignal());
        buffer.remove(buffer.get(0));
        getOutCollectorsDec().get(0).setSignal(buffer.get(0));
    }

    @Override
    public void clearNode() {
        rstNode();
        buffer = new ArrayList<>(Collections.nCopies(buffer.size(), initValue));
        getOutCollectorsInt().get(0).setSignal(initValue);
    }
}

