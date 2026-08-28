package ameba.evolution.blocks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;

strictfp public class StateModel implements Serializable {

    private double[][] A, B, C, D;
    private double[] x0, x;
    private double t, h, h2, h3, h6, d3, d6;
    private double[][] limits;


    public StateModel() {
    }

    public StateModel(double[][] A, double[][] B, double[][] C, double[][] D, double h, double[] x0, double[][] limits) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;
        this.h = h;
        this.x0 = x0;
        this.limits = limits;

        x = x0;
        h2 = h / 2;
        h3 = h / 3;
        h6 = h / 6;
        d3 = 1 / 3;
        d6 = 1 / 6;
    }

    public static StateModel create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, StateModel.class);
    }

    @JsonIgnore
    public static double[] scalarMultiply(double[] array, double scale) {
        double[] out = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            out[i] = (array[i]) * scale;
        }
        return out;
    }

    @JsonIgnore
    public static double[] addArrays(double[] array1, double[] array2) {
        double[] out = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            out[i] = array1[i] + array2[i];
        }
        return out;
    }

    @JsonIgnore
    public static double[] subArrays(double[] array1, double[] array2) {
        double[] out = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            out[i] = array1[i] - array2[i];
        }
        return out;
    }

    @JsonIgnore
    public static double[] addArrays(double[] array1, double[] array2, double[] array3, double[] array4) {
        double[] out = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            out[i] = array1[i] + array2[i] + array3[i] + array4[i];
        }
        return out;
    }

    public void load(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StateModel model = mapper.readValue(json, StateModel.class);
        this.A = model.getA();
        this.B = model.getB();
        this.C = model.getC();
        this.D = model.getD();

    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public double[][] getA() {
        return A;
    }

    public void setA(double[][] a) {
        A = a;
    }

    public double[][] getB() {
        return B;
    }

    public void setB(double[][] b) {
        B = b;
    }

    public double[][] getC() {
        return C;
    }

    public void setC(double[][] c) {
        C = c;
    }

    public double[][] getD() {
        return D;
    }

    public void setD(double[][] d) {
        D = d;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public double[] getX0() {
        return x0;
    }

    public void setX0(double[] x0) {
        this.x0 = x0;
    }

    public double[][] getLimits() {
        return limits;
    }

    public void setLimits(double[][] limits) {
        this.limits = limits;
    }

    @JsonIgnore
    public void rst() {
        x = x0;
        t = 0.0;
    }

    @JsonIgnore
    private double[] deriv(double t, double[] x, double[] u) {
        double[] dx = new double[this.x.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                dx[i] = dx[i] + A[i][j] * x[j];
            }
        }

        for (int i = 0; i < B.length; i++) {
            for (int j = 0; j < B[0].length; j++) {
                dx[i] = dx[i] + B[i][j] * u[j];
            }
        }
        return dx;
    }

    @JsonIgnore
    public double[] stepOde1(double[] u) {
        double[] k1;
        // Step through, updating t
        t += h;
        // Computing all of the trial values
        k1 = deriv(t, x, u);
        x = addArrays(x, scalarMultiply(k1, h));
        return x;
    }

    @JsonIgnore
    public double[] stepOde2(double[] u) {
        double[] k1, k2;
        // Step through, updating t
        t += h;
        // Computing all of the trial values
        k1 = deriv(t, x, u);
        k2 = deriv(t + h2, addArrays(x, scalarMultiply(k1, h2)), u);
        x = addArrays(x, scalarMultiply(k2, h));
        return x;
    }

    @JsonIgnore
    public double[] stepOde4(double[] u) {
        double[] k1, k2, k3, k4;
        // Step through, updating t
        t += h;
        // Computing all of the trial values
        k1 = deriv(t, x, u);
        k2 = deriv(t + h2, addArrays(x, scalarMultiply(k1, h2)), u);
        k3 = deriv(t + h2, addArrays(x, scalarMultiply(k2, h2)), u);
        k4 = deriv(t + h, addArrays(x, scalarMultiply(k3, h)), u);

        x = addArrays(x, scalarMultiply(addArrays(k1, scalarMultiply(k2, 2), scalarMultiply(k3, 2), k4), h));
        return x;
    }

    @JsonIgnore
    public double[] getOutputs(double[] u) {
        double[] y = new double[C.length];
        for (int i = 0; i < C.length; i++) {
            for (int j = 0; j < C[0].length; j++) {
                y[i] = y[i] + C[i][j] * x[j];
            }
        }

        for (int i = 0; i < D.length; i++) {
            for (int j = 0; j < D[0].length; j++) {
                y[i] = y[i] + D[i][j] * u[j];
            }
        }
        return y;
    }

    @JsonIgnore
    public double[] getStates() {
        return x;
    }

    @JsonIgnore
    public StateModel clone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (StateModel) ois.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}
