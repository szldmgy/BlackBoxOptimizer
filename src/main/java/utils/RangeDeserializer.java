package utils;

import com.google.gson.*;

import java.lang.reflect.Type;

public class RangeDeserializer implements JsonDeserializer<Range> {
    //no handler for array based ranges, they are  string based now
    @SuppressWarnings("unchecked")
    @Override
    public Range deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
       /* if(jsonElement.toString().contains("functionString")) {
            return new Gson().fromJson(jsonElement, FunctionParam.class);
        }*/

        Range p = new Gson().fromJson(jsonElement, Range.class);
        if (p.getValueArray() != null) {
               if(p.getValueArray()[0] instanceof Double){
                   Float[] floatArray = new Float[p.getValueArray().length];
                   for (int i = 0 ; i < p.getValueArray().length; i++)
                   {
                       floatArray[i] = ((Number) p.getValueArray()[i]).floatValue();
                   }
                   return new Range(floatArray);
               }

        }
        return p;
    }
}
