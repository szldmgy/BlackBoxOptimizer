package main;

import algorithms.AlgorithmFI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.ModelAndView;
import spark.Request;
import spark.template.velocity.VelocityTemplateEngine;
import utils.*;

import javax.script.ScriptException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Created by peterkiss on 2018. 01. 22..
 */
public class BrowserInterface {


    private final static String layout = "templates/layout.vtl";
    private final static String resultTemplate = "templates/resultnew.vtl";
    private final String[] algoritmhs;
    private final Boolean[] safeMode= {false};
    private final boolean[] recoveryMode = {false};
    private final String[] configFileName= new String[1];
    private final String[] saveFileName= new String[1];
    private static String outputDir;
    private static String experimentDir ;
    private static String uploadDir ;
    private static String backupDir ;
    private static List<String> classList;
    private static List<String> objTypes;
    final String[] objectiveTypes = Arrays.stream(Utils.Relation.values()).map(v->v.toString()).toArray(String[]::new);


    //available algorithms
    private Map<Class<? extends AlgorithmFI>,String> optimizerClasses;// = new HashMap<Class<? extends AlgorithmFI>,String>();

    private  TestConfig[] config = new TestConfig[1];
    public BrowserInterface(String initialConfigFileName,Map<Class<? extends AlgorithmFI>,String> optimizerClasses, String projectDir, String staticDir, String experimentDir, String outputDir, String backupDir,String uploadDir,String saveFileName) throws CloneNotSupportedException, FileNotFoundException {


        this.config[0] = TestConfig.readConfigJSON(initialConfigFileName);
        this.optimizerClasses = optimizerClasses;
        this.algoritmhs =  optimizerClasses.keySet().stream().map(a->a.getName()).toArray(String[]::new);
        this.saveFileName[0] =saveFileName;

        BrowserInterface.uploadDir = uploadDir;
        BrowserInterface.outputDir = outputDir;
        BrowserInterface.experimentDir = experimentDir;
        BrowserInterface.backupDir = backupDir;

        BrowserInterface.classList=  Arrays.asList(Integer.class.getName(),Float.class.getName(),Boolean.class.getName(),"Enum","Function");
        BrowserInterface.objTypes=  Arrays.asList(Integer.class.getName(),Float.class.getName()/*,Boolean.class.getName(),"Enum","Function"*/);


        staticFiles.externalLocation(projectDir + staticDir);
        staticFiles.externalLocation(uploadDir);
        staticFileLocation("/public");

        //final List<Param> p = config[0].getScriptParameters();
        //final String command = config[0].getBaseCommand();
        //final String[] parameternames = config[0].getScriptParameters().stream().map(par->par.getName()).toArray(String[]::new);
        //final List<ObjectiveContainer.Objective> objectives =  config[0].getObjectiveContainer().getObjectives();



    }

    public void run(){
        get("/results", (req, res) ->{

            Map<String, Object> model1 = getResultModel(config[0].getObjectiveContainer().getObjectives(), "experiment.csv",saveFileName[0]);


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


            File uploadDir = new File(BrowserInterface.uploadDir);
            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
            //String s = req.queryParams("chosenfile");
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            Part filePart = req.raw().getPart("chosenfile");
            Part fn = req.raw().getPart("cfn");
            this.configFileName[0] = filePart.getSubmittedFileName();
            //prepare base of the savefilename
            String [] fnparts =  configFileName[0].split("/");
            saveFileName[0] =fnparts[fnparts.length-1].replace(".json","");
            try (InputStream input = filePart.getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            if(tempFile.toFile().length()!=0)
                config[0] =  TestConfig.readConfigJSON(tempFile.toFile());
            Files.delete(tempFile);
            //  }
            Map<String, Object> model = getBBSetupModel(saveFileName[0],config, classList, objectiveTypes, algoritmhs,objTypes);


            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, layout)
            );
        });

