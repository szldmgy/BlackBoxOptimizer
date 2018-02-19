import algorithms.AbstractAlgorithm;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StressTestBase {
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


    static Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses = new HashMap<Class<? extends AbstractAlgorithm>,String>();
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
       // deleteDirectory(new File(testResultsPath));
        deleteDirectory(new File(testExpPath));
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
