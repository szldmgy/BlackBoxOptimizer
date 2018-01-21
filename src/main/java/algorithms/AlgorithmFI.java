package algorithms;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.log4j.Level;
import main.Main;
import utils.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;


/**
 * Created by peterkiss on 17/10/16.
 * This is the base class of all the optimizer algorithms. If you want to write your own optimizer
 *
 */


public abstract class AlgorithmFI {

    // references in config to these for saving state
    // these can be logically in config
    //List<IterationResult> landscape;


    List<Param> optimizerParams;
    protected List<Class> allowedTypes = new LinkedList<>();
    {
        allowedTypes.add(Float.class);
        allowedTypes.add(Double.class); //// TODO: 16/10/17 type hack
    }

    //ObjectiveContainer lastObjectives;
    protected long timeDelta = 0;
    protected TestConfig config;
    protected Integer iterationCounter=0;
    //final static Logger logger = Logger.getLogger(AlgorithmFI.class);

    public void setTimeDelta(long d){
        this.timeDelta = d;
    }


    //public List<IterationResult> getLandscape() {        return landscape;    }


    /*public String getLandscapeString(){
        String res = "";
        for(IterationResult ir :landscape){
            res += ir+"\n";
        }
        return res;
    }*/
    /**
     * override this if your algorithm can handle multiple types
     *
     * */
    private List<Class> getAllowedTypes(){
        return this.allowedTypes;
    }

    public boolean isApplyableForParams(){
        List<Param> pl = config.getScriptParameters();
        for(Param p : pl)
            if(!getAllowedTypes().contains(p.getParamGenericType()))
                return false;
        return true;
    }

