package optimizer.algorithms;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sun.org.apache.xml.internal.security.algorithms.Algorithm;
import jdk.internal.dynalink.linker.LinkerServices;
import optimizer.exception.AlgorithmException;
import optimizer.exception.ImplementationException;
import org.apache.log4j.Level;
import optimizer.main.Main;
import optimizer.utils.*;
import optimizer.config.TestConfig;
import optimizer.objective.ObjectiveContainer;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import optimizer.trial.Trial;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Created by peterkiss on 17/10/16.
 * This is the base class of all the optimizer optimizer.algorithms. If you want to write your own optimizer,
 * extend this. If you want to write your own optimization method, you have to extend this.
 *
 */


public abstract class AbstractAlgorithm {
    /**
     * Setter method for {@link #parallelizable}
     * @param parallelizable
     */
    public void setParallelizable(boolean parallelizable) {
        this.parallelizable &= parallelizable;
    }

    /**
     * False by default, since a lot of parameter optimization algortihms build on the result of previous trials.  execution of parallelizable can be switched on by setting this member to true in the subclasses.
     */
    protected boolean parallelizable = false;

    /**
     * Getter for parameters of the optimizer.
     * @return {@link #optimizerParams}
     */
    public List<Param> getOptimizerParams() {
        return optimizerParams;
    }

    /**
     * contains {@link Param}parameters of the tuning algorithm
     */
    List<Param> optimizerParams;

    /**
     * Here you can specify what parameter types can your algorithm handle.
     */
    protected List<Class> allowedTypes = new LinkedList<>();
    {
        allowedTypes.add(Float.class);
    }

    /**
     * The time difference between the start of the optimization and its potential restart to get a correct estimation of runtime.
     */
    protected long timeDelta = 0;
    /**
     *
     */
    protected TestConfig config;
    /**
     * Counter for the executed trials.
     */
    protected Integer iterationCounter=0;

    /**
     * Setter method for {@link #timeDelta}.
     * @param d
     */
    public void setTimeDelta(long d){
        this.timeDelta = d;
    }



    /**
     *Getter for the {@link #allowedTypes}.
     * */
    private List<Class> getAllowedTypes(){
        return this.allowedTypes;
    }

    /**
     * Checks here whether our {@link AbstractAlgorithm} instance is declared to be able to handle a given configuration.
     * @return
     */
    public boolean isApplyableForParams(){
        List<Param> pl = config.getScriptParametersReference();
        for(Param p : pl)
            if(!getAllowedTypes().contains(p.getParamGenericType()))
                return false;
        return true;
    }



