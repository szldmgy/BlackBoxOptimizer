package utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * Created by peterkiss on 16/08/17.
 */
public class ParamDeserializer implements JsonDeserializer<Param> {
    //no handler for array based ranges, they are  string based now
    @SuppressWarnings("unchecked")
    @Override
    public Param deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Param p = new Gson().fromJson(jsonElement,Param.class);
        Param modified = null;
        if(p.getParamGenericType() == Double.class){
            if(paramValuesFitInFloat(p)) {
                modified = new Param(((Double) p.getValue()).floatValue(), ((Double) p.getUpperBound()).floatValue(), ((Double) p.getLowerBound()).floatValue(), p.getName());
                modified.dependencies = new LinkedList();
                for(Object pdo : p.getDependencies()){
                    Param.ParameterDependency pd = (Param.ParameterDependency)pdo;
                    Param.ParameterDependency newPd = new Param.ParameterDependency(((Double)pd.getRangeOfThis().getUpperBound()).floatValue(),((Double)pd.getRangeOfThis().getLowerBound()).floatValue(),pd.getP(),pd.getP()==null?null:pd.getRangeOfOther().getLowerBound(),pd.getP()==null?null:pd.getRangeOfOther().getUpperBound());
                    modified.getDependencies().add(newPd);
                }
            }

        }

        return modified==null?p:modified;
    }
    public boolean paramValuesFitInFloat(Param p){
        for(Object pd: p.getDependencies()){
            double d = (Double)(((Param.ParameterDependency)pd).getRangeOfThis().getUpperBound());
            if((double)(float)d != d)
                return false;
            d = (Double)(((Param.ParameterDependency)pd).getRangeOfThis().getUpperBound());
            if((double)(float)d != d)
                return false;
        }
        return true;
    }
}
