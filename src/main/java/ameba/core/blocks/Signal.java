package ameba.core.blocks;

import com.rits.cloning.Cloner;

/**
 * Created by marko on 1/30/17.
 */
public class Signal<T> {
    private T value;
    private Class<T> tClass;

    public Signal(Class<T> tClass) throws IllegalAccessException, InstantiationException {
        this.tClass = tClass;
        if (tClass.isAssignableFrom(Double.class)) {
            value = (T) new Double(0.0);
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            value = (T) new Integer(0);
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            value = (T) new Boolean(false);
        }
    }

    public Signal(Class<T> tClass, T value) {
        this.tClass = tClass;
        this.value = value;
    }

    public static Signal createDouble() {
        return new Signal(Double.class, 0.0);
    }

    public static Signal createInteger() {
        return new Signal(Integer.class, 0);
    }

    public static Signal createBoolean() {
        return new Signal(Boolean.class, false);
    }

    public static Signal[] createDouble(double value1, double value2) {
        return new Signal[]{Signal.createDouble(value1), Signal.createDouble(value2)};
    }

    public static Signal[] createInteger(int value1, int value2) {
        return new Signal[]{Signal.createInteger(value1), Signal.createInteger(value2)};
    }

    public static Signal[] createBoolean(boolean value1, boolean value2) {
        return new Signal[]{Signal.createBoolean(value1), Signal.createBoolean(value2)};
    }

    public static Signal createDouble(double value) {
        return new Signal(Double.class, value);
    }

    public static Signal createInteger(int value) {
        return new Signal(Integer.class, value);
    }

    public static Signal createBoolean(boolean value) {
        return new Signal(Boolean.class, value);
    }

    public static Signal mulSignal(Signal signal1, Signal signal2) throws Exception {
        if (signal1.gettClass() != signal2.gettClass())
            throw new Exception("Signal generic types of classes must be same.");
        if (signal1.gettClass().isAssignableFrom(Double.class)) {
            return Signal.createDouble(signal1.getValueDouble() * signal2.getValueDouble());
        }
        if (signal1.gettClass().isAssignableFrom(Integer.class)) {
            return Signal.createInteger(signal1.getValueInteger() * signal2.getValueInteger());
        }
        if (signal1.gettClass().isAssignableFrom(Boolean.class)) {
            if (signal2.getValueBoolean()) {
                return Signal.createBoolean(!signal1.getValueBoolean());
            } else return Signal.createBoolean(signal1.getValueBoolean());
        }
        throw new Exception("Signal generic type has to be Double, Integer, Boolean");
    }

    public static Signal addSignal(Signal signal1, Signal signal2) throws Exception {
        if (signal1.gettClass() != signal2.gettClass())
            throw new Exception("Signal generic types of classes must be same.");
        if (signal1.gettClass().isAssignableFrom(Double.class)) {
            return Signal.createDouble(signal1.getValueDouble() + signal2.getValueDouble());
        }
        if (signal1.gettClass().isAssignableFrom(Integer.class)) {
            return Signal.createInteger(signal1.getValueInteger() + signal2.getValueInteger());
        }
        if (signal1.gettClass().isAssignableFrom(Boolean.class)) {
            throw new Exception("Cand add signals of type boolean");
        }
        throw new Exception("Signal generic type has to be Double, Integer, Boolean");
    }

    public static Signal subSignal(Signal signal1, Signal signal2) throws Exception {
        if (signal1.gettClass() != signal2.gettClass())
            throw new Exception("Signal generic types of classes must be same.");
        if (signal1.gettClass().isAssignableFrom(Double.class)) {
            return Signal.createDouble(signal1.getValueDouble() - signal2.getValueDouble());
        }
        if (signal1.gettClass().isAssignableFrom(Integer.class)) {
            return Signal.createInteger(signal1.getValueInteger() - signal2.getValueInteger());
        }
        if (signal1.gettClass().isAssignableFrom(Boolean.class)) {
            throw new Exception("Can't subtracts signals of type boolean");
        }
        throw new Exception("Signal generic type has to be Double, Integer, Boolean");
    }

    public static Signal divSignal(Signal signal1, Signal signal2) throws Exception {
        if (signal1.gettClass() != signal2.gettClass())
            throw new Exception("Signal generic types of classes must be same.");
        if (signal1.gettClass().isAssignableFrom(Double.class)) {
            return Signal.createDouble(signal1.getValueDouble() / signal2.getValueDouble());
        }
        if (signal1.gettClass().isAssignableFrom(Integer.class)) {
            throw new Exception("Can't divide signals of type integer");
        }
        if (signal1.gettClass().isAssignableFrom(Boolean.class)) {
            throw new Exception("Can't divide signals of type boolean");
        }
        throw new Exception("Signal generic type has to be Double, Integer, Boolean");
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Class<T> gettClass() {
        return tClass;
    }

    public Double getValueDouble() {
        return (Double) value;
    }

    public void setValueDouble(Double signal) {
        this.value = (T) signal;
    }

    public Integer getValueInteger() {
        return (Integer) value;
    }

    public void setValueInteger(Integer signal) {
        this.value = (T) signal;
    }

    public Boolean getValueBoolean() {
        return (Boolean) value;
    }

    public void setValueBoolean(Boolean signal) {
        this.value = (T) signal;
    }

    public Signal clone() {
        return Cloner.standard().deepClone(this);
    }

    public void clear() throws Exception {
        if (tClass.isAssignableFrom(Double.class)) {
            value = (T) new Double(0.0);
        } else if (tClass.isAssignableFrom(Integer.class)) {
            value = (T) new Integer(0);
        } else if (tClass.isAssignableFrom(Boolean.class)) {
            value = (T) new Boolean(false);
        } else throw new Exception("Clear method not implemented for " + tClass.getSimpleName() + " class.");
    }
}
