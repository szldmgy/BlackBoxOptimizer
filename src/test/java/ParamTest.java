import org.junit.*;
import utils.Param;
import utils.TestConfig;

import java.io.FileNotFoundException;

import static org.junit.Assert.*;
/**
 * Created by peterkiss on 20/04/17.
 */
public class ParamTest {

    @Test
    public void someTest() throws FileNotFoundException {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Rosenbrock_multi_SimulatedAnnealing.json");
        assertTrue(tc.getOptimizerParameters().get(2).getAllValueArray().getClass().getComponentType()==Float.class);
    }
}
