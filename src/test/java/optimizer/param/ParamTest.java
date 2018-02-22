package optimizer.param;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.*;
import optimizer.config.TestConfig;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
/**
 * Created by peterkiss on 20/04/17.
 */
public class ParamTest {

    String param_float_single_dep = "{\n" +
            "      \"name\": \"firstParam\",\n" +
            "      \"typeName\": \"java.lang.Float\",\n" +
            "      \"initValue\": 50.0,\n" +
            "      \"dependencies\": [\n" +
            "        {\n" +
            "          \"rangeOfThis\": {\n" +
            "            \"upperBound\": 100.0,\n" +
            "            \"lowerBound\": 0.0\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }";

    String param_list_sinple_dep = "[\n" +
            "    {\n" +
            "      \"name\": \"firstParam\",\n" +
            "      \"typeName\": \"java.lang.Float\",\n" +
            "      \"initValue\": 5.0,\n" +
            "      \"dependencies\": [\n" +
            "        {\n" +
            "          \"rangeOfThis\": {\n" +
            "            \"upperBound\": 20.0,\n" +
            "            \"lowerBound\": 10.0\n" +
            "          },\n" +
            "          \"p\": {\n" +
            "            \"name\": \"secondParam\",\n" +
            "            \"typeName\": \"java.lang.Float\",\n" +
            "            \"initValue\": 12.0,\n" +
            "            \"dependencies\": [\n" +
            "              {\n" +
            "                \"rangeOfThis\": {\n" +
            "                  \"upperBound\": 10.0,\n" +
            "                  \"lowerBound\": 0.0\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          \"rangeOfOther\": {\n" +
            "            \"upperBound\": 2.0,\n" +
            "            \"lowerBound\": 5.0\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"secondParam\",\n" +
            "      \"typeName\": \"java.lang.Float\",\n" +
            "      \"initValue\": 12.0,\n" +
            "      \"dependencies\": [\n" +
            "        {\n" +
            "          \"rangeOfThis\": {\n" +
            "            \"upperBound\": 10.0,\n" +
            "            \"lowerBound\": 0.0\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }]";


    public Param deserializeParam(String in){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        Gson gson = gsonBuilder.create();
        return gson.fromJson(in, Param.class);
    }

    public List<Param> deserializeParamList(String in){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        Type listType = new TypeToken<LinkedList<Param>>(){}.getType();

        Gson gson = gsonBuilder.create();
        return gson.fromJson(in, listType);
    }



    @Test
    public void deserializeTest(){
        Param p = deserializeParam(param_float_single_dep);
        assertTrue(p.getParamGenericType().equals(Float.class));
        assertTrue(p.getName().equals("firstParam"));
        assertTrue(Math.abs(((Number)p.getValue()).floatValue()-50f)<0.0001);

        List<Param> pl = deserializeParamList(param_list_sinple_dep);
       // assertTrue(pl.get(0).getDependencies().size()==2);
        //pl.get(0).getDependencies().get(0)

    }
    @Test
    public void someTest() throws FileNotFoundException {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Rosenbrock_multi_SimulatedAnnealing.json");
        assertTrue(tc.getOptimizerParameters().get(2).getAllValueArray().getClass().getComponentType()==Float.class);
    }
}
