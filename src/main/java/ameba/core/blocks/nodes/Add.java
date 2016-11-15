//package ameba.core.blocks.nodes;
//
//import ameba.core.blocks.Node;
//
//public class Add extends Node {
//
//    public Add(int maxInputEdges, int maxOutputEdges) {
//        super(maxInputEdges, maxOutputEdges, false);
//    }
//
//    //Calculate output value
//    @Override
//    public void clcNode() {
//        if (!clcFlg) {
//            outValue = 0.0;
//            int numInpOrg = 0;
//            for (Edge edge : inpEdges) {
//                if (edge.getSource().getOutputFlag()) {
//                    double ind = 1;
//                    if (inpOrgSign.get(inpEdges.indexOf(edge))) ind = -1;
//                    outValue += edge.getValue() * ind;
//                    numInpOrg++;
//                }
//            }
//            if (numInpOrg == inpEdges.size()) {
//                this.clcFlg = true;
//                this.outFlg = true;
//            } else {
//                this.outValue = 0.0;
//            }
//            outValue = outValue * nodeExpression;
//        }
//    }
//
//
//    @Override
//    public void rstNode() {
//        outFlg = false;
//        clcFlg = false;
//        outValue = 0.0;
//    }
//
//    @Override
//    public void clearNode() {
//        this.rstNode();
//    }
//
//
//}
//
