package utils;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by peterkiss on 01/04/17.
 */
public class Trial implements Callable<IterationResult> {
    String command;
    public Trial(String command){
        this.command = command;
    }


    @Override
    public IterationResult call() throws Exception {
        return null;
    }
}
