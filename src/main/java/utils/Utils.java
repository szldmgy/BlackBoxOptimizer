package utils;


import algorithms.AlgorithmFI;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;


import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by peterkiss on 01/04/17.
 */
public class Utils {

    /**
     * checks whether the to list are exchangable, specifically for checking user defined optimizersetups
     * @param l1
     * @param l2
     * @return true if there are the same parameters of same name and with the same type
     */
    public static boolean correspondingParameterLists(List<Param> l1,List<Param> l2){
        if(l1 == null || l1.isEmpty()) {
            if (l2 == null || l2.isEmpty())
                return true;
            else
                return false;
        }
        else if(l2 == null|| l2.isEmpty())
            return false;
        if(l1.size()!=l2.size())
            return false;
        int numberOfMatches = 0;
        for(Param<?> p: l1) {
            for (Param<?> p2 : l2) {
                if (p.getName().equals(p2.getName())) {
                    // TODO: 2018. 02. 14. Double again at smbo whaaat?? 
                    // if (p.getParamTypeName().equals(p2.getParamTypeName())) {
                        numberOfMatches++;
                        break;
                    //}
                }
            }
        }
        if( numberOfMatches!=l1.size())
            System.out.println("x");
        return numberOfMatches==l1.size();

    }



    public static Float FLOAT_REDEFINED_MAX_VALUE = 100000f;
    public static String getExpCSVFileName(String experimentName,String outputDir) {
        return outputDir+"/"+experimentName+".csv";
    }

    public static String getExpJSONFileName(String experimentName,String experimentDir) {
        return experimentDir +"/"+experimentName+".json";
    }


    public static String getExperimentName(String saveFileNameWithPath){
        String[] fnparts = saveFileNameWithPath.split("/");
        String[] saveFileNameParts = fnparts[fnparts.length-1].split("\\.");
        return saveFileNameParts[0];
    }
    public static String getExperimentUniqueName(String saveFileName,String experimentDir) {
        String experimentName = getExperimentName( saveFileName);
        String experimentFileName=getExpJSONFileName(experimentName,experimentDir);
        if(new File(experimentFileName).exists()) { //here can be a problem
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            experimentFileName.replace(".json","_" + dateFormat.format(new Date())+".json");
        }
        return getExpJSONFileName(getExperimentName(experimentName),experimentDir);
    }
    // TODO: 16/05/17 hacked loading paths, qualified names
    // TODO: 10/08/17 Should be run on server
    public static <T> Map<Class<? extends T>, String> findAllMatchingTypes(Class<T> toFind,String optimizerClassLocation) throws IOException {
        Map<Class<? extends T>,String> foundClasses = new HashMap<Class<? extends T>, String>() ;

        try(final Stream<Path> pathsStream = Files.walk(Paths.get(optimizerClassLocation))) {
            pathsStream.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().contains(".class")){
                        File f = filePath.toFile();

                        // URL url = null;
                        try {
                            URL url = f.toURI().toURL();
                            URL[] urls = new URL[]{url};
                            ClassLoader cl = new URLClassLoader(urls);

                            // Load in the class; MyClass.class should be located in
                            // the directory file:/c:/myclasses/com/mycompany
                            // TODO: 16/05/17 name this is lame... but seems like no better solution
                            //String s1 = f.getName();
                            //String[] s = s1.split("\\.");
                            //Arrays.stream(s).forEach(System.out::println);

                            Class optimizerClass = cl.loadClass("algorithms." + f.getName().split("\\.")[0]);
                            //String retval = Modifier.toString(optimizerClass.getModifiers());
                            // int i = optimizerClass.getModifiers();
                            if(isImplementedAlgorithm( optimizerClass))
                                foundClasses.put(optimizerClass,null);
                        } catch (ClassNotFoundException | MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        }
        findConfigFiles(foundClasses,optimizerClassLocation);

        return foundClasses;
    }

    public static boolean isImplementedAlgorithm(Class optimizerClass){
        if(Modifier.isAbstract(optimizerClass.getModifiers()))
            return false;
        Class C = optimizerClass;
        while (C != null) {
            //System.out.println(C.getName());
            if(C.equals(AlgorithmFI.class))
                return true;
            C = C.getSuperclass();
        }
        return false;
    }

    /**
     *
     * @param foundClasses
     * @param <T>
     */
    public static <T> void findConfigFiles(Map<Class<? extends T>, String> foundClasses,String optimizerClassLocation) {
        Path p = Paths.get(optimizerClassLocation);
        //System.out.println(p);
        if(!Files.exists(p))
            return;
        try(final Stream<Path> pathsStream = Files.walk(p)) {
            pathsStream.forEach( filePath -> {
                        if (Files.isRegularFile(filePath) && filePath.toString().contains(".config")) {
                            Optional<Class<? extends T>> cl =foundClasses.keySet().stream().filter(c -> filePath.toString().equals(c.getName())).findFirst();
                            if(cl.isPresent()) {
                                foundClasses.put(cl.get(), "");
                            }
                        }
                    }
            );

        }
        catch (IOException e) {
            e.printStackTrace();

        }
    }
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
                ret[i] = FLOAT_REDEFINED_MAX_VALUE;
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
