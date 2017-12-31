package utils;


import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by peterkiss on 01/04/17.
 */
public class Utils {
    public static boolean paramConfigsAreEqual(List<Param> p1,List<Param> p2){
        if(p1.size()!=p2.size())
            return false;
        return !p1.stream()
                .filter(
                        param -> !p2.stream().filter(
                                param1 -> param1.equals(param)&&param.getValue().equals(param1.getValue())
                        ).findFirst().isPresent()
                ).findFirst().isPresent();

    }

    public static Float[] evalFunction(String function, int arrayLength) throws ScriptException {
        Float[] ret = new Float[arrayLength];
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);
        String variablePattern = "\\$[a-zA-Z0-9_]*";

        for(Integer i = 0;i<arrayLength;++i ) {
            String toEval = function.replaceAll(variablePattern, i.toString());
            ret[i] = ((Number) engine.eval(toEval)).floatValue();
        }
//// TODO: 23/09/17 not really efficient
        for(Integer i = 0;i<arrayLength;++i ) {
            if(ret[i].equals(Float.POSITIVE_INFINITY))
                ret[i] = Float.MAX_VALUE;
            else if(ret[i].equals(Float.NEGATIVE_INFINITY))
                ret[i] = Float.MIN_VALUE;
        }
        return ret;
    }
    //// TODO: 21/09/17 a huge hack..
    public static int compareNumbers(Number a, Number b){
        if(a == null && b == null)
            return 0;
        if(a == null)
            return -1;
        if(b==null)
            return 1;
        try {
            return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
        }
        catch (Exception e){
            //// TODO: 20/09/17 hack
            if(a.equals(Float.MAX_VALUE)) return 1;
            if(b.equals(Float.MAX_VALUE)) return -1;

        }
        return 0;

    }

    public static <T extends  Number> boolean comply (T value , T upper, T lower){
        return Utils.compareNumbers(value,upper)<0 && Utils.compareNumbers(lower,value)<0;

    }


    public static <Boolean> boolean comply (Boolean value , Boolean upper, Boolean lower){
        return value.equals(upper);

    }

    public static  boolean comply (String value , String upper, String lower){
        return value.equals(upper);

    }

    // TODO: 26/10/17 unnecesary check
    public static <T> boolean comply (Relation relation, T value, T target,T lastValue){
        /*if(relation.equals(Relation.EQUALS) && value.equals(target))
            return true;*/
        if(value instanceof Number) {
            Double castedValue = ((Number) value).doubleValue();
            Double castedLastValue = ((Number) lastValue).doubleValue();

            Double castedThreshold = ((Number)target).doubleValue();
            int rel = Utils.compareNumbers(castedValue,castedThreshold);
            if (relation.equals(Relation.LESS_THEN) && rel < 0 )
                return true;
            if (relation.equals(Relation.GREATER_THEN) && rel > 0)
                return true;
            if (relation.equals(Relation.MINIMIZE_TO_CONVERGENCE) && Math.abs(castedValue-castedLastValue)<castedThreshold )
                return true;
            if (relation.equals(Relation.MAXIMIZE_TO_CONVERGENCE) && Math.abs(castedValue-castedLastValue)<castedThreshold )
                return true;
        }
        return false;
    }
    public  enum Relation{
        LESS_THEN, GREATER_THEN, /*EQUALS,*/ MINIMIZE,MAXIMIZE,MINIMIZE_TO_CONVERGENCE,MAXIMIZE_TO_CONVERGENCE,;
    }


    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
    public static boolean isFloat(String s)
    {
        try{
            float d = Float.parseFloat(s);
        }
        catch(Exception e){
            return  false;
        }
        return true;
    }
    public static boolean isBoolean(String s)
    {
        if(s.toUpperCase().trim().equals("FALSE")||s.toUpperCase().trim().equals("TRUE"))
            return true;
        return false;
    }
    public static void printParameters(Map<String,Param> params){
        for(Map.Entry<String,Param> entry : params.entrySet())
            System.out.println(entry.toString());

    }


    public static String printLandScape(List<IterationResult> landscape){
        String res = "";
        for(IterationResult ir :landscape){
            res += ir;
        }
        return res;
    }
}
