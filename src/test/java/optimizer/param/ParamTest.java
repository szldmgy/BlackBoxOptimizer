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
    //String param_list_sinple_dep_Enum,param_list_sinple_dep_Func,

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



   // @Test
    public void deserializeTest(){
        Param p = deserializeParam(param_float_single_dep);
        assertTrue(p.getParamGenericType().equals(Float.class));
        assertTrue(p.getName().equals("firstParam"));
        assertTrue(Math.abs(((Number)p.getValue()).floatValue()-50f)<0.0001);

        List<Param> pl = deserializeParamList(param_list_sinple_dep);
        assertTrue(pl.size()==2);
        assertTrue(pl.get(0).getDependencies().size()==1);
        assertTrue(pl.get(0).getParamGenericType().equals(Float.class));
        assertTrue(pl.get(1).getDependencies().size()==1);
        //assertTrue(((ParameterDependency)pl.get(0).getDependencies().get(0)).getP().equals(pl.get(1)));

    }
   // @Test
    public void functionParamTypeTest() throws FileNotFoundException {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Rosenbrock_multi_SimulatedAnnealing.json");
        assertTrue(tc.getOptimizerParameters().get(2).getAllValueArray().getClass().getComponentType()==Float.class);

    }

   // @Test
    public void dependencyTestFloat()
    {
        Param bounding = new Param(1f,11f,0f,"bounding");
        Param bounding2 = new Param(1f,11f,0f,"bounding2");
        Param bounded = new Param(0f,15f,0f,"bounded");
        bounded.addDependency(10f,13f,bounding,0f,10f);
        Range r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(13f,10f)));
        assertTrue(bounded.isActive());


        bounded.addDependency(11f,13f,bounding2,0f,10f);
        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(13f,11f)));
        assertTrue(bounded.isActive());


        Param bounding3 = new EnumParam("rbf",new String[]{"rbf","linear"}, "kernel"  );
        bounded.addDependency(12f,13f,bounding3,"rbf","rbf");
        bounded.addDependency(12.5f,12.75f,bounding3,"linear","linear");

        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(13f,12f)));
        assertTrue(bounded.isActive());


        bounding3.setInitValue("linear");

        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(12.75f,12.5f)));
        assertTrue(bounded.isActive());







    }


    @Test
    public void dependencyTestEnum()
    {
        Param bounding  =  new Param(0f,11f,0f,"bounding");
        Param bounding2 = new Param(0f,11f,0f,"bounding2");
        Param bounded = new EnumParam("1",new String[]{"1","2","3","4","5"}, "enum"  );
        bounded.addDependency("2","5",bounding,0f,10f);
        Range r = bounded.getActiveRange();
        assertTrue(r.equals(new Range("5","2")));


        bounded.addDependency("3","5",bounding2,0f,10f);
        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range("5","3")));

        Param bounding3 = new EnumParam("rbf",new String[]{"rbf","linear"}, "kernel"  );
        bounded.addDependency("4","5",bounding3,"rbf","rbf");
        bounded.addDependency("3","5",bounding3,"linear","linear");

        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range("5","4")));

        bounding3.setInitValue("linear");

        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range("5","3")));




    }



    @Test
    public void dependencyTestBoolean()
    {
        Param bounding  =  new Param(0f,11f,0f,"bounding");
        Param bounding2 = new Param(0f,11f,0f,"bounding2");
        Param bounded = new Param(true,true,false,"b");
        bounded.addDependency(false,false,bounding,0f,10f);
        Range r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(false,false)));
        assertTrue(bounded.isActive());


        //bounding Param out of its Range -> should not influence range
        bounding.setInitValue(11f);
        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(true,false)));
        assertTrue(bounded.isActive());

        // bounding back to Range
        bounding.setInitValue(10f);


        bounded.addDependency(false, true,bounding2,0f,5f);
        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(false,false)));



        Param bounding3 = new EnumParam("rbf",new String[]{"rbf","linear"}, "kernel"  );
        bounded.addDependency(true,true,bounding3,"rbf","rbf");
        bounded.addDependency(false,false,bounding3,"linear","linear");

        r = bounded.getActiveRange();
        assertTrue(r == null );
        assertTrue( ! bounded.isActive() );
        bounding3.setInitValue("linear");

        r = bounded.getActiveRange();
        assertTrue(r.equals(new Range(false,false)));
        assertTrue(  bounded.isActive() );




    }
  //  @Test
    public void hcTest() throws FileNotFoundException {
        //TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Test/MultiDpendency_Rosenbrock1Test_02-02-2018 11:00:54.json");
        //assertTrue(tc.getOptimizerParameters().get(2).getAllValueArray().getClass().getComponentType()==Float.class);

    }




}
