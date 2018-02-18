package utils;

import main.Main;
import org.apache.log4j.Level;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by peterkiss on 01/04/17.
 */
public class Trial implements Callable<IterationResult> {
    private final Boolean useFile;
    String baseCommand,fileName;
    ObjectiveContainer pattern;
    long startTime, delta;
    List<Param> config;
    public Trial(String command, boolean fileProcessOutPut, String fileName, ObjectiveContainer pattern, List<Param> config, long startTime, long delta){
        this.baseCommand = command;
        this.useFile = fileProcessOutPut;
        this.fileName = fileName;
        this.config= config;
        this.pattern = pattern;
        this.startTime = startTime;
        this.delta = delta;

    }


    @Override
    public IterationResult call() throws Exception {
        String command = TestConfig.getCommand(this.config,baseCommand);
        BufferedReader outputReader, errorReader;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(command);
        pr.waitFor();

        errorReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
        if(this.useFile) {
            FileReader fr = new FileReader(fileName);
            outputReader = new BufferedReader(fr);
        }
        else
            outputReader= new BufferedReader(new InputStreamReader(pr.getInputStream()));

        return new IterationResult(this.config, ObjectiveContainer.readObjectives(outputReader,errorReader,this.pattern),startTime,delta);


    }


}
