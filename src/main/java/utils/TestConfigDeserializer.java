package utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by peterkiss on 2018. 01. 24..
 */
public class TestConfigDeserializer  implements JsonDeserializer<TestConfig>

{
    // TODO: 16/08/17 overflow????
    //convert to float if target and value fits in its range - maybe should not use easy to go out...
    @SuppressWarnings("unchecked")
    @Override
    public TestConfig deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Range.class,new RangeDeserializer());
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        gsonBuilder.registerTypeAdapter(Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        TestConfig t = gson.fromJson(jsonElement, TestConfig.class);

        TestConfig modified = null;
        List<Param> pl = t.getScriptParametersReference();
        if(t.getOptimizerParameters() == null )
            return t;
       for(Param p : t.getOptimizerParameters()) {
           for (Object pdo : p.getDependencies()) {
               ParameterDependency pd = (ParameterDependency)pdo;
               if(pd.getP()!=null)
                   for(Param p1: t.getOptimizerParameters())
                       if(p1.equals(pd.getP()))
                           pd.setP(p1);
           }
       }
        return t;

    }

}