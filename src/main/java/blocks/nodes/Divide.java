//package blocks.nodes;
//
///**
// * Created with IntelliJ IDEA.
// * User: Uporabnik
// * Date: 8.7.2013
// * Time: 10:44
// * To change this template use File | Settings | File Templates.
// */
//
//import blocks.Node;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//
///**
// * @author Marko
// */
//public class Divide extends Node implements Serializable {
//    //Properties
//    //Input edges
//    private ArrayList<Edge> inpEdges = new ArrayList();
//    //Output edges
//    private ArrayList<Edge> outEdges = new ArrayList();
//    //Output value of organelle
//    private double outValue = 0.0;
//    //Flag that indicates that out value has been calculated
//    private boolean outFlg = false;
//    //Flag node calculated
//    private boolean clcFlg = false;
//    private double nodeExpression = 1.0;
//    private int id = 0;
//
//    //Constructor
//    public Divide(double exp) {
//        nodeExpression = exp;
//    }
//
//    //Calculate output value
//    @Override
//    public void clcNode() {
//        if (!clcFlg) {
//            outValue = 0.0;
//            int numInpOrg = 0;
//            if (inpEdges.get(0).getSource().getOutputFlag()) {
//                outValue = inpEdges.get(0).getValue();
//                numInpOrg++;
//            }
//            for (int i = 1; i < inpEdges.size(); i++) {
//                if (inpEdges.get(i).getSource().getOutputFlag()) {
//                    outValue /= inpEdges.get(i).getValue();
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
//    @Override
//    public void addInputEdge(Edge edge) {
//        inpEdges.add(edge);
//    }
//
//    @Override
//    public void addOutputEdge(Edge edge) {
//        outEdges.add(edge);
//    }
//
//    @Override
//    public void removeOutputEdge(Edge edge) {
//        outEdges.remove(edge);
//    }
//
//    @Override
//    public void removeInput(Edge edge) {
//        inpEdges.remove(edge);
//    }
//
//    @Override
//    public void replaceInputEdge(Edge edgeOld, Edge edgeNew) {
//        inpEdges.set(inpEdges.indexOf(edgeOld), edgeNew);
//    }
//
//    @Override
//    public void replaceOutputEdge(Edge edgeOld, Edge edgeNew) {
//        outEdges.set(outEdges.indexOf(edgeOld), edgeNew);
//    }
//
//    @Override
//    public ArrayList<Edge> getInputs() {
//        return inpEdges;
//    }
//
//    @Override
//    public void setInputs(ArrayList<Edge> inpEdgeList) {
//        inpEdges = inpEdgeList;
//    }
//
//    @Override
//    public ArrayList<Edge> getOutputs() {
//        return outEdges;
//    }
//
//    @Override
//    public void setOutputs(ArrayList<Edge> outEdgeList) {
//        outEdges = outEdgeList;
//    }
//
//    @Override
//    public double getOutputValue() {
//        return this.outValue;
//    }
//
//    @Override
//    public boolean getOutputFlag() {
//        return this.outFlg;
//    }
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
//    @Override
//    public boolean getClcFlag() {
//        return this.clcFlg;
//    }
//
//    @Override
//    public boolean conInpAvailable() {
//        return true;
//    }
//
//    @Override
//    public boolean conOutAvailable() {
//        return true;
//    }
//
//    @Override
//    public Object getPar() {
//        return null;
//    }
//
//    @Override
//    public void setPar(Object par) {
//    }
//
//    @Override
//    public Node copy() {
//        Object obj = null;
//        try {
//            // Write the object out to a byte array
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            ObjectOutputStream out = new ObjectOutputStream(bos);
//            out.writeObject(this);
//            out.flush();
//            out.close();
//
//            // Make an input stream from the byte array and read
//            // a copy of the object back in.
//            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
//            obj = in.readObject();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException cnfe) {
//            cnfe.printStackTrace();
//        }
//        return (Node) obj;
//    }
//
//    @Override
//    public double getExpression() {
//        return nodeExpression;
//    }
//
//    @Override
//    public void setExpression(double exp) {
//        nodeExpression = exp;
//    }
//
//    @Override
//    public boolean isMemorable() {
//        return false;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public int getId() {
//        return id;
//    }
//
//    @Override
//    public void setId(int id) {
//        this.id = id;
//    }
//
//    @Override
//    public String getTxt(HashMap<Node, String> states) {
//        String out = "";
//        if (states.containsKey(this)) {
//            out += states.get(this);
//        } else {
//            out += "\\frac{a_{" + id + "}}{";
//            for (int i = 0; i < inpEdges.size(); i++) {
//                out += " " + inpEdges.get(i).getSource().getTxt(states);
//            }
//            out += "}";
//        }
//        return out;
//    }
//
//    @Override
//    public String getStateTxt(HashMap<Node, String> states) {
//        String out = "";
//        out += "\\frac{a_{" + id + "}}{";
//        for (int i = 0; i < inpEdges.size(); i++) {
//            out += " " + inpEdges.get(i).getSource().getTxt(states);
//        }
//        out += "}";
//        return out;
//    }
//
//}
//
