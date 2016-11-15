//package ameba.core.blocks.nodes;
//
///**
// * Created with IntelliJ IDEA.
// * User: Uporabnik
// * Date: 8.7.2013
// * Time: 10:45
// * To change this template use File | Settings | File Templates.
// */
//
//import ameba.core.blocks.Node;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Random;
//
///**
// * @author Marko
// */
//public class Integral extends Node implements Serializable {
//    //Properties
//    //Random generator
//    private Random rndGen = new Random();
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
//    private double oldValue = 0.0;
//    //Functional parameter
//    private double par;
//    //Expression parameter
//    private double nodeExpression = 1.0;
//    private int id = 0;
//    //Initial value
//    private double initValue = 0.0;
//
//    //Constructor
//    public Integral(double par, double intValue, double exp) {
//        this.initValue = intValue;
//        this.par = par;
//        nodeExpression = exp;
//        oldValue = intValue;
//        outFlg = true;
//        outValue = intValue;
//    }
//
//    //Calculate output value
//    @Override
//    public void clcNode() {
//        if (!this.clcFlg) {
//            if (inpEdges.size() > 0) {
//                if (inpEdges.get(0).getSource().getOutputFlag()) {
//                    outValue = (oldValue + par * inpEdges.get(0).getValue());
//                    oldValue = outValue;
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
//        if (inpEdges.size() == 0) inpEdges.add(edge);
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
//        oldValue = initValue;
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
//        if (inpEdges.size() > 0) {
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
//        if (rndGen.nextDouble() > 0.5) {
//            return par;
//        } else
//            return initValue;
//    }
//
//    @Override
//    public void setPar(Object par) {
//        if (rndGen.nextDouble() > 0.5) {
//            this.initValue = (Double) par;
//        } else
//            this.par = (Double) par;
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
//    public boolean isMemorable() {
//        return true;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void setExpression(double exp) {
//        nodeExpression = exp;
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
//        String out = "a_{" + id + "}(";
//        out += states.get(this.getInputs().get(0).getSource());
//        out = out.substring(0, out.length() - 1);
//        out += "-1) + T_{0}(" + states.get(this.getInputs().get(0).getSource()) + "))";
//        return out;
//    }
//
//    @Override
//    public String getStateTxt(HashMap<Node, String> states) {
//        String out = "a_{" + id + "}(";
//        out += states.get(this.getInputs().get(0).getSource());
//        out = out.substring(0, out.length() - 1);
//        out += "-1) + T_{0}(" + states.get(this.getInputs().get(0).getSource()) + "))";
//        return out;
//    }
//}
//
