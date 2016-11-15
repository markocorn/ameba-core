//package ameba.core.blocks.nodes;
//
///**
//* Created with IntelliJ IDEA.
//* User: Uporabnik
//* Date: 8.7.2013
//* Time: 10:45
//* To change this template use File | Settings | File Templates.
//*/
//
//import ameba.core.blocks.Node;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//
///**
//* @author Marko
//*/
//public class PartialDerivative extends Node implements Serializable {
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
//    //Memory buffer
//    private double oldValue1 = 0;
//    private double oldValue2 = 0;
//    //Functional parameter
//    private double nodeExpression = 1.0;
//    private int id=0;
//    //Initial value
//    private double initValue = 0.0;
//
//
//    //Constructor
//    public PartialDerivative(double exp, double intValue) {
//        this.initValue=intValue;
//        nodeExpression = exp;
//        oldValue1 = intValue;
//        oldValue2 = intValue;
//        outFlg = true;
//        outValue = intValue;
//    }
//
//    //Calculate output value
//    @Override
//    public void clcNode() {
//        if (!this.clcFlg) {
//            if (inpEdges.size() > 1) {
//                if (inpEdges.get(0).getSource().getOutputFlag() && inpEdges.get(1).getSource().getOutputFlag()) {
//                    outValue = (inpEdges.get(0).getValue() - oldValue1) / (inpEdges.get(1).getValue() - oldValue2);
//                    oldValue1 = inpEdges.get(0).getValue();
//                    oldValue2 = inpEdges.get(1).getValue();
//                    outValue *= nodeExpression;
//                    clcFlg = true;
//                }
//            } else {
//                this.outValue = initValue * nodeExpression;
//                this.clcFlg = true;
//            }
//        }
//    }
//
//    @Override
//    public void addInputEdge(Edge edge) {
//        if (inpEdges.size() < 2) inpEdges.add(edge);
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
//        return outValue;
//    }
//
//    @Override
//    public boolean getOutputFlag() {
//        return outFlg;
//    }
//
//    @Override
//    public void rstNode() {
//        outFlg = true;
//        clcFlg = false;
//    }
//
//    @Override
//    public void clearNode() {
//        oldValue1 = initValue;
//        oldValue2 = initValue;
//        outValue = initValue * nodeExpression;
//        rstNode();
//
//    }
//
//    @Override
//    public boolean getClcFlag() {
//        return clcFlg;
//    }
//
//    @Override
//    public boolean conInpAvailable() {
//        if (inpEdges.size() > 2) {
//            return false;
//        } else {
//            return true;
//        }
//    }
//
//    @Override
//    public boolean conOutAvailable() {
//        return true;
//    }
//
//    @Override
//    public Object getPar() {
//        return initValue;
//    }
//
//    @Override
//    public void setPar(Object par) {
//        this.initValue = (Double) par;
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
//        return true;  //To change body of implemented methods use File | Settings | File Templates.
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
//        String out = "a_{" + id + "}(\frac{" + states.get(this.getInputs().get(0).getSource()) + " - ";
//        out +=states.get(this.getInputs().get(0).getSource());
//        out = out.substring(0,out.length()-1);
//        out += "-1)}{T_{0}})" ;
//        return  out;
//    }
//
//    @Override
//    public String getStateTxt(HashMap<Node, String> states) {
//        String out = "a_{" + id + "}(\frac{" + states.get(this.getInputs().get(0).getSource()) + " - ";
//        out +=states.get(this.getInputs().get(0).getSource());
//        out = out.substring(0,out.length()-1);
//        out += "-1)}{T_{0}})" ;
//        return  out;
//    }
//}
//
