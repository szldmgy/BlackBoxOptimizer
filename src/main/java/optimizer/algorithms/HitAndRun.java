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
import optimizer.utils.Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 07. 10..
 */

public class HitAndRun extends AbstractAlgorithm {

    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(1.f, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE,"max_step_size"));
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"max_number_of_runs"));
    }

    @Override
    public void updateParameters( List<Param> parameterMap, List<IterationResult> landscape) {

        try {
            if (is.hits != 0 && landscape.get(landscape.size() - 1).betterThan(landscape.get(landscape.size() - 1 - is.hits))) {
                is.hits = 0;
                updateParameters(parameterMap, landscape);
                return;
            }
            else {
                if(is.hits == (int)optimizerParams.get(1).getValue()) {
                    this.iterationCounter = this.config.getIterationCount().get();
                    return;
                }
                double[] s = new double[parameterMap.size()];
                Random gen = new Random();
                for (int i = 0; i < s.length; ++i) {
                    s[i] = (2 * gen.nextDouble()) - 1;
                }
                double c = ((Float) optimizerParams.get(0).getValue()).doubleValue();
                List<Param> center = landscape.get(landscape.size()-1-is.hits).getConfigurationClone();

                for (int i = 0; i < s.length; ++i) {
                    if (s[i] > 0 && ((Number) center.get(i).getUpperBound()).doubleValue() - ((Number) center.get(i).getValue()).doubleValue() < c * s[i])
                        c = (((Number) center.get(i).getUpperBound()).doubleValue() - ((Number) center.get(i).getValue()).doubleValue()) / s[i];
                    if (s[i] < 0 && ((Number) center.get(i).getValue()).doubleValue() - ((Number) center.get(i).getLowerBound()).doubleValue() < c * s[i])
                        c = (((Number) center.get(i).getValue()).doubleValue() - ((Number) center.get(i).getLowerBound()).doubleValue()) / s[i];
                }

                c *= gen.nextDouble();
                for (int i = 0; i < s.length; ++i) {
                    center.get(i).add(new Double(c * s[i]).floatValue());
                }
                Param.refillList(parameterMap,center);
                ++is.hits;
            }

        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, InternalState.class);

        }

    }

    @Override
    public void saveState(String internalStateBackupFileName) {
        if (this.config.getOptimizerStateBackupFilename() == null)
            return;
        else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, InternalState.class);
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
        int hits = 0;
    }
}
