package ameba.core.blocks.nodes;

/**
 * Created by marko on 2/21/17.
 */
public class NodeMem extends Node {

    public NodeMem(int[] inpColLimitDec, int[] inpColLimitInt, int[] inpColLimitBin, int[] outColLimitDec, int[] outColLimitInt, int[] outColLimitBin, double paramsDec, double[] paramsLimitsDec, int paramsInt, int[] paramsLimitsInt, boolean paramsBin, boolean[] paramsLimitsBin) {
        super(inpColLimitDec, inpColLimitInt, inpColLimitBin, outColLimitDec, outColLimitInt, outColLimitBin, paramsDec, paramsLimitsDec, paramsInt, paramsLimitsInt, paramsBin, paramsLimitsBin);
    }

    @Override
    public void processNode() throws Exception {
        if (isSignalInputsReady()) {
            clcNode();
            setSignalClcDone(true);
        }
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
        setSignalClcDone(false);
    }
}