    public String getLandscapeCSVString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner("\n");
        sj.add("iteration,"+this.config.getLandscape().get(0).getCSVHeaderString());
        int i =0;
        for(IterationResult ir :this.config.getLandscape()){
            try {
                if(!ir.badConfig())
                    sj.add(i++ +","+ir.getCSVString());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return sj.toString();
    }


    @Deprecated
    public  void run(String configFileName,String saveFileName) throws Exception {
        loadConfigFromJsonFile(configFileName);
        // TODO: 17/07/17 maybe enable safemode here too, but not used for now
        run(false,0,saveFileName);
    }




    // TODO: 25/07/17 modify signatures int could be enough
    public void run(boolean safeMode, int sfr,String saveFileName) throws Exception {
        //if(config.getLandscape() == null)
        //    config.
        //config.getLandscape().clear();
        //this.iterationCounter = config.getIterationCounter();
        try {
            boolean terminated = false;
            long startTime = System.currentTimeMillis();
            while (!terminated ) {

                try {
                    if(configAllowed(config.getScriptParameters())) {

                        BufferedReader r = null;
                        r = executeAndGetResultBufferedReader(config);




                        config.setObjectiveContainer(ObjectiveContainer.readObjectives(r, config.getObjectiveContainer()));
                    }
                    else{
                        config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue( config.getObjectiveContainer()));

                    }

                    terminated =this.terminated();
//                    Map<String, Param> paramsToSave = new HashMap<>();
//                    for (Param e : config.getScriptParameters())
//                        paramsToSave.put(e.getName(), (Param) e.clone());
                    config.getLandscape().add(new IterationResult(config.getScriptParameters(), config.getObjectiveContainer(),startTime,timeDelta));

                    if(!terminated) {
                        //Utils.printParameters(config.getScriptParameters());
                        Main.log(Level.INFO,"OLD PARAMETERS" + config.getScriptParameters().toString());
                        //here we pass references, the
                        try {
                            updateParameters(config.getScriptParameters(), config.getLandscape()/*, config.getOptimizerParameters()*/);
                        }  catch (Exception e){
                            System.out.println("=============Algorithm error!===========");
                            e.printStackTrace();
                        }
                        Main.log(Level.INFO,"NEW PARAMETERS" + config.getScriptParameters().toString());
                    }

                    //Utils.printParameters(config.getScriptParameters());

                   // mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

//Object to JSON in file
                    // TODO: 17/07/17 check if we can omit this
                    /*
                    try (Writer writer = new FileWriter("trial.json")) {
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        gson.toJson(this.config, writer);
                    }*/
                    if(safeMode && this.config.getIterationCounter() % sfr == 0|| terminated) {
                        this.saveState(this.config.getOptimizerStateBackupFilename()==null?"optBackUp.json":this.config.getOptimizerStateBackupFilename());
                        // TODO: 22/09/17 removed for get rid of garbage generation
                        //String resultFileName = (!terminated?"backup/experiment_"+this.getAlgorithmSimpleName()+"_"+this.config.getIterationCounter():"experiment_"+this.getAlgorithmSimpleName()+new Date().toString())+".json";
                        if(!terminated) {
                            //resultFileName = (!terminated ? resultFileName.replace("results","backup").replace(".csv","_"+this.config.getIterationCounter()+".json"): resultFileName);
                            saveFileName = (!terminated ? saveFileName.replace("experiments","backup").replace(".json","_"+this.config.getIterationCounter()+".json"): saveFileName);
                            writeResultFile(saveFileName);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }


                //this.config.setIterationCounter(++this.iterationCounter);
                this.config.setIterationCounter(this.config.getIterationCounter()+1);
                //System.out.println(this.config.toString());
                Main.log(Level.INFO,this.config.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedReader executeAndGetResultBufferedReader(TestConfig config) throws IOException, InterruptedException {
        BufferedReader r;
        String command = config.getCommand();
        Runtime rt = Runtime.getRuntime();
        Main.log(Level.INFO,"Executing : " + command);

        Process pr = rt.exec(command);
        pr.waitFor();

        if(config.getObjectiveFileName()!=null) {
            FileReader fr = new FileReader(config.getObjectiveFileName());
            r = new BufferedReader(fr);
        }
        else
            r= new BufferedReader(new InputStreamReader(pr.getInputStream()));
        return r;
    }

    //if one of the param is not active at a given place we say no..
    private boolean configAllowed(List<Param> scriptParameters) throws Exception {
        for(Param p : scriptParameters){
            if(!p.isActive())
                continue;
            if(p.isValid()&&!p.isInRange())
                return false;

        }
        return true;
    }

    public ObjectiveContainer lookup(List<Param> configuration,long startTime,long delay) throws InterruptedException, IOException, CloneNotSupportedException {
        for(IterationResult ir : config.getLandscape())
            if(Utils.paramConfigsAreEqual(ir.getConfiguration(),configuration))
                return ir.getObjectives();
        String command = config.getCommand(configuration);
        ObjectiveContainer oc = runAlgorithm(command);

        config.getLandscape().add(new IterationResult(configuration,oc,startTime,delay));
        return oc;

    }

    private ObjectiveContainer runAlgorithm(String command) throws IOException, InterruptedException {
        BufferedReader r = executeAndGetResultBufferedReader(config);
        return ObjectiveContainer.readObjectives(r, config.getObjectiveContainer());
    }


    public void writeResultFile(String configFileName) throws IOException {
        /*SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String configFileName  = getAlgorithmSimpleName()+"_"+dateFormat.format(new Date());*/
        //config.getSaveFileName();
        //writeOptimizerParamsToJsonFile(configFileName);
        //this.config.setOptimizerConfigFilename(configFileName);
        try (Writer writer = new FileWriter(configFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this.config, writer);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // TODO: 04/04/17 this.lastObjectives == null  
    public  boolean terminated() throws FileNotFoundException {
        if(this.config.getIterationCount().isPresent()
                && this.config.getIterationCounter()>= this.config.getIterationCount().get())
            return  true;
        return this.config.getObjectiveContainer() == null ? false : this.config.getObjectiveContainer().terminated();

    }
    /*
    public void readParams(String jsonName) throws FileNotFoundException {
        Type listType = new TypeToken<LinkedList<Param>>(){}.getType();
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(jsonName));
        this.optimizerParams = gson.fromJson(reader, TestConfig.class);

    }*/

    /**
     * returns the parameters of the optimizer algorithm
     * @return
     * @throws CloneNotSupportedException
     */
    public List<Param> getConfig() throws CloneNotSupportedException {
        List<Param> ret = new LinkedList<>();
        for(Param p: this.optimizerParams)
            ret.add((Param) p.clone());
        return ret;
    }

    /**
     * set the parameters of the optimizer. This is called to set up the parameters wen we use the html interface
     * @param paramList
     */
    public void setOptimizerParams(List<Param> paramList){
        this.optimizerParams = paramList;
    }
    public void writeOptimizerParamsToJsonFile(String jsonName) throws FileNotFoundException {
        // TODO: 22/09/17 removed for get rid of garbage generation
        if(false) {
            Type listType = new TypeToken<LinkedList<Param>>() {
            }.getType();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, listType);

            try {
                //PrintWriter writer = new PrintWriter(getAlgorithmSimpleName()+"_params.json", "UTF-8");
                PrintWriter writer = new PrintWriter(jsonName + ".config", "UTF-8");
                writer.println(s);
                //writer.println("The second line");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     */
    public void setConfiguration(TestConfig tc){
        this.config = tc;
    }

    /**
     * loads the entire test-configuration from a json
     * @param configFileName
     */
    public void loadConfigFromJsonFile(String configFileName){
        if(configFileName == null)
            return;
        Main.log(Level.INFO,System.getProperty("user.dir"));
        Main.log(Level.INFO,"ConfigFile = " + configFileName);
        try {
            if (configFileName.contains(".json")) {
                Gson gson = new Gson();
                JsonReader reader = new JsonReader(new FileReader(configFileName));
                this.config =  gson.fromJson(reader, TestConfig.class);
                loadOptimizerParamsFromJsonFile(config.getOptimizerConfigFilename());
            } else {
                this.config = new TestConfig(configFileName);
            }
        }catch (IOException e){
            Main.log(Level.INFO,"File not found:  "+ configFileName);
            return;
        }

    }

    /**
     * loads the parameter configuration of the optimizer from an external json
     * @param configFileName
     * @throws FileNotFoundException
     */
    public void loadOptimizerParamsFromJsonFile(String configFileName) throws FileNotFoundException {
        if(configFileName == null)
            return;
        Type listType = new TypeToken<LinkedList<Param>>(){}.getType();
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(configFileName));
        this.optimizerParams =  gson.fromJson(reader, listType);
    }

    public String getAlgorithmQualifiedName(){
        return this.getClass().getName();
    }
    public String getAlgorithmSimpleName(){
        return this.getClass().getSimpleName();
    }
    //public String getBa

    /**
     * update the configuration of the optimizer algorithm if it is necessary.
     * @param algParams this contains the parametewrs to be optimized
     */
    public void updateConfigFromAlgorithmParams(List<Param> algParams){}

    /**
     * override to save he algorithm internal state for recovery
     */
    public void saveState(String internalStateBackupFileName){}
    /**
     * override to save he algorithm internal state for recovery
     */
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {}


    /**
     * Here the optimizer algorithm receives references to the lists containing state of the optimization
     * @param parameterMap contains the description of the {@link utils.Param Param} values their ranges {@link utils.Param#getDependencies()} and their dependencies. The system expects to change the parametervalues here using set
     * @param landscape readonly, contains the history of executions of the algorithm to be optimized
     * .@param optimizerParams parameters of the optimizer
     */
    public abstract void updateParameters(List< Param> parameterMap, List<IterationResult> landscape/*, List<Param> optimizerParams*/) throws CloneNotSupportedException;



}
