package optimizer.objective;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectiveTest {

    String o_int_max = " {\n" +
            "        \"relation\": \"MAXIMIZE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 100.0,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0,\n" +
            "        \"typeName\": \"java.lang.Integer\"\n" +
            "      }";

    String o_float_min = " {\n" +
            "        \"relation\": \"MINIMIZE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 40.0,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0,\n" +
            "        \"typeName\": \"java.lang.Float\"\n" +
            "      }";

    String o_float_lt = " {\n" +
            "        \"relation\": \"LESS_THAN\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50.0,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 15,\n" +
            "        \"typeName\": \"java.lang.Float\"\n" +
            "      }";

    String o_int_gt = " {\n" +
            "        \"relation\": \"GREATER_THAN\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 50,\n" +
            "        \"typeName\": \"java.lang.Integer\"\n" +
            "      }";

    String o_float_maxc = " {\n" +
            "        \"relation\": \"MAXIMIZE_TO_CONVERGENCE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0.01,\n" +
            "        \"typeName\": \"java.lang.Float\"\n" +
            "      }";

    String o_float_minc = " {\n" +
            "        \"relation\": \"MINIMIZE_TO_CONVERGENCE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0.01,\n" +
            "        \"typeName\": \"java.lang.Integer\"\n" +
            "      }";

    public Objective deserializeObjective(String in){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        return gson.fromJson(in, Objective.class);
    }

    @Test
    public void deserialize(){
        Objective<?> o = deserializeObjective(o_int_max);
        assertTrue(o.getRelation().equals(Relation.MAXIMIZE));
        assertTrue(o.getName().equals("true_positives"));
        assertTrue(o.getType().equals(Integer.class));
        assertTrue(Math.abs(o.getWeight()-100f)<0.0001f);

        Objective<?> o1 = deserializeObjective(o_float_min);
        assertTrue(o1.getRelation().equals(Relation.MINIMIZE));
        assertTrue(o1.getName().equals("true_positives"));
        assertTrue(o1.getType().equals(Float.class));
        assertTrue(Math.abs(o1.getWeight()-40f)<0.0001f);

        Objective<?> o2 = deserializeObjective(o_float_lt);
        assertTrue(o2.getRelation().equals(Relation.LESS_THAN));
        assertTrue(o2.getName().equals("true_positives"));
        assertTrue(Math.abs(((Number)o2.getTarget()).floatValue()-50f)>0.0001f);
        assertTrue(o2.getType().equals(Float.class));
        assertTrue(Math.abs(o2.getWeight()-50f)<0.0001f);

        Objective<?> o3 = deserializeObjective(o_int_gt);
        assertTrue(o3.getRelation().equals(Relation.GREATER_THAN));
        assertTrue(o3.getName().equals("true_positives"));
        assertTrue(((Number)o3.getTarget()).intValue()==50);
        assertTrue(o3.getType().equals(Integer.class));
        assertTrue(Math.abs(o2.getWeight()-50f)<0.0001f);


    }
    @Test
    public void cloneTest() throws CloneNotSupportedException {
        Objective<Integer> o = deserializeObjective(o_int_max);
        o.setValue(10);
        Objective o1 = (Objective) o.clone();
        o1.setValue(11);
        assertTrue(!o1.getValue().equals(o.getValue()));


    }

    @Test
    public void equalsTest() throws CloneNotSupportedException {
        Objective<Integer> o = deserializeObjective(o_int_max);
        o.setValue(10);
        Objective o1 = (Objective) o.clone();
        o1.setValue(11);
        assertTrue(o1.equals(o));
    }

    @Test
    public void metTest(){
        Objective<Float> o = deserializeObjective(o_float_maxc);
        o.setValue(10f);
        o.setValue(10f);
        assertTrue(o.met());

        Objective<Float> o1 = deserializeObjective(o_float_minc);
        o1.setValue(10f);
        o1.setValue(10f);
        assertTrue(o.met());

    }


}
