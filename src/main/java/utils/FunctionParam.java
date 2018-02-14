package utils;

import javax.script.ScriptException;
import java.util.Arrays;

/**
 * Created by peterkiss on 09/08/17.
 */
public class FunctionParam extends Param<Float> {
    String functionString;
    public FunctionParam(String name,String functionString,int arrayLength) throws ScriptException {
        super(0f, Utils.evalFunction(functionString,arrayLength), name);
        this.functionString = functionString;


    }
    @Override
    public String getParamTypeName(){
        return "Function";
    }
    @Override
    public String getAdditionalInfo(){return this.functionString;};
    /*
    @Override
    public  Float[] getActiveValueArray(){
        return (T[])this.getActiveRange().getValueArray();
    }

    @Override
    public  Float[] getAllValueArray(){
        return Arrays.copyOf(this.dependencies.get(0).rangeOfThis.getValueArray(),this.dependencies.get(0).rangeOfThis.getValueArray();
    }*/


}
