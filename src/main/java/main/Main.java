package main;

import algorithms.AlgorithmFI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import spark.ModelAndView;
import spark.Request;
import spark.template.velocity.VelocityTemplateEngine;
import utils.*;

import javax.script.ScriptException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.lang.reflect.*;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static spark.Spark.*;


/**
 * Created by peterkiss on 14/10/16.
 *
 *
 */
// TODO: 15/06/17 iterative run: if push optimize -> we should get back to /hello-> command "java" <alg file> <parameters> and give options to ranges,deps, obj-s...
// TODO: 15/06/17 and should write into conf ig file... of th next level


public class Main {

    static Logger logger = Logger.getLogger(Main.class);
    //public Appender fileHandler;
    //Formatter plainText;

    //// TODO: 27/11/17 why we need this shit?? 
    private static Logger getLogger(){
        if(logger == null){
            new Main();
        }
        return logger;
    }
    public static void log(Level level, String msg){
        getLogger().log(level, msg);
        System.out.println(msg);
    }

    // development path
    final static String defaultOptimizerClassLocation =  "target/classes/algorithms/";

    final static String outputfile =  "BlackBoxOptimizer/target/classes/algorithms/";

    //jar path
    final static String defaultJarOptimizerClassLocation =  "lib/algorithms/";

    //final static String defaultJarOptimizerClassLocation =  "/algorithms";

    //availible algorithms
    static Map<Class<? extends AlgorithmFI>,String> optimizerClasses = new HashMap<Class<? extends AlgorithmFI>,String>();
    final static String layout = "templates/layout.vtl";
    final static String resultTemplate = "templates/resultnew.vtl";
    final static String outputDir = "results";
    final static String experimentDir = "experiments";
    final static String backupDir = "backup";

    //commandline usage
    final static boolean  inmediateRun[] = new boolean[1];
    //save state in each iteration
    final static boolean[] safeMode = new boolean[1];
    //use optimizer settings defined in a separated file
    final static boolean[] customParamFile= new boolean[1];
    //name of file storing optimiser params
    final static String[] customParamFileName= new String[1];
    //save in every $savingFrequence iteation
    final static Integer[] savingFrequence = new Integer[]{-1};
    //indicates whether we reloaded the config, if did so we should keep the landscape
    static boolean recoveryMode = false;
    static String[] configFileName = {"examples/Rosenbrock_multi.json"};

    public static String getSaveFileName() {
        return saveFileName[0];
    }

    static String[] saveFileName = {"Rosenbrock_multi"};

