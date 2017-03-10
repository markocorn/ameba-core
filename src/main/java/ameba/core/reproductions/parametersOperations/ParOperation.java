package ameba.core.reproductions.parametersOperations;

/**
 * Created by marko on 3/10/17.
 */
public class ParOperation {
    ParOperationSettings parOperationSettings;

    public ParOperation(ParOperationSettings parOperationSettings) {
        this.parOperationSettings = parOperationSettings;
    }

    public ParOperationSettings getParOperationSettings() {
        return parOperationSettings;
    }

    public void setParOperationSettings(ParOperationSettings parOperationSettings) {
        this.parOperationSettings = parOperationSettings;
    }

    public Double limitDouble(Double par) {
        if (par > getParOperationSettings().getValueLimitDec()[1]) {
            par = getParOperationSettings().getValueLimitDec()[1];
        }
        if (par < getParOperationSettings().getValueLimitDec()[0]) {
            par = getParOperationSettings().getValueLimitDec()[0];
        }
        return par;
    }

    public Integer limitInteger(Integer par) {
        if (par > getParOperationSettings().getValueLimitInt()[1]) {
            par = getParOperationSettings().getValueLimitInt()[1];
        }
        if (par < getParOperationSettings().getValueLimitInt()[0]) {
            par = getParOperationSettings().getValueLimitInt()[0];
        }
        return par;
    }

    public Boolean limitBoolean(Boolean par) {
        if (getParOperationSettings().getValueLimitBin()[1].equals(false)) {
            return false;
        }
        if (getParOperationSettings().getValueLimitBin()[0].equals(true)) {
            return true;
        }
        return par;
    }
}