        post("/updateconfig", (request, response) -> {
            Map<String, Object> model = new HashMap<String, Object>();
            String safeModeString = request.queryParams("safe_mode");
            if(safeModeString != null) {
                config[0].setSavingFrequence(Integer.parseInt(request.queryParams("frequency")));
                safeMode[0] = true;
            }

            //todo algorithmname here it is alwaysd null, saverfilename goes with default

            saveFileName[0] = request.queryParams("savefilename");
            String useIterationString = request.queryParams("use_iterations");
            String iterationCountString = request.queryParams("iterationCount");
            String command_input = request.queryParams("commandinput");

            int counter = 0;
            List<IterationResult> landscape = new LinkedList<IterationResult>();
            //followings needed for recovery(??)
            //setup previous or predefined optimizer algorithms it there is any -
            List<Param> predefinedOptimizerParams = config[0].getOptimizerParameters();


            if(config[0].getLandscape().size()>0) {
                recoveryMode[0] = true;
                landscape = config[0].getLandscape();
                counter = config[0].getIterationCounter();

            }
            else
                recoveryMode[0] = false;
            TestConfig c = new TestConfig(); //todo we need a new one? not like it would change too much
            c.setAlgorithmName(config[0].getAlgorithmName());
            // in case we have htem in the same file
            c.setOptimizerParameters(predefinedOptimizerParams);
            c.setBaseCommand(command_input);
            c.setIterationCounter(config[0].getIterationCounter()); //this is for recovery mode (??)
            if(useIterationString != null)
                c.setIterationCount(Optional.of(Integer.parseInt(iterationCountString)));


            String objFileName1 = request.queryParams("objFileName");
// TODO: 23/07/17 what about other predefined lists??? - objectives variable might be removed, use only the tc one all this shit because of recovery could be simpler
            ObjectiveContainer objectiveContainer = readObjectives(request);
           // objectives.clear();
           // objectives.addAll(objectiveContainer.getObjectives());

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
            }); //recovery mode if there is any predefined algorithm, otherwise the first in the list
            if(!algParamMap.keySet().contains(config[0].getAlgorithmName()))
                config[0].setAlgorithmName(algParamMap.keySet().iterator().next());
            //we set up values for optimizerparam from configfile
            List<Param> pl = algParamMap.get(config[0].getAlgorithmName());
            if(pl!=null && config[0].getOptimizerParameters()!=null) // for algorithm without parameters
                for(Param paramInMap : pl){ //for reloading values at recovery??
                    for(Param loadedParam : config[0].getOptimizerParameters())
                        if(paramInMap.equals(loadedParam))
                            paramInMap = loadedParam; //is this ok? it might be lost after loop

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
            config[0].setAlgorithmName(algorithmname);
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
            if(recoveryMode[0]) {
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
            config[0].setOptimizerClasses(optimizerClasses);
            if(!recoveryMode[0]) {
                config[0].setIterationCounter(0);
                config[0].clearLandscape();

            }


            String expFileName = Utils.getExperimentUniqueName(saveFileName[0],experimentDir);
            //String expFileName = Utils.getExpJSONFileName(experimentName,experimentDir) ;
            String resFileName = Utils.getExpCSVFileName(Utils.getExperimentName(expFileName),outputDir);

            config[0].runAndGetResultfiles(expFileName, resFileName, experimentDir,backupDir);


            Map<String, Object> model1 = getResultModel(this.config[0].getObjectiveContainer().getObjectives(), resFileName,saveFileName[0]);


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
        model.put("filename",file.replace("experiments/",""));
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



    private static Map<String, Object> getResultModel(List<ObjectiveContainer.Objective> objectives, String resFileName, String configFileName) {
        final List<String> objectiveList = new LinkedList<String>();
        objectives.forEach(o->objectiveList.add(o.getName()));

        final List<String> objectiveRelationList = new LinkedList<String>();
        objectives.forEach(o->objectiveRelationList.add(o.getRelation().equals(Utils.Relation.GREATER_THEN)||o.getRelation().equals(Utils.Relation.MAXIMIZE)?"increase":"decrease"));

        // TODO: 10/08/17 landscape arrived from server


        Map<String, Object> model1 = new HashMap<>();
        model1.put("template", "templates/resultnew.vtl");
        //String resultFilePath =  projectDir + "/BlackBoxOptimizer"+staticDir+"/"+resFileName;
        //String resultFilePath =  projectDir + "/BlackBoxOptimizer"+staticDir+"/"+resFileName;
        List<String>[] resFileList = new List[1];
        List<String>[] setupFileList = new List[1];
        try {
            resFileList[0] =Files.list(Paths.get(outputDir))
                    .filter(Files::isRegularFile).map(f->outputDir+"/"+f.getFileName().toString()).collect(Collectors.toList());
            setupFileList[0] = Files.list(Paths.get(experimentDir))
                    .filter(Files::isRegularFile).map(f->experimentDir+"/"+f.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> failedExperiments = new LinkedList<>();
        setupFileList[0].stream().filter(sfn->!resFileList[0].contains(sfn.replace(".json",".csv").replace(experimentDir+"/",outputDir+"/"))).forEach(sfn->{failedExperiments.add(sfn);});
        failedExperiments.stream().forEach(sfn->setupFileList[0].remove(sfn));
        Collections.sort(resFileList[0]);
        Collections.sort(setupFileList[0]);
        model1.put("failed",failedExperiments);
        model1.put("filename",configFileName);
        model1.put("resfilelist",resFileList[0]);
        model1.put("setupfilelist",setupFileList[0]);
        model1.put("resultfile",resFileName);
        model1.put("objective_relations", objectiveRelationList);
        model1.put("objective_names", objectiveList);
        return model1;
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
        String name_param_id_map_string = request.queryParams("name_param_id_map");
        Map<String,String> param_id_name_map = new HashMap<>();
        if(name_param_id_map_string!=null&&!name_param_id_map_string.isEmpty()) {
            String entries[] = name_param_id_map_string.split(",");
            for (String s : entries)
                if (!s.isEmpty()) {
                    String[] s1s = s.split(":");
                    param_id_name_map.put(s1s[0], s1s[1]);
                }
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
                            String dependencyParamId =  request.queryParams(s1 +"_select_other_name");
                            String dependencyParamName =  param_id_name_map.get(dependencyParamId);


                            Param dependencyParam = null;
                            //checking out whether the founded dependeny have been added to the paramlist
                            for(Param p1 : paramList)
                                if(p1.getName().equals(dependencyParamName))
                                    dependencyParam = p1;
                            //there is the problem, that we dont know its type here..
                            if(dependencyParam==null)
                                dependencyParam = new DummyParam(dependencyParamName);

                            //here we could find if we iterate over all the possibilities
                            /*String typePostFix1 = getTypePostFix(dependencyParam.getParamGenericTypeName());
                            String deplowerStr = request.queryParams(s1 + "_dep_lower"+ typePostFix1);
                            String depupperStr = request.queryParams(s1 + "_dep_upper"+ typePostFix1);*/
                            // TODO: 2018. 01. 31. move to some decent place reusable at multiple places
                            Class<?>[] allowedClasses = {Float.class,Integer.class,String.class,Boolean.class};
                            String deplowerStr = "";
                            String depupperStr = "";
                            for(Class<?> t : allowedClasses) {
                                String typePostFix1 = getTypePostFix(t.getCanonicalName());
                                deplowerStr = request.queryParams(s1 + "_dep_lower" + typePostFix1);
                                depupperStr = request.queryParams(s1 + "_dep_upper" + typePostFix1);
                                if(deplowerStr!=null)
                                    break;
                            }
                            if(deplowerStr==null)
                                throw new RuntimeException("not implemented!!");

                            // the examined param is not in the list-> we create it with its default range
                            if (param == null) {
                                param = createNewParamWithoutDepencency(paramname, valStr, typeStr, enumStr, lowerStr, upperStr, funcRunningCountStr);
                            }
                            //by now we added the param, now we can add dependency - a dummy now
                            addDependencyToNotBoundedParam(param, dependencyParam,  depupperStr,deplowerStr);
                            if(!paramList.contains(param))
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
                                //we come here when we add a default range to a bounded parameter = when bounding param is not in its range specified to bound this

                                try {
                                    param.addDependency1(lowerStr,upperStr,valStr,funcRunningCountStr);
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }
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
                param.addDependencyToNodBoundedRange(dependencyParam,Boolean.parseBoolean(deplowerStr),Boolean.parseBoolean(deplowerStr) );
                break;
            case "java.lang.Float":
                param.addDependencyToNodBoundedRange(dependencyParam,Float.parseFloat(deplowerStr),Float.parseFloat(depupperStr) );
                break;
            case "java.lang.Integer":
                param.addDependencyToNodBoundedRange(dependencyParam,Integer.parseInt(deplowerStr),Integer.parseInt(depupperStr) );

                break;

        }
    }

    @Deprecated
    private static void addDefaultRange(String typeStr, String lowerStr, String upperStr, String enumStr, Param<?> param) throws ScriptException {
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
                param = new Param<Integer>(Integer.parseInt(valStr),Integer.parseInt(upperStr),Integer.parseInt(lowerStr), paramname);
                break;

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
}