    public static void main(String[] args) throws IOException, CloneNotSupportedException {

        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        if(args.length==1)
            configFileName[0] = args[0];
        String projectDir = System.getProperty("user.dir");
        //String staticDir = "/src/main/resources/public";
        String staticDir = "";

        staticFiles.externalLocation(projectDir + staticDir);
         File uploadDir = new File("upload");
        uploadDir.mkdir();
        staticFiles.externalLocation("upload");

        for(int i = 0; i< args.length;i++)
        {
            String s = args[i];
            if(s.equals("-r"))
                inmediateRun[0]=true;
            else if(s.equals("-s"))
                safeMode[0]=true;
            else if(s.equals("-sp")) {
                safeMode[0] = true;
                savingFrequence[0] = Integer.parseInt(args[++i]);
            }
            else if(s.equals("-p")) {
                customParamFile[0] = true;
                customParamFileName[0] = args[i++];
            }
            else
                configFileName[0] = s;
        }


        final TestConfig[] config = new TestConfig[1];
        String[] algorithmName = new String[1];


        // TODO: 10/08/17 CALL TO SERVER 
        //find all availible optimizer algorithms
        optimizerClasses = findAllMatchingTypes(AlgorithmFI.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);

        //load config from json file
        if (configFileName[0].contains(".json")) {
            config[0] = readConfigJSON(configFileName[0]);

            if(inmediateRun[0])
            {
                try {
                    // TODO: 10/08/17 CALL SERVER 
                    runOptimizer(config[0],safeMode[0],saveFileName[0]);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                return;
            }


        }


        staticFileLocation("/public");
        List<String> classList=  Arrays.asList(Integer.class.getName(),Float.class.getName(),Boolean.class.getName(),"Enum","Function");
        List<String> objTypes=  Arrays.asList(Integer.class.getName(),Float.class.getName()/*,Boolean.class.getName(),"Enum","Function"*/);

        final List<Param> p = config[0].getScriptParameters();
        final String command = config[0].getBaseCommand();
        final String[] parameternames = config[0].getScriptParameters().stream().map(par->par.getName()).toArray(String[]::new);
        final List<ObjectiveContainer.Objective> objectives =  config[0].getObjectiveContainer().getObjectives();

        final String[] objectiveTypes = Arrays.stream(Utils.Relation.values()).map(v->v.toString()).toArray(String[]::new);




        final String[] algoritmhs =  optimizerClasses.keySet().stream().map(a->a.getName()).toArray(String[]::new);


        post("/loadsetup","multipart/form-data",(req,res)->{
// TODO: 28/08/17 check if exists
          //  Part fn = req.raw().getPart("cfn");


            config[0] = new TestConfig();
           // if(fn!=null&&!fn.equals("")) {
               /* fn = fn.replace("C:\\fakepath\\",experimentDir+"/" );
                fn = fn.replace("C:/fakepath/",experimentDir+"/" );
                File f= new File(fn);
                if(!f.exists())
                    fn = fn.replace(experimentDir,backupDir);
                if(new File(fn).exists())
                    config[0] =  readConfigJSON(fn);*/



            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
            //String s = req.queryParams("chosenfile");
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            Part filePart = req.raw().getPart("chosenfile");
            Part fn = req.raw().getPart("cfn");
            configFileName[0] = filePart.getSubmittedFileName();
            //prepare base of the savefilename
            String [] fnparts =  configFileName[0].split("/");
            saveFileName[0] =fnparts[fnparts.length-1].replace(".json","");
            try (InputStream input = filePart.getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            if(tempFile.toFile().length()!=0)
                config[0] =  readConfigJSON(tempFile.toFile());
            Files.delete(tempFile);
          //  }
            Map<String, Object> model = getBBSetupModel(saveFileName[0],config, classList, objectiveTypes, algoritmhs,objTypes);


            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, layout)
            );
        });


        get("/results", (req, res) ->{
            config[0] =  readConfigJSON("test.json");

            Map<String, Object> model1 = getResultModel(objectives, "experiment.csv",saveFileName[0]);


            return new VelocityTemplateEngine().render(
                    new ModelAndView(model1, layout)
            );
        });

        get("/stop", (req, res) ->{


            Map<String, Object> model = getGoodBye();

               stop();
            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, layout)
            );
        });

        get("/hello", (req, res) ->{


            Map<String, Object> model = getBBSetupModel(saveFileName[0],config, classList, objectiveTypes, algoritmhs,objTypes);


            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, layout)
            );
        });

        final List<Param> paramsList = config[0].getScriptParameters();
        Type listOfTestObject = new TypeToken<List<String>>(){}.getType();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        get("/paramlist", (req, res) ->
                gson.toJson(paramsList.stream().map(e -> e.getName()).toArray())
        );

        /*post("/tasks", (request, response) -> {
            Map<String, Object> model = new HashMap<String, Object>();

            String description = request.queryParams("description");
            return new ModelAndView(new HashMap<>(), layout);
        }, new VelocityTemplateEngine());*/

        post("/updateconfig", (request, response) -> {
            Map<String, Object> model = new HashMap<String, Object>();
            String safeModeString = request.queryParams("safe_mode");

            if(safeModeString != null) {
                savingFrequence[0] = Integer.parseInt(request.queryParams("frequency"));
                safeMode[0] = true;
            }

            //String algorithmname = request.queryParams("algorithm_names");
            algorithmName[0] = config[0].getAlgorithmName();
            saveFileName[0] = request.queryParams("savefilename");
            String useIterationString = request.queryParams("use_iterations");
            String iterationCountString = request.queryParams("iterationCount");
            String command_input = request.queryParams("commandinput");

            int counter = 0;
            List<IterationResult> landscape = new LinkedList<IterationResult>();

            //setup previous or predefined optimizer algorithms it there is any
            List<Param> predefinedOptimizerParams = config[0].getOptimizerParameters();


            if(config[0].getLandscape().size()>0) {
                recoveryMode = true;
                landscape = config[0].getLandscape();
                counter = config[0].getIterationCounter();

            }
            else
                recoveryMode = false;
            TestConfig c = new TestConfig();
            c.setAlgorithmName(algorithmName[0]);
            // in case we have htem in the same file
            c.setOptimizerParameters(predefinedOptimizerParams);
            c.setBaseCommand(command_input);
            c.setIterationCounter(config[0].getIterationCounter());
            if(useIterationString != null)
                c.setIterationCount(Optional.of(Integer.parseInt(iterationCountString)));


            String objFileName1 = request.queryParams("objFileName");
// TODO: 23/07/17 what about other predefined lists???
            ObjectiveContainer objectiveContainer = readObjectives(request);
            objectives.clear();
            objectives.addAll(objectiveContainer.getObjectives());

            c.setObjectiveContainer(objectiveContainer);
            String usefileStr = request.queryParams("use_file_output");
            if(usefileStr!=null)
                //String objFileName = request.queryParams("objFileBrowser");
                c.setObjectiveFileName(objFileName1);
            c.setLandscape(landscape);
            c.setIterationCounter(counter);

            List<Param> paramList = null;
            try{
                paramList = readParams(request,null);
            }catch(Exception e){
                e.printStackTrace();
            }



            c.setScriptParameters(paramList);
            /*try (Writer writer = new FileWriter("test.json")) {
                Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
                gson1.toJson(c, writer);
            }*/
            config[0]= c;
            Map<String,List<Param>> algParamMap = new HashMap<String, List<Param>>();
            optimizerClasses.forEach( (optimizerClass, configfile ) ->{
                try {
                    Object algorithmObj = optimizerClass.newInstance();

                    Method setConfig= optimizerClass.getMethod("setConfiguration",TestConfig.class);
                    setConfig.invoke(algorithmObj,config[0]);

                    /*Method setConfigFromFile= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
                    setConfig.invoke(algorithmObj,"test.json");*/

                    Method setParams= optimizerClass.getMethod("updateConfigFromAlgorithmParams",List.class);
                    setParams.invoke(algorithmObj,config[0].getScriptParameters());

                    Method getConfig= optimizerClass.getMethod("getConfig");
                    Object o = getConfig.invoke(algorithmObj);
                    List<Param> pl = (List<Param>)o;

                    Method isApplyableMethod= optimizerClass.getMethod("isApplyableForParams");
                    if((Boolean)isApplyableMethod.invoke(algorithmObj))
                        algParamMap.put(optimizerClass.getSimpleName(),(List<Param>)o);


                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            if(!algParamMap.keySet().contains(config[0].getAlgorithmName()))
                config[0].setAlgorithmName(algParamMap.keySet().iterator().next());
            //we set up values for optimizerparam from configfile
            List<Param> pl = algParamMap.get(config[0].getAlgorithmName());
            if(pl!=null && config[0].getOptimizerParameters()!=null) // for algorithm without parameters
            for(Param paramInMap : pl){
                for(Param loadedParam : config[0].getOptimizerParameters())
                    if(paramInMap.equals(loadedParam))
                        paramInMap = loadedParam;

            }


            Map<String, Object> model1 = new HashMap<>();


            model1.put("algorithmname",config[0].getAlgorithmName());
            model1.put("filename",saveFileName[0]);
            model1.put("template","templates/algorithm.vtl");
            model1.put("algParamMap",algParamMap);
            model1.put("parametertypes",classList);

            return new ModelAndView(model1, layout);
        }, new VelocityTemplateEngine());

        post("/updatealgorithmconfig", (request, response) -> {
            Map<String, Object> model = new HashMap<String, Object>();

            request.queryParams().stream().forEach(System.out::println);

            String algorithmname = request.queryParams("algorithm_names");
            algorithmName[0] = algorithmname;
            String useIterationString = request.queryParams("use_iterations");

            String iterationCountString = request.queryParams("iterationCount");
            String command_input = request.queryParams("commandinput");

            TestConfig c = new TestConfig();
            c.setAlgorithmName(algorithmname);
            c.setBaseCommand(command_input);
            c.setIterationCounter(config[0].getIterationCounter());

            if(useIterationString != null)
                c.setIterationCount(Optional.of(Integer.parseInt(iterationCountString)));


            String objFileName1 = request.queryParams("objFileName");

            ObjectiveContainer objectiveContainer = readObjectives(request);

            c.setObjectiveContainer(objectiveContainer);
            c.setObjectiveFileName(objFileName1);
            if(recoveryMode) {
                c.setLandscape(config[0].getLandscape());
                c.setIterationCounter(config[0].getIterationCounter());
            }
            List<Param> paramList = null;
            try{
                paramList = readParams(request,algorithmname);
            }catch(Exception e){
                e.printStackTrace();
            }


            // TODO: 10/08/17 send parameters to SERVER 
            c.setScriptParameters(paramList);
            try (Writer writer = new FileWriter("test_"+algorithmname+"_conf.json")) {
                Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
                gson1.toJson(c, writer);
            }
            config[0]= c;
            Map<String,List<Param>> algParamMap = new HashMap<String, List<Param>>();
            // TODO: 10/08/17 SERVER SENDS BACK  algorithm configs
            optimizerClasses.forEach( (optimizerClass, configfile ) ->{
                try {
                    Object algorithmObj = optimizerClass.newInstance();

                    Method setConfig= optimizerClass.getMethod("setConfiguration",TestConfig.class);
                    setConfig.invoke(algorithmObj,config[0]);

                    /*Method setConfig= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
                    setConfig.invoke(algorithmObj,"test.json");*/


                    Method setParams= optimizerClass.getMethod("setOptimizerParams",List.class);
                    setParams.invoke(algorithmObj,config[0].getScriptParameters());

                    Method getConfig= optimizerClass.getMethod("getConfig");
                    Object o = getConfig.invoke(algorithmObj);
                    algParamMap.put(optimizerClass.getSimpleName(),(List<Param>)o);


                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            Map<String, Object> model1 = new HashMap<>();


            model1.put("filename",saveFileName[0]);
            model1.put("template","templates/algorithm.vtl");
            model1.put("algParamMap",algParamMap);
            model1.put("parametertypes",classList);

            return new ModelAndView(model1, layout);
        }, new VelocityTemplateEngine());

        post("/run", (request, response) ->{



            String algorithmname = request.queryParams("algorithm_names");

            List<Param> lp = readParams(request,algorithmname);

// TODO: 10/08/17 SEND OPT PARAMETERS TO SERVER 
            //to save the config


            config[0].setAlgorithmName(algorithmname);
            config[0].setOptimizerParameters(lp);



            /*String resFileName = "experiment.csv";
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String resFileNameNew  =  algorithmname+"_"+dateFormat.format(new Date());÷/
            /*File[] files = new File(outputDir+"/").listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    System.out.println("Directory: " + file.getName());
                   // showFiles(file.listFiles()); // Calls same method again.
                } else {
                    System.out.println("File: " + file.getName());
                }
            }*/
            //if(saveFileName[0].equals("default"))
            String experimentName = saveFileName[0];
            if(new File(experimentDir +"/"+experimentName+".json").exists()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

                experimentName += "_" + dateFormat.format(new Date());
            }
            String resFileName =  outputDir+"/"+experimentName+".csv";
            String expFileName = "experiment.json";
            expFileName = experimentDir +"/"+experimentName+".json";

            File directory = new File(outputDir);
            if (! directory.exists()){
                directory.mkdir();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }
            directory = new File(experimentDir);
            if (! directory.exists()){
                directory.mkdir();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }
            directory = new File(backupDir);
            if (! directory.exists()){
                directory.mkdir();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }

            try (Writer writer = new FileWriter(expFileName)) {
                List<IterationResult> ls = config[0].getLandscape();
                config[0].setLandscape(null);
                gson.toJson(config[0], writer);
                config[0].setLandscape(ls);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            String result = runOptimizer(config[0],algorithmname, lp, safeMode[0],resFileName);
            String resultFilePath =  /*projectDir + "/BlackBoxOptimizer"+staticDir+"/"+*/resFileName;
            try (Writer writer = new FileWriter(resultFilePath)) {
                writer.write(result);
            }


            Map<String, Object> model1 = getResultModel(objectives, resFileName,saveFileName[0]);


            //VelocityEngine engine = new VelocityEngine();
            return new VelocityTemplateEngine().render(
                    new ModelAndView(model1, layout)
            );
        });

    }

    private static Map<String,Object> getGoodBye() {
        return new HashMap<>();
    }

    private static Map<String, Object> getBBSetupModel(String file,TestConfig[] config, List<String> classList, String[] objectiveTypes, String[] algoritmhs,List<String> objtypes) throws CloneNotSupportedException {
        Map<String, Object> model = new HashMap<>();
        model.put("filename",file);
        model.put("parametertypes",classList);
        model.put("template","templates/param.vtl");
        model.put("paramlist",config[0].getScriptParameters());
        model.put("command",config[0].getBaseCommand());
        model.put("objlist",config[0].getObjectiveContainer().getObjectives());
        model.put("objectivetypes", objectiveTypes);
        model.put("parameternames",config[0].getScriptParameters().stream().map(par->par.getName()).toArray(String[]::new));
        model.put("algorithms",algoritmhs);
        model.put("objtypes",objtypes);
        model.put("obj_filename", config[0].getObjectiveFileName());
        model.put("iteration_count", config[0].getIterationCount().isPresent()?config[0].getIterationCount().get():0);
        return model;
    }

    private static TestConfig readConfigJSON(String configFileName) throws FileNotFoundException {
        File f = new File(configFileName);
        return readConfigJSON(f);
    }

    private static TestConfig readConfigJSON(File f) throws FileNotFoundException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        gsonBuilder.registerTypeAdapter(ObjectiveContainer.Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        JsonReader reader = new JsonReader(new FileReader(f));
        return gson.fromJson(reader, TestConfig.class);
    }

    private static Map<String, Object> getResultModel(List<ObjectiveContainer.Objective> objectives, String resFileName, String configFileName) {
        final List<String> objectiveList = new LinkedList<String>();
        objectives.forEach(o->objectiveList.add(o.getName()));

        final List<String> objectiveRelationList = new LinkedList<String>();
        objectives.forEach(o->objectiveRelationList.add(o.getRelation().equals(Utils.Relation.GREATER_THEN)||o.getRelation().equals(Utils.Relation.MAXIMIZE)?"increase":"decrease"));

        // TODO: 10/08/17 landscape arrived from server


        Map<String, Object> model1 = new HashMap<>();
        model1.put("template", "templates/resultnew.vtl");
        //String resultFilePath =  projectDir + "/BlackBoxOptimizer"+staticDir+"/"+resFileName;
        String[] resFileList = null;
        String[] setupFileList = null;
        try {
                resFileList =Files.list(Paths.get(outputDir))
                    .filter(Files::isRegularFile).map(f->outputDir+"/"+f.getFileName().toString()).toArray(String[]::new);
                setupFileList = Files.list(Paths.get(experimentDir))
                    .filter(Files::isRegularFile).map(f->experimentDir+"/"+f.getFileName().toString()).toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }
        model1.put("filename",configFileName);
        model1.put("resfilelist",resFileList);
        model1.put("setupfilelist",setupFileList);
        model1.put("resultfile",resFileName);
        model1.put("objective_relations", objectiveRelationList);
        model1.put("objective_names", objectiveList);
        return model1;
    }

    private static String runOptimizer(TestConfig testConfig, boolean b,String resFileName) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Class optimizerClass = null;
        for (Class optAlg : optimizerClasses.keySet())
            if(optAlg.getSimpleName().equals(testConfig.getAlgorithmName()))
                optimizerClass = optAlg;


        // final Class optimizerClasses = main.Main.class.getClassLoader().loadClass(algorithmName[0]);
        Object algorithmObj = optimizerClass.newInstance();

        /*Method setConfig= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
        setConfig.invoke(algorithmObj,confi);*/
        Method setConfig= optimizerClass.getMethod("setConfiguration",TestConfig.class);
        setConfig.invoke(algorithmObj,testConfig);


        Method setOptimizerConfigMethod= optimizerClass.getMethod("setOptimizerParams",List.class);
        setOptimizerConfigMethod.invoke( algorithmObj,testConfig.getOptimizerParameters());

        if(testConfig.getIterationCounter() != 0 ) {
            Method loadInternalStateMethod = optimizerClass.getMethod("loadState", String.class);
            loadInternalStateMethod.invoke(algorithmObj, testConfig.getOptimizerStateBackupFilename());
        }

        Method loadOptimizerparams = optimizerClass.getMethod("loadOptimizerParamsFromJsonFile",String.class);
        loadOptimizerparams.invoke(algorithmObj,customParamFileName[0]);

        if(testConfig.getLandscape().size()>0) {
            Method setupTimedelta = optimizerClass.getMethod("seTimeDelta", long.class);
            setupTimedelta.invoke(algorithmObj, testConfig.getLandscape().get(testConfig.getLandscape().size() - 1).getTimeStamp());
        }

        Method runMethod= optimizerClass.getMethod("run",boolean.class,int.class,String.class);
        runMethod.invoke( algorithmObj, b,savingFrequence[0],resFileName);



        Method getLandsCapeCSVMethod = optimizerClass.getMethod("getLandscapeCSVString");
        Object result1 = getLandsCapeCSVMethod.invoke(algorithmObj);

        return (String)result1;
    }

    // TODO: 18/10/17 redundant parameter passing, should make one "run" method instead of two
    public static String runOptimizer(TestConfig testConfig,String algorithmname, List<Param> lp, boolean safeMode,String resFileName) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class optimizerClass = null;
        for (Class optAlg : optimizerClasses.keySet())
            if(optAlg.getSimpleName().equals(algorithmname))
                optimizerClass = optAlg;


        // final Class optimizerClasses = main.Main.class.getClassLoader().loadClass(algorithmName[0]);
        Object algorithmObj = optimizerClass.newInstance();
        Method setConfig= optimizerClass.getMethod("setConfiguration",TestConfig.class);
        setConfig.invoke(algorithmObj,testConfig);

       /* Method setConfig= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
        setConfig.invoke(algorithmObj,"test.json");*/

        Method setOptimizerConfigMethod= optimizerClass.getMethod("setOptimizerParams",List.class);
        setOptimizerConfigMethod.invoke( algorithmObj,lp);

        if(testConfig.getLandscape().size()>0) {
            Method setupTimedelta = optimizerClass.getMethod("setTimeDelta", long.class);
            setupTimedelta.invoke(algorithmObj, testConfig.getLandscape().get(testConfig.getLandscape().size() - 1).getTimeStamp());
        }

        Method runMethod= optimizerClass.getMethod("run",boolean.class,int.class,String.class);
        runMethod.invoke( algorithmObj, safeMode,savingFrequence[0],resFileName);


       /* Method getLAndsCapeMethod = optimizerClass.getMethod("getLandscapeString");
        Object resultO = getLAndsCapeMethod.invoke(algorithmObj);
*/
        Method getLandsCapeCSVMethod = optimizerClass.getMethod("getLandscapeCSVString");
        Object result1 = getLandsCapeCSVMethod.invoke(algorithmObj);

        return (String)result1;
    }

    /**
     * param_names -> all the dependencies
     ;;range_secondParam_div_paramdiv_dep_secondParam3

     dependency_ids -> div containing a param
     ;firstParam_div;secondParam_div;param0_div

     param_range_div_ids -> all the ranges (for any parameters)
     ;firstParam_div_paramdiv_dep_firstParam3;secondParam_div_paramdiv_dep_secondParam3;;param0_div_paramdiv_dep_param05

     * @param request
     * @param alg_name
     * @return
     */

    public static List<Param> readParams(Request request,String alg_name) {

        String param_div_range_idsStr = request.queryParams("param_range_div_ids");
        String[] param_div_range_ids = param_div_range_idsStr.split(";");
        String param_div_idsString = request.queryParams("dependency_ids");
        String param_div_ids[] = param_div_idsString.split(";");
        String dep_div_idsString = request.queryParams("param_names");
        String dep_div_ids[] = dep_div_idsString.split(";");

        List<Param> paramList = new LinkedList<Param>();

        if(alg_name!=null) {
            param_div_ids = Arrays.stream(param_div_ids)
                    .filter(pn -> pn.contains(alg_name)).toArray(String[]::new);
            param_div_range_ids = Arrays.stream(param_div_range_ids)
                    .filter(pn -> pn.contains(alg_name)).toArray(String[]::new);
        }

        //iterate over all params - paramdiv one created to describe a param
        for(String pdn : param_div_ids){
            if(!pdn.equals("")){ // skip the removed ones
                //todo we expect something like "param1_div", where param1 is the original name, but at algorithms <algname>_<paramname>_div-> need de middle one
                String paramId =  pdn.replace("_div","");
                paramId = paramId.replace(alg_name+"_","");

                //String[] pdn_parts = pdn.split("_");
                //String paramId = pdn.split("_")[pdn_parts.length-2];
                String paramname = request.queryParams(pdn + "paramname");
                String typeStr = request.queryParams(pdn + "_type");
                String typePostFix = getTypePostFix(typeStr);

                boolean func = typePostFix.equals("_func");
                //iterate over all range given for any param
                for(String prdn : param_div_range_ids) {
                    //filter what belongs to the param we are examining
                    if (!prdn.equals("") && prdn.contains(paramId)) {
                        System.out.println("PRDN: "+prdn);
                        System.out.println("PDN:"+pdn);
                        System.out.println("PNAME:"+paramname);
                        String s = paramname;
                        System.out.println(s);
                        String valStr = "";
                        String funcRunningCountStr = "";
                        if(func){
                            valStr = request.queryParams(prdn + "_value_string"+typePostFix);
                            funcRunningCountStr = request.queryParams(prdn + "_value_int"+typePostFix);
                        }
                        else
                            valStr = request.queryParams(prdn + "_value"+typePostFix);
                        String funcRunningCount = request.queryParams(prdn + "_value"+typePostFix);
                        //        System.out.println(valStr);
                        //String typeStr = request.queryParams(pdn + "_type");
                        //System.out.println(typeStr);
                        String lowerStr = request.queryParams(prdn + "_lower"+typePostFix);
                        System.out.println(lowerStr);
                        String upperStr = request.queryParams(prdn + "_upper"+typePostFix);
                        String enumStr = request.queryParams(pdn + "_enum_descriptor");
                        System.out.println(upperStr);
                        //Class clazz = Class.forName(typeStr);
                        Param<?> param = null;


                        //looks for the param in the list, whether we have added before
                        for(Param par : paramList)
                            if(par.getName().equals(paramname))
                                param = par;


                        // checking whether the given range of the param is bounded or not = is there any dependency whose id contains the id of the range
                        boolean foundDep = false;
                        List<String> dep_ids = Arrays.stream(dep_div_ids).filter(str->str.contains(prdn)).collect(Collectors.toList());
                        for(String s1 : dep_ids) {
                            if(s1.equals(""))
                                continue;
                            foundDep = true;
                            //here we found a dependency
                            String dependencyParamName =  request.queryParams(s1 +"_select_other_name");
                            Param dependencyParam = null;
                            //checking out whether the founded dependeny have been added to the paramlist
                            for(Param p1 : paramList)
                                if(p1.getName().equals(dependencyParamName))
                                    dependencyParam = p1;
                            //there is the problem, that we dont know its type here..
                            if(dependencyParam==null)
                                dependencyParam = new DummyParam(dependencyParamName);


                            String typePostFix1 = getTypePostFix(dependencyParam.getParamGenericTypeName());
                            String deplowerStr = request.queryParams(s1 + "_dep_lower"+ typePostFix1);
                            String depupperStr = request.queryParams(s1 + "_dep_upper"+ typePostFix1);

                            // the examined param is not in the list-> we create it with its default range
                            if (param == null) {
                                param = createNewParamWithoutDepencency(paramname, valStr, typeStr, enumStr, lowerStr, upperStr, funcRunningCountStr);
                            }
                            //by now we added the param, now we can add dependency
                            addDependencyToNotBoundedParam(param, dependencyParam, deplowerStr, depupperStr);
                            paramList.add(param);

                        }
                        //there is no dependency for the param so we add param to the list
                        if(!foundDep) {
                            if (param == null || param instanceof DummyParam) {
                                param = createNewParamWithoutDepencency(paramname, valStr, typeStr, enumStr, lowerStr, upperStr,funcRunningCountStr);
                                updateDependencies(paramList,param);

                                if(!paramList.contains(param))
                                    paramList.add(param);
                            }
                            else{
                                //we come here when we add a default range to a bounded parameter
                                //todo we die here!!!!
                                //String deplowerStr = request.queryParams(s1 + "_dep_lower"+ typePostFix1);
                                //String depupperStr = request.queryParams(s1 + "_dep_upper"+ typePostFix1);
                                param.addDependency1(lowerStr,upperStr);
                            }

                        }

                    }
                }
            }
        }
        return paramList;
    }

    public static ObjectiveContainer readObjectives(Request request) {
        List<ObjectiveContainer.Objective> olist = new LinkedList<ObjectiveContainer.Objective>();
        String object_names = request.queryParams("object_names");
        String[] obj_div_names = object_names.split(";");
        for(String obj_div_name : obj_div_names){
            if(obj_div_name.equals(""))
                continue;
            String obj_name = request.queryParams(obj_div_name + "_name");
            String obj_relation = request.queryParams(obj_div_name + "_relation");
            String obj_weight = request.queryParams(obj_div_name + "_weight");
            String obj_value = request.queryParams(obj_div_name + "_relation_value");
            String obj_type = request.queryParams(obj_div_name + "_type");
            //Class<?> clazz = Class.forName("com.bla.TestActivity");

            ObjectiveContainer.Objective objective = null;
            Utils.Relation rel = Utils.Relation.valueOf(obj_relation);
            boolean targetMakesSense = rel != Utils.Relation.MAXIMIZE && rel != Utils.Relation.MINIMIZE;
            if (obj_type.equals("java.lang.Integer"))
                objective = new ObjectiveContainer.Objective(Utils.Relation.valueOf(obj_relation),false,obj_name,null,obj_value==null||!targetMakesSense?0:new Float(Float.parseFloat(obj_value)).intValue(),0,Float.parseFloat(obj_weight));
            else if (obj_type.equals("java.lang.Float"))
                objective = new ObjectiveContainer.Objective(Utils.Relation.valueOf(obj_relation),false,obj_name,null,obj_value==null||!targetMakesSense?0.0:Float.parseFloat(obj_value ),0.0,Float.parseFloat(obj_weight));
            //else if (obj_type.equals("java.lang.Boolean "))

            if(!olist.contains(objective))
                olist.add(objective);


        }

        return new ObjectiveContainer(olist);
    }

    private static String getTypePostFix(String typeName ) {
        if(typeName.equals(Integer.class.getName()))
            return "_int";
        if(typeName.equals(Float.class.getName()))
            return "_float";
        if(typeName.equals(Boolean.class.getName()))
            return "_bool";
        if(typeName.equals("Function"))
            return "_func";
        return "_string";
    }
    // we defined the param properly, so if there is any depending on this, we bound it to this
    private static void updateDependencies(List<Param> paramList, Param<?> param) {
        for(Param<?> p : paramList)
            if(p.updateDependencies(param));


    }

    //why in the hell we need this????
    private static void addDependencyToNotBoundedParam(Param<?> param, Param dependencyParam, String depupperStr, String deplowerStr) {
        String paramTypeString = dependencyParam.getParamGenericType().getName();
        switch (paramTypeString) {
            case "java.lang.String":
                //String[] values = enumStr.split(",;");
                //this is dummy most probably!!!
                param.addDependencyToNodBoundedRange(dependencyParam,deplowerStr,depupperStr);
                break;
            case "java.lang.Boolean":
                // TODO: 19/05/17 dummy falses
                param.addDependencyToNodBoundedRange(dependencyParam,Boolean.parseBoolean(deplowerStr),Boolean.parseBoolean(depupperStr) );
                break;
            case "java.lang.Float":
                param.addDependencyToNodBoundedRange(dependencyParam,Float.parseFloat(deplowerStr),Float.parseFloat(depupperStr) );
                break;
            case "java.lang.Integer":
                param.addDependencyToNodBoundedRange(dependencyParam,Integer.parseInt(deplowerStr),Integer.parseInt(depupperStr) );

                break;

        }
    }

    //needed???
    private static void addDefaultRange(String typeStr, String lowerStr, String upperStr, String enumStr, Param<?> param) {
        switch (typeStr) {
            case "Enum": {
                String[] values = enumStr.split(",;");
                Param<String> param1 = (Param<String>) param;
                param1.addDependency(values);
            }
            break;
            case "Boolean": {
                // TODO: 19/05/17 dummy falses
                Param<Boolean> param1 = (Param<Boolean>) param;

                param1.addDependency(Boolean.FALSE, Boolean.FALSE);
            }break;
            case "Float": {
                Param<Float> param1 = (Param<Float>) param;

                param1.addDependency(Float.parseFloat(lowerStr), Float.parseFloat(upperStr));
            }break;
            case "Integer":{
                Param<Integer> param1 = (Param<Integer>) param;

                param1.addDependency(Integer.parseInt(lowerStr), Integer.parseInt(upperStr));
            }break;

        }
    }

    private static Param<?> createNewParamWithoutDepencency(String paramname, String valStr, String typeStr, String enumStr, String lowerStr,String upperStr,String funcRunningCountStr) {
        Param<?> param = null;
        switch (typeStr) {
            case "Enum":
                String[] values = enumStr.split("[,;]");
                param = new Param<String>(valStr, values, paramname);
                break;
            case "java.lang.Boolean":
                // TODO: 19/05/17 dummy falses
                param = new Param<Boolean>(Boolean.parseBoolean(valStr), Boolean.FALSE, Boolean.FALSE, paramname);
                break;
            case "java.lang.Float":
                param = new Param<Float>(Float.parseFloat(valStr), Float.parseFloat(upperStr), Float.parseFloat(lowerStr), paramname);
                break;
            case "java.lang.Integer":
                param = new Param<Integer>(Math.round(Float.parseFloat(valStr)), Math.round(Float.parseFloat(upperStr)),  Math.round(Float.parseFloat(lowerStr)), paramname);
                break;
                // TODO: 09/08/17 Function params with dependency??? counter hack 100 ,eval error

            case "Function":
                try {
                    param = new FunctionParam(paramname,valStr,Integer.parseInt(funcRunningCountStr));
                } catch (ScriptException e) {
                    e.printStackTrace();
                }

                break;

        }
        return param;
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
        findConfigFiles(foundClasses);

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
    public static <T> void findConfigFiles(Map<Class<? extends T>, String> foundClasses) {
        Path p = Paths.get(defaultOptimizerClassLocation);
        System.out.println(p);
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
}
