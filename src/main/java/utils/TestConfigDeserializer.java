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
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        gsonBuilder.registerTypeAdapter(ObjectiveContainer.Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        TestConfig t = gson.fromJson(jsonElement, TestConfig.class);
        TestConfig modified = null;
        List<Param> pl = t.getScriptParameters();
       for(Param p : t.getOptimizerParameters()) {
           for (Object pdo : p.getDependencies()) {
               Param.ParameterDependency pd = (Param.ParameterDependency)pdo;
               if(pd.getP()!=null)
                   for(Param p1: t.getOptimizerParameters())
                       if(p1.equals(pd.getP()))
                           pd.setP(p1);
           }
       }
        return t;

    }

}