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

package optimizer.algorithms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import optimizer.trial.IterationResult;
import optimizer.param.Param;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 04..
 */

// optimizerParams[0] : population size
// optimizerParams[1] : number of populations

public abstract class Genetic extends AbstractAlgorithm {
    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(3,1000,1,"population_size"));
        this.optimizerParams.add(new Param(3,1000,1,"population_number"));
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {

        Random rand = new Random();

        try {
            if(is.generation == 0 && is.population < (int)optimizerParams.get(0).getValue()) {
                for(Param p : parameterMap) {
                    float lb = ((Number)p.getLowerBound()).floatValue();
                    float ub = ((Number)p.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    p.setInitValue(lb + r * (ub - lb));
                }
                ++is.population;
                return;
            }
            if(is.population == (int)optimizerParams.get(0).getValue()) {
                for(int i = (is.generation + 1) * (is.population - 1); i > is.generation * (is.population - 1); --i )
                if(landscape.get(i).betterThan(landscape.get(is.bestChromosome)))
                    is.bestChromosome = i;
                is.population = 1;
                ++is.generation;
            }

            select(landscape);
            List<Param> child = crossover(landscape, is.mother, is.father);
            Param.refillList(parameterMap, child);


        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }

    protected abstract void select(List<IterationResult> landscape);
    protected abstract List<Param> crossover(List<IterationResult> landscape, int mother, int father) throws CloneNotSupportedException;


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, HitAndRun.InternalState.class);

        }

    }

    @Override
    public void saveState(String internalStateBackupFileName) {
        if (this.config.getOptimizerStateBackupFilename() == null)
            return;
        else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, HitAndRun.InternalState.class);
            try{
                //PrintWriter writer = new PrintWriter(getAlgorithmSimpleName()+"_params.json", "UTF-8");
                PrintWriter writer = new PrintWriter(this.config.getOptimizerStateBackupFilename(), "UTF-8");
                writer.println(s);
                //writer.println("The second line");
                writer.close();
            } catch (IOException e) {
                // do something
            }

        }

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }


    class InternalState {

        int generation = 0;
        int population = 1;

        int bestChromosome = 0;

        int mother;
        int father;

    }
}
