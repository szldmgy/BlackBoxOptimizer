package utils;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Created by peterkiss on 16/08/17.
 */
public class ObjectiveDeserializer implements JsonDeserializer<ObjectiveContainer.Objective>

    {

        @SuppressWarnings("unchecked")
        @Override
        public ObjectiveContainer.Objective deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws
            JsonParseException {
            ObjectiveContainer.Objective o = new Gson().fromJson(jsonElement, ObjectiveContainer.Objective.class);
            ObjectiveContainer.Objective modified = null;
            Object value,target,dummyNull =null;
            if(!o.getType().getCanonicalName().equals(o.getTypeName())) {
                if(o.getTypeName().equals(Integer.class.getCanonicalName())) {
                    value = o.getValue()!=null? ((Number)o.getValue()).intValue() :null;
                    target = o.getTarget()!= null?((Number)o.getTarget()).intValue():null;
                    dummyNull = 0;

                }
                else {
                    value = o.getValue()!=null? ((Number)o.getValue()).floatValue() :null;
                    target = o.getTarget()!= null?((Number)o.getTarget()).floatValue():null;
                    dummyNull = 0f;
                }

                return new ObjectiveContainer.Objective(o.relation, o.isTerminator(), o.getName(), value == null ? null : value, target == null ? target : target, dummyNull, o.getWeight());
            }
            return o;
        }


    }