    /**
     * This method contains the optimizer.main loop of the optimization task.
     * @param experimetDir Folder to save the experiment describing JSON.
     * @param backupDir Folder tp store potential backup files
     * @param saveFileName Unique filename with relative path generated before for experiment describing JSON.
     * @throws Exception
     */
    public void run(String experimetDir, String backupDir,String saveFileName) throws AlgorithmException, IOException, ImplementationException {

            boolean terminated = false;
            long startTime = System.currentTimeMillis();
            while (!terminated ) {

try{
                    if(this.parallelizable){

                            int threads = Runtime.getRuntime().availableProcessors();
                            ExecutorService pool = Executors.newFixedThreadPool(threads);
                            Set<Future<IterationResult>> set = new HashSet<Future<IterationResult>>();
                        try {
                            for (int i = 0; i < this.config.getIterationCount().get(); ++i) {
                                if (!configAllowed(config.getScriptParametersReference())) {
                                    config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue(config.getObjectiveContainer()));
                                    config.getLandscapeReference().add(new IterationResult(config.getScriptParametersReference(), config.getObjectiveContainer(), startTime, timeDelta));
                                }
                                set.add(pool.submit(new Trial(config.getBaseCommand(), false, "", config.getObjectiveContainer(), Param.cloneParamList(this.config.getScriptParametersReference()), startTime, timeDelta)));
                                updateParameters(config.getScriptParametersReference(), config.getLandscapeReference()/*, config.getOptimizerParameters()*/);
                                Main.log(Level.INFO, "PARAMETERS Parallel EXEC" + config.getScriptParametersReference().toString());
                            }
                            for (Future<IterationResult> future : set) {
                                IterationResult ir = future.get();
                                Main.log(Level.INFO, "GETTING RESULT FROM " + ir.getCSVString());
                                this.config.setIterationCounter(this.config.getIterationCounter() + 1);
                                this.config.getLandscapeReference().add(ir);
                                if (this.config.getSavingFrequence() != -1 && this.config.getIterationCounter() % this.config.getSavingFrequence() == 0) {
                                    String saveFileName1 = saveFileName.replace(experimetDir, backupDir).replace(".json", "_" + this.config.getIterationCounter() + ".json");
                                    writeResultFile(saveFileName1);
                                }
                            }
                        }catch (ExecutionException e){
                            throw new ImplementationException("Parallelizetion failed : "+e.getStackTrace() );
                        }finally {
                            pool.shutdown();
                        }
                        config.setIterationCounter(config.getIterationCount().get());
                        terminated = true;

                    }
                    else {
                        if (configAllowed(config.getScriptParametersReference())) {

                            BufferedReader r = null;
                            r = executeAndGetResultBufferedReader(config);
                            ObjectiveContainer oc = ObjectiveContainer.readObjectives(r,null, config.getObjectiveContainer());
                            config.setObjectiveContainer(oc);
                        } else {
                            config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue(config.getObjectiveContainer()));

                        }
                        terminated =this.terminated();
                        config.getLandscapeReference().add(new IterationResult(config.getScriptParametersReference(), config.getObjectiveContainer(),startTime,timeDelta));
                    }



                    if(!terminated) {
                        Main.log(Level.INFO,"OLD PARAMETERS" + config.getScriptParametersReference().toString());
                        try {
                            updateParameters(config.getScriptParametersReference(), config.getLandscapeReference()/*, config.getOptimizerParameters()*/);
                        }  catch (Exception e){
                            throw new AlgorithmException("Algorithm error");
                        }
                        Main.log(Level.INFO,"NEW PARAMETERS" + config.getScriptParametersReference().toString());
                    }

                    if(this.config.getSavingFrequence()!=-1 && this.config.getIterationCounter() % this.config.getSavingFrequence() == 0|| terminated) {
                        if(terminated && config.getLandscapeReference().size()<config.getIterationCount().get())
                            System.out.println("para");
                        this.saveState(this.config.getOptimizerStateBackupFilename()==null?"optBackUp.json":this.config.getOptimizerStateBackupFilename());
                        if(!terminated) {
                            String saveFileName1 = saveFileName.replace(experimetDir,backupDir).replace(".json","_"+this.config.getIterationCounter()+".json");
                            writeResultFile(saveFileName1);
                        }
                    }
    // TODO: 2018. 02. 25. InterruptedException  
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                this.config.setIterationCounter(this.config.getIterationCounter()+1);
                Main.log(Level.INFO,this.config.toString());
            }

    }

    /**
     * Method to execute a process based on configuration specified at the moment in config, and returns a {@link BufferedReader} object to read its output.
     * @param config {@link TestConfig} object containg all parameter of the next run.
     * @return BufferedReader that reads either from a result file, or from standard output, based on specification in the config.
     * @throws IOException
     * @throws InterruptedException
     */
    @Deprecated
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

    /**
     * Decides whether a given optimization problem can be runed using the given {@link AbstractAlgorithm}vThat is whether it handles the Parameter types that are in the configuration of the problem.
     * @param scriptParameters The parameters of the algorihm to be optimized.
     * @return
     * @throws Exception
     */
    private synchronized boolean configAllowed(List<Param> scriptParameters) throws ImplementationException {
        for(Param p : scriptParameters){
            if(!p.isActive())
                continue;
            if(p.isValid()&&!p.isInRange())
                return false;

        }
        return true;
    }

    //later

    /**
     * Beta method for executing Trials onn demand if the logic optimization requires it . If the Trial has been already run should return with its result without redundant execution.
     * @param configuration The configuration to try.
     * @param startTime
     * @param delay
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public ObjectiveContainer lookup(List<Param> configuration,long startTime,long delay) throws InterruptedException, IOException, CloneNotSupportedException {
        for(IterationResult ir : config.getLandscapeReference())
            if(Utils.paramConfigsAreEqual(ir.getConfiguration(),configuration))
                return ir.getObjectives();
        String command = config.getCommand(configuration,config.getBaseCommand());
        ObjectiveContainer oc = runAlgorithm(command);

        config.getLandscapeReference().add(new IterationResult(configuration,oc,startTime,delay));
        return oc;

    }

    /**
     * The method runs the algorithm then retrieves a reads the objective values and passes them back in an {@link ObjectiveContainer}
     * @param command
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private ObjectiveContainer runAlgorithm(String command) throws IOException, InterruptedException {
        BufferedReader r = executeAndGetResultBufferedReader(config);
        return ObjectiveContainer.readObjectives(r,null, config.getObjectiveContainer());
    }

    @Deprecated
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
     * Method for querying whether  the tuning has been terminated.
     * If termination is due to iteration number limit, it is possible that we have more trials, since some tuning optimizer.algorithms execute them in a grouped way
     * @return
     * @throws FileNotFoundException
     */
    public  boolean terminated() throws FileNotFoundException {
        if(this.config.getIterationCount().isPresent()
                && this.config.getIterationCounter()>= this.config.getIterationCount().get())
            return  true;
        return this.config.getObjectiveContainer() == null ? false : this.config.getObjectiveContainer().terminated();

    }


    /**
     * Method to get the parameters of the optimizer algorithm.
     * @return a cloned list of optimizer parameters.
     * @throws CloneNotSupportedException
     */
    public List<Param> getConfig() throws CloneNotSupportedException {
        List<Param> ret = new LinkedList<>();
        for(Param p: this.optimizerParams)
            ret.add((Param) p.clone());
        return ret;
    }

    /**
     * Methof for setting the parameters of the optimizer algorithm ({@code #optimizerParams}). This is called to set up the parameters wen we use the html interface
     * @param paramList List of parameters
     */
    public void setOptimizerParams(List<Param> paramList){
        this.optimizerParams = paramList;
    }

    /**
     * Writes the list of {@link Param} of the optimizer algorithm into a separated JSON
     * @param jsonName
     * @throws FileNotFoundException
     */
    @Deprecated
    public void writeOptimizerParamsToJsonFile(String jsonName) throws FileNotFoundException {
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
     * Set the experiment setup({@link TestConfig}) to be runned using this algorithm.
     */
    public void setConfiguration(TestConfig tc){
        this.config = tc;
    }

    /**
     * loads the entire test-configuration from a json file
     * @param configFileName
     */
    @Deprecated
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
                //this.config = new TestConfig(configFileName);
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
    @Deprecated
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
     * @param parameterMap contains the description of the {@link Param Param} values their ranges {@link Param#getDependencies()} and their dependencies. The system expects to change the parametervalues here using set
     * @param landscape readonly, contains the history of executions of the algorithm to be optimized
     * .@param optimizerParams parameters of the optimizer
     */
    public abstract void updateParameters(List< Param> parameterMap, List<IterationResult> landscape/*, List<Param> optimizerParams*/) throws CloneNotSupportedException;



}
