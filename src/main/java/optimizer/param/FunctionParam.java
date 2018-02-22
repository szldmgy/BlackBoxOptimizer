package optimizer.param;

import optimizer.utils.Utils;

import javax.script.ScriptException;

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
    public String getAdditionalInfo(){return this.functionString;}



}
