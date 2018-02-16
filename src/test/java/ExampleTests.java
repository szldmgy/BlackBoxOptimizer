import algorithms.AlgorithmFI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.Main;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import utils.OptimizerException;
import utils.Param;
import utils.TestConfig;
import utils.Utils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Created by peterkiss on 2018. 01. 22..
 */
public class ExampleTests {
    //available algorithms
    // development path
    final static String defaultOptimizerClassLocation =  "target/classes/algorithms/";

    final static String outputfile =  "BlackBoxOptimizer/target/classes/algorithms/";

    //jar path
    final static String defaultJarOptimizerClassLocation =  "lib/algorithms/";


    //path for result files
    static String resultsPath = "results";
    //path for experiment descriptors
    static String expPath = "experiments";
    //path for backup files
    static String backupPath = "backup";

    //path for test result files
    static String testResultsPath = "results/tests";
    //path for test experiment descriptors
    static String testExpPath = "experiments/tests";
    //path for test backup files
    static String testBackupPath = "backup/tests";
    //path for hardcore setup files
    static String testResourcesPath = "src/test/resources/";

    static FileWriter cllogFileW;


    static Map<Class<? extends AlgorithmFI>,String> optimizerClasses = new HashMap<Class<? extends AlgorithmFI>,String>();
    @BeforeClass
    public static void setup() throws IOException {

         cllogFileW = new FileWriter(new File("Cllog.log"));
        File directory = new File(resultsPath);
        if (! directory.exists()){
            directory.mkdir();

        }
        directory = new File(expPath);
        if (! directory.exists()){
            directory.mkdir();

        }
        directory = new File(backupPath);
        if (! directory.exists()){
            directory.mkdir();

        }

         directory = new File(testResultsPath);
        if (! directory.exists()){
            directory.mkdir();

        }
         directory = new File(testExpPath);
        if (! directory.exists()){
            directory.mkdir();

        }
         directory = new File(testBackupPath);
        if (! directory.exists()){
            directory.mkdir();

        }

    }
    @AfterClass
    public static void cleanup() throws IOException {
        cllogFileW.close();
        deleteDirectory(new File(testBackupPath));
        deleteDirectory(new File(testResultsPath));
        deleteDirectory(new File(testExpPath));
    }
    //testing all the example setups with all  applicable algorithms
    @Test
    public void runAll1() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, OptimizerException {
        optimizerClasses = Utils.findAllMatchingTypes(AlgorithmFI.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);
        File[] files = new File("Examples/").listFiles();
        testFiles(files);
    }

    // helper method:
    public  void testFiles(File[] files) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, OptimizerException {
        int counter = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Directory: " + file.getName());
                testFiles(file.listFiles()); // Calls same method again.
            } else {
                if(file.getName().endsWith(".json")) {
                    String tasknameparts[] = file.getName().replace(".json","").split("/");
                    String tn = tasknameparts[tasknameparts.length-1];
                    TestConfig config = TestConfig.readConfigJSON(file);
                    config.setOptimizerClasses(optimizerClasses);
                    Map<String, List<Param>> algParamMap = config.filterAlgorithms();
                    for(String name: algParamMap.keySet() ) {
                        Class<? extends AlgorithmFI> c = config.getOptimizerClassBySimpleName(name);

                        System.out.println("============================");
                        System.out.println(file.getName()+" ==> "+c.getSimpleName());
                        System.out.println("============================");

                        config.setAlgorithmName(c.getSimpleName());
                        config.setOptimizerParameters(algParamMap.get(name));
                        config.setIterationCount( Optional.of(10) );
                        config.setIterationCounter( 0);
                        config.clearLandscape();

                        File f = new File(testResultsPath+"/"+tn + "_"+c.getSimpleName()+".csv");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

                        writer.write(config.runOptimizer(false, testExpPath,testBackupPath,testExpPath+"/"+tn + "_"+c.getSimpleName()+".json"));
                        writer.close();
                        String fn = testExpPath+"/"+tn + "_"+c.getSimpleName()+".json";
                        config.wirteExperimentDescriptionFile(fn);


                        counter++;
                    }
                }



            }
        }
        System.out.println("Tests runned:  "+counter);
    }

    @Test
    public void complicatedTest() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, OptimizerException {


        optimizerClasses = Utils.findAllMatchingTypes(AlgorithmFI.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);
        File[] files = new File(testResourcesPath+"/Test/").listFiles();
        testFiles(files);
    }

    //test command_line usage for all setup generated in runAll1
    @Test
    public void runAll2(){

        File[] files = new File(testExpPath+"/").listFiles();
        testClFiles(files);

    }

    @Test
    public void runAll3(){

        File[] files = new File(testBackupPath+"/").listFiles();
        testRecovery(files);

    }

    private void testRecovery(File[] files) {

        Arrays.stream(files).filter(file -> file.getName().contains(".json")).forEach(file -> {
           /* File directory = new File(testResultsPath+"/"+ file.getName().replace(".json",""));
            if (! directory.exists()){
                directory.mkdir();

            }*/
            System.out.println("============================");
            System.out.println(file.getName() +" ==> recovery");
            System.out.println("============================");

            //avoid looping
            try {
                TestConfig tc = TestConfig.readConfigJSON(file.getAbsolutePath());
                tc.setSavingFrequence(-1);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String s = gson.toJson(tc, TestConfig.class);
                FileWriter w = new FileWriter(file.getAbsolutePath().toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] args = new String[]{"-r","-sp","3", file.getAbsolutePath()};
            try {
                cllogFileW.write(String.join(" ",args));
                Main.main(args);
            } catch (IOException e) {
                fail(e.getMessage());
            } catch (CloneNotSupportedException e) {
                fail(e.getMessage());            }
            try {
                // not working because bad configurations are not written in csv
                // assertTrue(TestConfig.readConfigJSON(file.getAbsolutePath().replace(testBackupPath, testExpPath)).getIterationCount().get()<=Files.lines(Paths.get(file.getAbsolutePath().replace(testBackupPath, testResultsPath).replace(".json", ".csv"))).count()-1);
                assertTrue(TestConfig.readConfigJSON(file.getAbsolutePath().replace(testBackupPath, testExpPath)).getIterationCounter()<=TestConfig.readConfigJSON(file.getAbsolutePath().replace(testBackupPath, testExpPath)).getIterationCount().get());
            } catch (IOException e) {
                fail(e.getMessage());
            }

        });
    }

    //helper method for runall2
    private void testClFiles(File[] files) {
        Arrays.stream(files).filter(file -> file.getName().contains(".json")&&!file.getName().contains("_cl.json")).forEach(file -> {
            File directory = new File(testResultsPath+"/"+ file.getName().replace(".json",""));
            if (! directory.exists()){
                directory.mkdir();

            }
            System.out.println("============================");
            System.out.println(file.getName());
            System.out.println("============================");
            String clFileName = file.getAbsolutePath().replace(".json","_cl.json");
            try {
                TestConfig tc = TestConfig.readConfigJSON(file.getAbsolutePath());
                tc.setIterationCounter(0);
                tc.wirteExperimentDescriptionFile(clFileName);
            } catch (FileNotFoundException e) {
                fail(e.getMessage());
            }


            String[] args = new String[]{"-r","-sp","3",testExpPath+"/"+ new File(clFileName).getName()};
            try {
                cllogFileW.write(String.join(" ",args));
                Main.main(args);
            } catch (IOException e) {
                fail(e.getMessage());
            } catch (CloneNotSupportedException e) {
                fail(e.getMessage());
            }

        });


    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
