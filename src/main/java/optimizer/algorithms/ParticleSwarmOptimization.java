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

import optimizer.trial.IterationResult;
import optimizer.param.Param;
import optimizer.utils.Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 12..
 */
public class ParticleSwarmOptimization extends AbstractAlgorithm {
    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"swarm_size"));
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "omega"));
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "phi_p"));
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "phi_g"));
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {

            if(is.initialisedParticles == 0) {
                int dim = parameterMap.size();
                float[] blo = new float[dim];
                float[] bup = new float[dim];
                for(int i = 0; i < dim; ++i) {
                    blo[i] = ((Number)parameterMap.get(i).getLowerBound()).floatValue();
                    bup[i] = ((Number)parameterMap.get(i).getUpperBound()).floatValue();
                }
                is.swarm = new ArrayList<>();
                for(int i = 0; i < ((Number)optimizerParams.get(0).getValue()).intValue(); ++i) {

                    is.swarm.add(new Particle(dim,blo,bup));
                }
            }
            if(is.initialisedParticles < (int)optimizerParams.get(0).getValue()) {
                for(int i = 0; i < parameterMap.size(); ++i) {
                    parameterMap.get(i).setInitValue(is.swarm.get(is.initialisedParticles).position[i]);
                }
                if(is.initialisedParticles != 0)
                    is.swarm.get(is.initialisedParticles-1).bestFitness = (float) landscape.get(landscape.size()-1).getFitness();
                ++is.initialisedParticles;
                return;
            }
            if(is.initialisedParticles == (int)optimizerParams.get(0).getValue()) {
                is.swarm.get(is.initialisedParticles-1).bestFitness = (float) landscape.get(landscape.size()-1).getFitness();
                ++is.initialisedParticles;
                is.swarmBestKnownPosition = is.swarm.get(0).position.clone();
                is.swarmBestFitness = is.swarm.get(0).bestFitness;
                for(int i = 1; i < is.swarm.size(); ++i) {
                    if(is.swarm.get(i).bestFitness < is.swarmBestFitness) {
                        is.swarmBestFitness = is.swarm.get(i).bestFitness;
                        is.swarmBestKnownPosition = is.swarm.get(i).bestKnownPosition.clone();
                    }
                }
            }

            if(!is.firstStep) {
                if(landscape.get(landscape.size()-1).getFitness() < is.swarm.get(is.actualParticle).bestFitness) {
                    is.swarm.get(is.actualParticle).bestKnownPosition = is.swarm.get(is.actualParticle).position.clone();
                    is.swarm.get(is.actualParticle).bestFitness = (float) landscape.get(landscape.size()-1).getFitness();
                    if(is.swarm.get(is.actualParticle).bestFitness < is.swarmBestFitness) {
                        is.swarmBestFitness = is.swarm.get(is.actualParticle).bestFitness;
                        is.swarmBestKnownPosition = is.swarm.get(is.actualParticle).bestKnownPosition.clone();
                    }
                }
                is.firstStep = false;

                ++is.actualParticle;
                if(is.actualParticle == (int)optimizerParams.get(0).getValue())
                    is.actualParticle = 0;
            }

            for(int d = 0; d < parameterMap.size(); ++d) {
                Random rand = new Random();
                float rp = rand.nextFloat();
                float rg = rand.nextFloat();

                is.swarm.get(is.actualParticle).velocity[d] = is.swarm.get(is.actualParticle).velocity[d]
                        * /*(double)*/((Number)optimizerParams.get(1).getValue()).floatValue() +
                        /*(double)*/((Number)optimizerParams.get(2).getValue()).floatValue() * rp * (is.swarm.get(is.actualParticle).bestKnownPosition[d] - is.swarm.get(is.actualParticle).position[d]) +
                        /*(double)*/((Number)optimizerParams.get(3).getValue()).floatValue() * rg * (is.swarmBestKnownPosition[d] - is.swarm.get(is.actualParticle).position[d]);


                is.swarm.get(is.actualParticle).position[d] += is.swarm.get(is.actualParticle).velocity[d];
                parameterMap.get(d).setInitValue(is.swarm.get(is.actualParticle).position[d]);
            }



    }


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
    }

    @Override
    public void saveState(String internalStateBackupFileName) {

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }

    class Particle {
        float[] position;
        float[] velocity;
        float[] bestKnownPosition;
        float bestFitness;

        public Particle(int dim, float[] blo, float[] bup) {
            position = new float[dim];
            velocity = new float[dim];
            bestKnownPosition = new float[dim];

            Random rand = new Random();
            for(int i = 0; i < dim; ++i) {
                float r = rand.nextFloat();
                position[i] = blo[i] + r * (bup[i] - blo[i]);
                bestKnownPosition[i] = blo[i] + r * (bup[i] - blo[i]);
                r = rand.nextFloat();
                velocity[i] = blo[i] - bup[i] + 2 * r * (bup[i] - blo[i]);
            }
            bestFitness = 0f;

        }
    }

    class InternalState {
        ArrayList<Particle> swarm;
        int actualParticle;
        int initialisedParticles = 0;
        float[] swarmBestKnownPosition;
        float swarmBestFitness;
        boolean firstStep = true;

    }
}
