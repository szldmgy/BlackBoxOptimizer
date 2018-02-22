package general;

import optimizer.algorithms.AbstractAlgorithm;
import org.junit.Test;
import optimizer.exception.OptimizerException;
import optimizer.param.Param;
import optimizer.config.TestConfig;
import optimizer.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParallelTests extends StressTestBase {
    static Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses = new HashMap<Class<? extends AbstractAlgorithm>,String>();

    @Test
    public void runAll1() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, OptimizerException, CloneNotSupportedException {
        optimizerClasses = Utils.findAllMatchingTypes(AbstractAlgorithm.class, Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);
        File[] files = new File("Examples/").listFiles();
        testFiles(files);
    }

    // helper method:
    public  void testFiles(File[] files) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, OptimizerException, CloneNotSupportedException {
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
                    for(String name: new String[]{"RandomSearch","GridSearch"} ) {
                        Class<? extends AbstractAlgorithm> c = config.getOptimizerClassBySimpleName(name);

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

                        writer.write(config.runOptimizer( testExpPath,testBackupPath,testExpPath+"/"+tn + "_"+c.getSimpleName()+".json"));
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

}
