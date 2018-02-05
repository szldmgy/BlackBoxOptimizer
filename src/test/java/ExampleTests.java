import algorithms.AlgorithmFI;
import main.Main;
import org.junit.Test;
import utils.Param;
import utils.TestConfig;
import utils.Utils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


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
    static Map<Class<? extends AlgorithmFI>,String> optimizerClasses = new HashMap<Class<? extends AlgorithmFI>,String>();

    @Test
    public void someTest() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        File directory = new File("testresults/");
        if (! directory.exists()){
            directory.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }
        optimizerClasses = Utils.findAllMatchingTypes(AlgorithmFI.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);
        File[] files = new File("Examples/").listFiles();
        testFiles(files);
    }

    public  void testFiles(File[] files) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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

                        File f = new File("testresults/"+tn + "_"+c.getSimpleName()+".csv");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

                        writer.write(config.runOptimizer(false, file.getName().replace(".json", ".csv")));
                        writer.close();
                        counter++;
                    }
                }



            }
        }
        System.out.println("Tests runned:  "+counter);
    }

   /* @Test
    public static void mainTest(File[] files) {

    }*/
}
