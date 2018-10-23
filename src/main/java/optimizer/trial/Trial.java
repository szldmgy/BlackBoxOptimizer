/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package optimizer.trial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import optimizer.config.TestConfig;
import optimizer.exception.JSONReadException;
import optimizer.objective.ObjectiveContainer;
import optimizer.param.Param;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * This class is intended to execute one trial in a separated thread.
 * Created by peterkiss on 01/04/17.
 */
public class Trial implements Callable<IterationResult> {

    public static Trial deserializeTrial(String s){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Trial.class, new TrialDeserializer());
        Gson gson = gsonBuilder.create();
        try {
            return gson.fromJson(s, Trial.class);
        }catch (Exception e){
            throw new JSONReadException("Error during deserialization of JSON");
        }


    }
    /**
     * A member specifying whether the executed process writes its results into a file or on the standard output
     * Since using the same filename in parallel executed instances of the process can be problematic now it is set up to false all the times.
     */
    private final Boolean useFile;
    /**
     * The command pattern, where recent values of the parameters will be inserted.
     */
    private String baseCommand;
    /**
     * Name of the output file of the executed process.
     */
    private String processOutputFileName;
    /**
     * This  member contains an instance of an {@link ObjectiveContainer}, storing a prototype of the objective values to be read at the end of the execution of the runned process.
     */
    private ObjectiveContainer pattern;
    /**
     * Stores the timestamp of starting moment of the process.
     */
    long startTime;
    /**
     * stores the time difference stemming from the possible interruption of optimization.
     */
    long delta;
    /**
     * The recent configuration of {@link Param}, with those the process will be run here.
     */
    List<Param> config;

    /**
     * working directory to execute the BBF.
     */
    String workingDir;
    public Trial(String baseCommand, boolean fileProcessOutPut, String processOutputFileName, ObjectiveContainer pattern, List<Param> config, long startTime, long delta, String workingDir){
        this.baseCommand = baseCommand;
        this.useFile = fileProcessOutPut;
        this.processOutputFileName = processOutputFileName;
        this.config= config;
        this.pattern = pattern;
        this.startTime = startTime;
        this.delta = delta;
        this.workingDir = workingDir;

    }

    /**
     * Here we execute the trial with parameters specified in {@link #config}.
     * @return The result of the trial.
     * @throws Exception
     */
    @Override
    public IterationResult call() throws Exception {
        String command = TestConfig.getCommand(this.config,baseCommand);
        BufferedReader outputReader, errorReader;
        //Runtime rt = Runtime.getRuntime();
        ProcessBuilder builder = new ProcessBuilder( command.split(" "));
        builder.directory(new File(this.workingDir).getAbsoluteFile());
        builder.redirectErrorStream(true);
        //builder.inheritIO();
        System.out.println("Executing1: "+builder.command());
        Process pr = null;
        //builder.redirectOutput();
        try {
             pr = builder.start();//= rt.exec(command);
        }catch (Exception e){
            e.printStackTrace();
        }
        int res = pr.waitFor();

        errorReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
        if(this.useFile) {
            FileReader fr = new FileReader(processOutputFileName);
            outputReader = new BufferedReader(fr);
        }
        else
            outputReader= new BufferedReader(new InputStreamReader(pr.getInputStream()));
        System.out.println("RES = "+res);

        return new IterationResult(this.config, ObjectiveContainer.readObjectives(outputReader,errorReader,this.pattern),startTime,delta);


    }


}
