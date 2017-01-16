package ameba.core.blocks.nodes;

import ameba.core.blocks.Collector;
import ameba.core.blocks.edges.Edge;

import java.util.ArrayList;

public class InputDec extends Node implements INode {
    /**
     * Flag that indicates signal has been imported.
     */
    private boolean signalImported;

    /**
     * Construct InputDec object.
     */

    public InputDec(final int minOutputEdges, final int maxOutputEdges) {
        super(new ArrayList<Collector<? extends Edge>>(), new ArrayList<Collector<? extends Edge>>() {{
            add(new Collector<Edge<Double>>(minOutputEdges, maxOutputEdges));
        }});
        signalImported = false;
    }

    /**
     * Method to set input signal imported from outside of the agent.
     *
     * @param signal Value to be imported.
     */
    public void importSignal(Object signal) {
        if (signal instanceof Double) {
            setSignalDec((Double) signal);
        }
        signalImported = true;
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        if (signalImported) {
            setSignalReady(true);
        }
    }

    /**
     * Reset input node and it's imported signal flag.
     */
    @Override
    public void rstNode() {
        setSignalReady(false);
        signalImported = false;
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignalDec(0.0);
    }


}
