package ameba.core.blocks.nodes;

/**
 * Created by marko on 2/21/17.
 */
public class NodeMem extends Node implements INode {

    public NodeMem(Integer[] inpColLimitDec, Integer[] inpColLimitInt, Integer[] inpColLimitBin, Integer[] outColLimitDec, Integer[] outColLimitInt, Integer[] outColLimitBin) {
        super(inpColLimitDec, inpColLimitInt, inpColLimitBin, outColLimitDec, outColLimitInt, outColLimitBin);
    }

    @Override
    public void processNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalReady()) {
                    setState(1);
                }
                break;
            case 1:
                if (isSignalSend() || isOutConnected()) {
                    setState(2);
                }
                break;
            case 2:
                if (isSignalInputsReady()) {
                    clcNode();
                    setState(3);
                    setSignalClcDone(true);
                }
                break;
            case 3:
                if (isSignalClcDone()) {
                    setState(4);
                }
                break;
            case 4:
                break;
        }
    }

    @Override
    public void rstNode() {
        setSignalInputsReady(false);
        setSignalReady(true);
        setSignalSend(false);
        setSignalClcDone(false);
        setState(0);
    }
}
