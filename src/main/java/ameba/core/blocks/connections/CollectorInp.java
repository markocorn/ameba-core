package ameba.core.blocks.connections;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorInp extends Collector implements ICollector {

    public CollectorInp(int minEdges, int maxEdges, Node node) {
        super(minEdges, maxEdges, node);
    }

    @Override
    public Object getSignal(Class aClass) {
        if (aClass.isAssignableFrom(Double.class)) {
            Double sum = 0.0;
            for (Edge edge : getEdges()) {
                sum += (Double) edge.getSignal(Double.class);
            }
            return sum;
        }
        if (aClass.isAssignableFrom(Integer.class)) {
            Integer sum = 0;
            for (Edge edge : getEdges()) {
                sum += (Integer) edge.getSignal(Integer.class);
            }
            return sum;
        }
        if (aClass.isAssignableFrom(Boolean.class)) {
            for (Edge edge : getEdges()) {
                if ((Boolean) edge.getSignal(Boolean.class)) {
                    return Boolean.valueOf(true);
                }
            }
            return Boolean.valueOf(false);
        }
        return null;
    }

    @Override
    public boolean isSignalReady() {
        for (Edge edge : getEdges()) {
            if (!edge.isSignalReady()) {
                return false;
            }
        }
        return true;
    }
}
