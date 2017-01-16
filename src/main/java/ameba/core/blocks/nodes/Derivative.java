package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

/**
 * @author Marko
 */
public class Derivative extends Node {
    //Old input value
    private double signalOld;
    //Maximum length of signalOld
    private int maxBuffer;
    //Initial value
    private double initValue = 0;

    /**
     * @param minOutputEdges
     * @param maxOutputEdges
     * @param initValue
     * @param parameter
     */
    public Derivative(int minOutputEdges, int maxOutputEdges, double initValue, double parameter) {
        super(1, 1, minOutputEdges, maxOutputEdges);
        this.initValue = initValue;
        this.signalOld = 0.0;
        this.setDecimalParameters(new double[]{parameter});
        setSignal(initValue);
        setSignalReady(true);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInputEdges().size() > 0) {
            //Signal from source node has to be ready and not send.
            if (isNodeClcReady()) {
                setSignal((getInputEdges().get(0).getSignal() - signalOld) / getDecimalParameters()[0]);
                signalOld = getInputEdges().get(0).getSignal();
            }
        }
    }

    @Override
    public void clearNode() {
        signalOld = 0.0;
        rstNode();
        setSignal(initValue);
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
    }
}