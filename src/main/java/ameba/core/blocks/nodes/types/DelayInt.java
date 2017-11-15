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

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marko
 */
public class DelayInt extends NodeMem {
    private ArrayList<Integer> buffer;
    //Initial value
    private int initValue;

    public DelayInt(int initial, Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, 0, 1, 0);
        this.initValue = initial;
        getParamsInt().add(par);
        if (parLimits[0] < 1) parLimits[0] = 1;
        getParamsLimitsInt().add(parLimits);

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceInt(new CollectorSourceInt(this));
        if (getParamsInt().get(0) < 1) setParamInt(0, 1);
        buffer = new ArrayList<>(Collections.nCopies(getParamsInt().get(0) - 1, initValue));
        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() {
        buffer.add(getCollectorsTargetInt().get(0).getSignal());
        getCollectorsSourceInt().get(0).setSignal(buffer.get(0));
        buffer.remove(0);
    }

    @Override
    public void clearNode() {
        rstNode();
        if (getParamsInt().get(0) < 1) setParamInt(0, 1);
        buffer = new ArrayList<>(Collections.nCopies(getParamsInt().get(0) - 1, initValue));
        getCollectorsSourceInt().get(0).setSignal(initValue);
    }
}

