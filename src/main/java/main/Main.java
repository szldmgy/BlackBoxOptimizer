package main;

import algorithms.AbstractAlgorithm;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import utils.*;

import java.lang.reflect.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


/**
 * Main class of BlaBoO -  starts a standalone optimization task, or instanitate a {@link BrowserInterface} object.
 * Created by peterkiss on 14/10/16.
 *
 */


public class Main {

    static Logger logger = Logger.getLogger(Main.class);


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

    /**
     * development path for the {@link AbstractAlgorithm} .class files
     */
    final static String defaultOptimizerClassLocation =  "target/classes/algorithms/";

    //final static String outputfile =  "BlackBoxOptimizer/target/classes/algorithms/";

    /**
     * deployment path for the {@link AbstractAlgorithm} .class files
     */
    final static String defaultJarOptimizerClassLocation =  "lib/algorithms/";

    /**
     * {@link java.util.Map} storing available algorithms, along with some predefined parametrization of them -this latter will be removed.
     */
    static Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses = new HashMap<Class<? extends AbstractAlgorithm>,String>();


    final static String outputDir = "results";
    final static String experimentDir = "experiments";
    final static String backupDir = "backup";
    final static String uploadDir = "upload";

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
    static String[] configFileName = {"examples/Rosenbrock/Rosenbrock_multi.json"};

    public static String getSaveFileName() {
        return saveFileName[0];
    }

    static String[] saveFileName = {null};// {"experiments/Wormhole_tuning_1.json1"};

    /**
     * Entry point of BlaBoO, either runs a a standalone optimization or starts backend of the Browser GUI. Creates the necessary folder structure.
     * @param args command line arguments for start up:
     *             "-r" indicates "run" = standalone mode
     *             "-s" switch on safe mode - takes a snapshot of the optimization by default at every 10th iteration
     *             "-sp <number of iteration>" making backups with a given frequency
     *             "<experiment JSON>" file name to load the setup of the experiment
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public static void main(String[] args) throws IOException, CloneNotSupportedException {

        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        if(args.length==1)
            configFileName[0] = args[0];
        String projectDir = System.getProperty("user.dir");
        String staticDir = "";

        File directory = new File(outputDir);
        if (! directory.exists()){
            directory.mkdir();
         }
        directory = new File(experimentDir);
        if (! directory.exists()) {
            directory.mkdir();
        }
        directory = new File(backupDir);
        if (! directory.exists()){
            directory.mkdir();
        }
        directory = new File(uploadDir);
        if (! directory.exists()){
            directory.mkdir();
        }

        boolean testmode=false;
        for(int i = 0; i< args.length;i++)
        {
            String s = args[i];
            if(s.equals("-r"))
                inmediateRun[0]=true;
            else if(s.equals("-s")) {
                safeMode[0] = true;
                savingFrequence[0]= 10;
            }
            else if(s.equals("-sp")) {
                safeMode[0] = true;
                savingFrequence[0] = Integer.parseInt(args[++i]);
            }
            /*else if(s.equals("-o")) {
                saveFileName[0] = args[++i];
            }
            else if(s.equals("-p")) {
                customParamFile[0] = true;
                customParamFileName[0] = args[i++];
            }*/
            else {
                configFileName[0] = s;

            }

            if(configFileName[0].contains("test"))
                testmode = true;
        }


        final TestConfig[] config = new TestConfig[1];

        String[] algorithmName = new String[1];


        //find all availible optimizer algorithms
        optimizerClasses = Utils.findAllMatchingTypes(AbstractAlgorithm.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);

        //load config from json file - this branch supposed to be the only path now
        if (configFileName[0].contains(".json")) {


            if(inmediateRun[0])
            {
                try {
                    String locationModifier = testmode?"/tests":"";

                    try {
                        config[0] = TestConfig.readConfigJSON(configFileName[0]);
                    }catch (Exception e)
                    {
                        System.out.println("Inproper json file! Use the GUI if you want to make sure make sure..");
                        return;
                    }
                    config[0].setOptimizerClasses(optimizerClasses);
                    config[0].setSavingFrequence(savingFrequence[0]);

                    String experimentName = Utils.getExperimentUniqueName(configFileName[0],experimentDir+locationModifier);
                    //String expFileName = Utils.getExpJSONFileName(experimentName,experimentDir) ;
                    String resFileName = Utils.getExpCSVFileName(Utils.getExperimentName(experimentName),outputDir+locationModifier);


                    config[0].runAndGetResultfiles(experimentName, resFileName,experimentDir+locationModifier, backupDir+locationModifier);

                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (OptimizerException e) {
                    System.out.println(e.getMessage());
                }
                return;
            }
            else {
                saveFileName[0] = Utils.getExperimentUniqueName(configFileName[0],experimentDir);
                BrowserInterface bi = new BrowserInterface(configFileName[0], optimizerClasses, projectDir, staticDir, experimentDir, outputDir, backupDir,uploadDir,saveFileName[0]);
                bi.run();
            }

        }

    }
}
