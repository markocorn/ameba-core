package ameba.core.blocks.nodes;

/**
 * Created by marko on 2/21/17.
 */
public class NodeMem extends Node {

    public NodeMem(int[] inpColLimitDec, int[] inpColLimitInt, int[] inpColLimitBin, int[] outColLimitDec, int[] outColLimitInt, int[] outColLimitBin, int paramsDec, int paramsInt, int paramsBin) {
        super(inpColLimitDec, inpColLimitInt, inpColLimitBin, outColLimitDec, outColLimitInt, outColLimitBin, paramsDec, paramsInt, paramsBin);
    }

    @Override
    public void processNode() {
        if (!isSignalClcDone()) {//Optimisation process not call if note is calculated already
            if (isSignalInputsReady()) {
                clcNode();
                setSignalClcDone(true);
            }
        }
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
        setSignalClcDone(false);
    }
}
