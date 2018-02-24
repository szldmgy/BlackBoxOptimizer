package optimizer.exception;

import optimizer.param.FunctionParam;
import optimizer.param.Param;
import optimizer.param.ParameterDependency;
import org.junit.Test;

import javax.script.ScriptException;

import static org.junit.Assert.assertTrue;

public class InvalidParameterValueExceptionTest {

    @Test(expected = InvalidParameterValueException.class)
    public void addDependencyWithInvalidBoundary1(){

            Param enump = new Param("1", new String[]{"1","2","3"},"e");
            ParameterDependency pd  = new ParameterDependency(null, null,enump,"0","2");

    }

    @Test(expected = InvalidParameterValueException.class)
    public void addDependencyWithInvalidBoundary2(){

        Param enump = new Param("1", new String[]{"1","2","3"},"e");
        ParameterDependency pd  = new ParameterDependency(null, null,enump,"1","15");

    }

    @Test(expected = InvalidParameterValueException.class)
    public void addDependencyWithInvalidBoundary3() throws ScriptException {
        Param funcp = new FunctionParam("f", "2*$x", 10);
        funcp.setInitValue(2f);
        ParameterDependency pd = new ParameterDependency(null, null, funcp, 1f, 5f);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void setWrongInitValueException() throws ScriptException {
        Param funcp = new FunctionParam("f", "2*$x", 10);
        funcp.setInitValue(1f);
    }


}
