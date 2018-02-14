package utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by peterkiss on 16/08/17.
 */
public class ParamDeserializer implements JsonDeserializer<Param> {
    //no handler for array based ranges, they are  string based now
    @SuppressWarnings("unchecked")
    @Override
    public Param deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if(jsonElement.toString().contains("functionString")) {
            FunctionParam fp = new Gson().fromJson(jsonElement, FunctionParam.class);
            // TODO: 2018. 02. 12.  this is a quick fix, should implement decent deserialization for Ranges
            List<ParameterDependency> fixedDeps = new LinkedList<ParameterDependency>();
            for(ParameterDependency pd :fp.getDependencies()){
                Float[] floatArray = new Float[pd.getRangeOfThis().getValueArray().length];
                for (int i = 0 ; i < pd.getRangeOfThis().getValueArray().length; i++)
                {
                    floatArray[i] = ((Number) pd.getRangeOfThis().getValueArray()[i]).floatValue();
                }
                ParameterDependency newpd = new ParameterDependency(floatArray);
                newpd.setP(pd.getP());
                newpd.setRangeOfOther(pd.getRangeOfOther());
                fixedDeps.add(newpd);
            }
            fp.setDependencies(fixedDeps);
            return fp;
        }

        Param p = new Gson().fromJson(jsonElement,Param.class);
        Param modified = p;
       // try {
            //supposed to happen only in case of numeric values
            if(p.getValueTypeName()!=null&&!p.getValueTypeName().equals(p.getParamGenericTypeName())) {
                //Class<?> c = Class.forName(p.getValueTypeName());
                if(p.getValueTypeName().equals(Integer.class.getCanonicalName()))
                    modified = new Param(((Number) p.getValue()).intValue(), ((Number) p.getUpperBound()).intValue(), ((Number) p.getLowerBound()).intValue(), p.getName());
                else
                    modified = new Param(((Number) p.getValue()).floatValue(), ((Number) p.getUpperBound()).floatValue(), ((Number) p.getLowerBound()).floatValue(), p.getName());
                modified.dependencies = new LinkedList();
                for(Object pdo : p.getDependencies()){
                    ParameterDependency pd = (ParameterDependency)pdo;
                    ParameterDependency newPd = pd;
                    if(p.getValueTypeName().equals(Integer.class.getCanonicalName()))
                        newPd = new ParameterDependency(((Number)pd.getRangeOfThis().getLowerBound()).intValue(),((Number)pd.getRangeOfThis().getUpperBound()).intValue(),pd.getP(),pd.getP()==null?null:pd.getRangeOfOther().getLowerBound(),pd.getP()==null?null:pd.getRangeOfOther().getUpperBound());
                    else
                        newPd = new ParameterDependency(((Number)pd.getRangeOfThis().getLowerBound()).floatValue(),((Number)pd.getRangeOfThis().getUpperBound()).floatValue(),pd.getP(),pd.getP()==null?null:pd.getRangeOfOther().getLowerBound(),pd.getP()==null?null:pd.getRangeOfOther().getUpperBound());
                    modified.getDependencies().add(newPd);
                }
                //p = modified;

            }
           /* List<Param.ParameterDependency> pdl = new LinkedList<>();
            for(Object pdo : p.getDependencies()) {
                Param.ParameterDependency pd = (Param.ParameterDependency) pdo;
                Param.ParameterDependency newPd = pd;
                if (pd.getP()!=null&&pd.getP().getValueTypeName() != null && !pd.getP().getValueTypeName().equals(pd.getP().getParamGenericTypeName())) {
                    if (pd.getP().getValueTypeName().equals(Integer.class.getCanonicalName()))
                        newPd = new Param.ParameterDependency((pd.getRangeOfThis().getLowerBound()), (pd.getRangeOfThis().getUpperBound()), pd.getP(), pd.getP() == null ? null : ((Number) pd.getRangeOfOther().getLowerBound()).intValue(), pd.getP() == null ? null : ((Number) pd.getRangeOfOther().getUpperBound()).intValue());
                    else
                        newPd = new Param.ParameterDependency((pd.getRangeOfThis().getLowerBound()), (pd.getRangeOfThis().getUpperBound()), pd.getP(), pd.getP() == null ? null : ((Number) pd.getRangeOfOther().getLowerBound()).floatValue(), pd.getP() == null ? null : ((Number) pd.getRangeOfOther().getUpperBound()).floatValue());
                }
                pdl.add(newPd);
            }

                p.dependencies =  pdl;
     //       }
     //   catch (ClassNotFoundException e) {
     //       e.printStackTrace();
     //   }

        if(p.getParamGenericType() == Double.class){
            if(paramValuesFitInFloat(p)) {
                modified = new Param(((Double) p.getValue()).floatValue(), ((Double) p.getUpperBound()).floatValue(), ((Double) p.getLowerBound()).floatValue(), p.getName());
                modified.dependencies = new LinkedList();
                for(Object pdo : p.getDependencies()){
                    Param.ParameterDependency pd = (Param.ParameterDependency)pdo;
                    Param.ParameterDependency newPd = new Param.ParameterDependency(((Double)pd.getRangeOfThis().getLowerBound()).floatValue(),((Double)pd.getRangeOfThis().getUpperBound()).floatValue(),pd.getP(),pd.getP()==null?null:pd.getRangeOfOther().getLowerBound(),pd.getP()==null?null:pd.getRangeOfOther().getUpperBound());
                    modified.getDependencies().add(newPd);
                }
            }

        }

        return modified==null?p:modified;*/
        return modified;
    }
    public boolean paramValuesFitInFloat(Param p){
        for(Object pd: p.getDependencies()){
            double d = (Double)(((ParameterDependency)pd).getRangeOfThis().getUpperBound());
            if((double)(float)d != d)
                return false;
            d = (Double)(((ParameterDependency)pd).getRangeOfThis().getUpperBound());
            if((double)(float)d != d)
                return false;
        }
        return true;
    }
}
