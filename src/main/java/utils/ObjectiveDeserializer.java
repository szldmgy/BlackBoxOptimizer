package utils;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Created by peterkiss on 16/08/17.
 */
public class ObjectiveDeserializer implements JsonDeserializer<ObjectiveContainer.Objective>

    {
        // TODO: 16/08/17 overflow???? 
        //convert to float if target and value fits in its range - maybe should not use easy to go out...
        @SuppressWarnings("unchecked")
        @Override
        public ObjectiveContainer.Objective deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws
            JsonParseException {
            ObjectiveContainer.Objective o = new Gson().fromJson(jsonElement, ObjectiveContainer.Objective.class);
            ObjectiveContainer.Objective modified = null;
            Double value=0.,target=0.;
        if((o.getTarget() != null && o.getTarget().getClass() == Double.class) || (o.getValue()!=null && o.getValue().getClass() == Double.class)){
            if(o.getValue()!=null) {
                value = (Double) (o.getValue());
                if ((double) (float) value.doubleValue() != value.doubleValue())
                    return o;
            }
            else
                value = null;
            if(o.getTarget()!=null) {
                target = (Double) (o.getTarget());
                if ((double) (float) target.doubleValue() != target.doubleValue())
                    return o;
            }
            else
                target=null;
            return new ObjectiveContainer.Objective(o.relation,o.isTerminator(),o.getName(),value==null?null:value.floatValue(),target==null?target:target.floatValue(),null,o.getWeight());
        }
        return o;

    }

}