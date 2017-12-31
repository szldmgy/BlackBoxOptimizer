package utils;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

/**
 * Created by peterkiss on 14/10/16.
 *
 * read settings file
 * command to run the script
 * PARAMETERS
 * intial values, constraints
 * place of object-function's value
 * acceptance threshold on object function
 */
public class TestConfig {
    //input from html or config file, to be modified by new parameter values, should not be modified
    private String baseCommand;
    //path to file
    private String objectiveFileName;
    //simple name of the class implementing AlgorithmFI
    private String algorithmName;
    //for saving the actual state of optimization task we save what iteration we are in
    private int iterationCounter;

    //if we have to save the optimizer algorithm's inner state we do that in this file
    private String optimizerStateBackupFilename;

    //stores the optimizer algorithm's parameter-setup
    private String optimizerConfigFilename;

    //we store the landscape here in order to be able to reload the last state in case of interruption
    private  List<IterationResult> landscape = new LinkedList<>();

    //recent state of parameterspace
    private List<Param> optimizerParameters;

    //optional limit for iterations
    private Optional<Integer> iterationCount;



    final static Logger logger = Logger.getLogger(TestConfig.class);


   /* public static TestConfig copySetUp(TestConfig tc){
        TestConfig tcn = new TestConfig();
        tcn.setIterationCount(tc.getIterationCount());
        tcn.setLandscape();
    }*/

    public void setBaseCommand(String baseCommand) {
        this.baseCommand = baseCommand;
    }
    public void setObjectiveFileName(String objectiveFileName) {
        this.objectiveFileName = objectiveFileName;
    }
    public void setScriptParameters(List<Param> scriptParameters) {
        this.scriptParameters = scriptParameters;
    }
    public void setIterationCount(Optional<Integer> iterationCount) {        this.iterationCount = iterationCount;    }
    public void setAlgorithmName(String algorithmName) {        this.algorithmName = algorithmName;    }
    public void setLandscape(List<IterationResult> landscape) {        this.landscape = landscape;    }
    public void setOptimizerConfigFilename(String optimizerConfigFilename) {        this.optimizerConfigFilename = optimizerConfigFilename;    }
    public void setIterationCounter(int iterationCounter) {        this.iterationCounter = iterationCounter;    }
    public void setOptimizerStateBackupFilename(String optimizerStateBackupFilename) {        this.optimizerStateBackupFilename = optimizerStateBackupFilename;    }



    public String getAlgorithmName() {        return algorithmName;    }
    public String getBaseCommand() { return baseCommand; }
    public String getObjectiveFileName() { return objectiveFileName; }
    public Optional<Integer> getIterationCount() {return iterationCount;}
    public List< Param> getScriptParameters() {return scriptParameters;}
    public List< Param> getOptimizerParameters() {return optimizerParameters; }
    public List<IterationResult> getLandscape() { return landscape; }
    public ObjectiveContainer getObjectiveContainer() {        return objectiveContainer;    }
    public String getOptimizerConfigFilename() {       return optimizerConfigFilename;    }
    public int getIterationCounter() {        return iterationCounter;    }
    public String getOptimizerStateBackupFilename() {        return optimizerStateBackupFilename;    }


    public String getCommand()
    {
        String command = "";
        List<String> s =new LinkedList<>();
        s.add(null);
        Scanner sc = new Scanner(this.getBaseCommand());
        while (sc.hasNext()) {
            s.set(0,sc.next());
            if(s.get(0).charAt(0) == '$') {

                command += scriptParameters.stream().filter(e -> e.getName().equals(s.get(0).substring(1))).findFirst().get().getValue().toString();
            }else
                command += s.get(0);
            command += " ";
        }
        return  command;
    }
    public String getCommand(List<Param> scriptParameters)
    {
        String command = "";
        List<String> s =new LinkedList<>();
        s.add(null);
        Scanner sc = new Scanner(this.getBaseCommand());
        while (sc.hasNext()) {
            s.set(0,sc.next());
            if(s.get(0).charAt(0) == '$') {

                command += scriptParameters.stream().filter(e -> e.getName().equals(s.get(0).substring(1))).findFirst().get().getValue().toString();
            }else
                command += s.get(0);
            command += " ";
        }
        return  command;
    }

