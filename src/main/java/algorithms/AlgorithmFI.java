package algorithms;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import javafx.concurrent.Task;
import org.apache.log4j.Level;
import main.Main;
import utils.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;


/**
 * Created by peterkiss on 17/10/16.
 * This is the base class of all the optimizer algorithms. If you want to write your own optimizer,
 * extend this. If you
 *
 */


public abstract class AlgorithmFI {
    //private boolean parallelizable;

    // references in config to these for saving state
    // these can be logically in config
    //List<IterationResult> landscape;

    public List<Param> getOptimizerParams() {
        return optimizerParams;
    }

    /**
     * contai ns parameters of the tuning algorithm
     */
    List<Param> optimizerParams;

    /**
     * Here you can specify what parameter types can your algorithm handle
     */
    protected List<Class> allowedTypes = new LinkedList<>();
    {
        allowedTypes.add(Float.class);
        //allowedTypes.add(Double.class); //// TODO: 16/10/17 type hack
    }

    protected long timeDelta = 0;
    protected TestConfig config;
    protected Integer iterationCounter=0;

    public void setTimeDelta(long d){
        this.timeDelta = d;
    }



    /**
     * override this if your algorithm can handle multiple types
     *
     * */
    private List<Class> getAllowedTypes(){
        return this.allowedTypes;
    }

    /**
     * we check here whether our algorithm is declared to be able to handle a given configuration
     * @return
     */
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

    /**
     * main loop of the optimization task
     * @param sfr
     * @param experimetDir
     * @param backupDir
     * @param saveFileName
     * @throws Exception
     */
    public void run(int sfr,String experimetDir, String backupDir,String saveFileName) throws Exception {
        try {
            boolean terminated = false;
            long startTime = System.currentTimeMillis();
            while (!terminated ) {

                try {
                    /*if(this.parallelizable){
                        Thread t = new Thread(new Task<>(config.getScriptParameters()) {
                        })
                    }*/
                    //else {
                        if (configAllowed(config.getScriptParameters())) {

                            BufferedReader r = null;
                            r = executeAndGetResultBufferedReader(config);
                            config.setObjectiveContainer(ObjectiveContainer.readObjectives(r, config.getObjectiveContainer()));
                        } else {
                            config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue(config.getObjectiveContainer()));

                        }
                    //}
                    terminated =this.terminated();

                    //ls adding should be sysnchronized -
                    config.getLandscape().add(new IterationResult(config.getScriptParameters(), config.getObjectiveContainer(),startTime,timeDelta));

                    if(!terminated) {
                        Main.log(Level.INFO,"OLD PARAMETERS" + config.getScriptParameters().toString());
                        try {
                            updateParameters(config.getScriptParameters(), config.getLandscape()/*, config.getOptimizerParameters()*/);
                        }  catch (Exception e){
                            System.out.println("=============Algorithm error!===========");
                            e.printStackTrace();
                        }
                        Main.log(Level.INFO,"NEW PARAMETERS" + config.getScriptParameters().toString());
                    }

                    if(sfr!=-1 && this.config.getIterationCounter() % sfr == 0|| terminated) {
                        if(terminated && config.getLandscape().size()<config.getIterationCount().get())
                            System.out.println("para");
                        this.saveState(this.config.getOptimizerStateBackupFilename()==null?"optBackUp.json":this.config.getOptimizerStateBackupFilename());
                        if(!terminated) {
                            String saveFileName1 = saveFileName.replace(experimetDir,backupDir).replace(".json","_"+this.config.getIterationCounter()+".json");
                            writeResultFile(saveFileName1);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                this.config.setIterationCounter(this.config.getIterationCounter()+1);
                Main.log(Level.INFO,this.config.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param config
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * the method runs the algorithm then retrievs a reads the objective values and passes them back in an @ObjectiveContainer
     * @param command
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * is the tuning terminated?
     * if termination is due to iteration number limit, it is possible that we have more trials, since some tuning algorithms execute them in a grouped way
     * @return Boolean whether we reached the
     * @throws FileNotFoundException
     */
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