    @Override
    public String toString() {
        return "TestConfig{" +
                "baseCommand='" + baseCommand + '\'' +
                ", objectiveFileName='" + objectiveFileName + '\'' +
                ", algorithmName='" + algorithmName + '\'' +
                ", objectiveContainer=" + objectiveContainer +
                ", scriptParameters=" + scriptParameters +
                ", iterationCount=" + iterationCount +
                '}';
    }
    public TestConfig() {
        objectiveContainer = new ObjectiveContainer();
        scriptParameters = new LinkedList<>();
        iterationCount = Optional.empty();
    }


    public void setObjectiveContainer(ObjectiveContainer objectiveContainer) {
        this.objectiveContainer = objectiveContainer;
    }

    private ObjectiveContainer objectiveContainer;
    private List < Param> scriptParameters;

    public void setOptimizerParameters(List<Param> optimizerParameters) {
        this.optimizerParameters = optimizerParameters;
    }

    @Deprecated
    public TestConfig(String configFileName) throws FileNotFoundException {
        scriptParameters = new LinkedList<>();
        this.objectiveContainer = new ObjectiveContainer();
        Scanner s = new Scanner(new BufferedReader(new FileReader(configFileName)));

            if(s.hasNextLine()) {
                baseCommand = s.nextLine();
                logger.info("Com = "+ this.baseCommand);
            }
            if(s.hasNextLine()){
                    int paramCounter = 0;

                    String line;
                    while (s.hasNextLine()) {
                        line = s.nextLine();
                        if (line.equals("")) continue;
                        logger.info("Line = "+ line);
                        if(line.contains("OBJ") && !line.contains("OBJECTIVEFILE")){
                            String[]  words = line.split(" ");
                            ObjectiveContainer oc = new ObjectiveContainer();

                            if(words.length==4){
                                logger.info("GET an objective ");
                                String name = words[1];
                                String relString = words[2];
                                String valString = words[3];
                                // TODO: 04/04/17 default value init

                                if(Utils.isInteger(valString))
                                    this.objectiveContainer.objectives.add( new ObjectiveContainer.Objective(Utils.Relation.valueOf(words[2]), false,name,0,Integer.parseInt(valString),0,1));
                                if(Utils.isFloat(valString))
                                    this.objectiveContainer.objectives.add( new ObjectiveContainer.Objective(Utils.Relation.valueOf(words[2]), false,name,0,Float.parseFloat(valString),0f,1));
                                if(Utils.isBoolean(valString))
                                    this.objectiveContainer.objectives.add( new ObjectiveContainer.Objective(Utils.Relation.valueOf(words[2]), false,name,!Boolean.parseBoolean(valString),Boolean.parseBoolean(valString),false,1));


                            }
                            continue;

                        }
                        if(line.trim().toUpperCase().equals("OBJECTIVEFILE")){

                            this.objectiveFileName = s.nextLine();
                            logger.info("File = "+ this.objectiveFileName);

                            continue;
                        }
                        // TODO: 04/04/17 itertions as objective 
//                        if(line.trim().toUpperCase().equals("ITERATION")){
//                            System.out.println("iteration");
//                            Integer i = Integer.parseInt(s.next());
//                            this.iterationCount = Optional.of(i);
//                            System.out.println("IterationCount = "+this.iterationCount.get());
//                          //  this.objectiveContainer.objectives.put("iteration",Utils.Relation.EQUALS,i,)
//                                continue;
//                        }
                        // TODO: 04/04/17 handle wrong line
                        if(line.contains("ITERATION")){

                            System.out.println("iteration");
                            Integer i = Integer.parseInt(line.split(" ")[1]);
                            this.iterationCount = Optional.of(i);
                            logger.info("IterationCount = "+this.iterationCount.get());
                            //  this.objectiveContainer.objectives.put("iteration",Utils.Relation.EQUALS,i,)
                            continue;
                        }
                        if(line.contains("$")) {
                            logger.info("param:  " + line);
                            Scanner lsc = new Scanner(line);

                            String parameterName = lsc.next();
                            Float d1;
                            if (parameterName.charAt(0) != '$' && Utils.isFloat(parameterName)) {
                                d1 = Float.parseFloat(parameterName);
                                parameterName = Integer.toString(paramCounter);
                            } else
                                d1 = lsc.nextFloat();
                            Float d2 = lsc.nextFloat();
                            Float d3 = lsc.nextFloat();

                            scriptParameters.add( new Param(d1, d2, d3,parameterName));
                            paramCounter++;
                        }

                    }

              //  }
            }
        // TODO: 21/07/17 what is this?? 
            Collections.sort(scriptParameters);

    }





}
