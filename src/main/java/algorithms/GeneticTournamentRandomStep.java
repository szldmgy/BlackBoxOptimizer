package algorithms;

import utils.IterationResult;
import utils.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 04..
 */

//optimizerParams[2] : max step size
public class GeneticTournamentRandomStep extends Genetic {

    {
        this.optimizerParams.add(new Param(10.0f,10000f,0.0001f,"max_step_size"));
    }

    @Override
    protected void select(List<IterationResult> landscape) {

        try {
            double fitnessSum = landscape.get(is.bestChromosome).getFitness();
            for(int i = (is.generation-1)*((int)optimizerParams.get(0).getValue()-1); i < is.generation*((int)optimizerParams.get(0).getValue()-1); ++i)
                fitnessSum += landscape.get(i).getFitness();
            Random rand = new Random();
            double tresholdf = fitnessSum * rand.nextDouble();
            double tresholdm = fitnessSum * rand.nextDouble();

            if(landscape.get(is.bestChromosome).getFitness() >= tresholdf)
                is.father = is.bestChromosome;
            else {
                double s = landscape.get(is.bestChromosome).getFitness();
                for (int i = (is.generation - 1) * ((int) optimizerParams.get(0).getValue() - 1); i < is.generation * ((int) optimizerParams.get(0).getValue() - 1); ++i) {
                    s += landscape.get(i).getFitness();
                    if (s >= tresholdf) {
                        is.father = i;
                        break;
                    }
                }
            }

            if(landscape.get(is.bestChromosome).getFitness() >= tresholdm)
                is.mother = is.bestChromosome;
            else {
                double s = landscape.get(is.bestChromosome).getFitness();
                for(int i = (is.generation-1)*((int)optimizerParams.get(0).getValue()-1); i < is.generation*((int)optimizerParams.get(0).getValue()-1); ++i) {
                    s += landscape.get(i).getFitness();
                    if(s >= tresholdm) {
                        is.mother = i;
                        break;
                    }
                }

            }

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected List<Param> crossover(List<IterationResult> landscape, int mother, int father) {
        List<Param> result = new ArrayList<>();

        Random rand = new Random();
        try {
            for(int i = 0; i < landscape.get(0).getConfiguration().size(); ++i) {
                boolean b = rand.nextBoolean();
                result.get(i).setInitValue(b ? landscape.get(father).getConfiguration().get(i).getValue() :
                        landscape.get(mother).getConfiguration().get(i).getValue());
            }

            double[] s = new double[result.size()];
            for (int i = 0; i < s.length; ++i) {
                s[i] = (2 * rand.nextDouble()) - 1;
            }
            double c = (Double) optimizerParams.get(2).getValue();

            for (int i = 0; i < s.length; ++i) {
                if (s[i] > 0 &&((Number) result.get(i).getUpperBound()).doubleValue() - ((Number) result.get(i).getValue()).doubleValue() < c * s[i])
                    c = (((Number) result.get(i).getUpperBound()).doubleValue() - ((Number) result.get(i).getValue()).doubleValue()) / s[i];
                if (s[i] < 0 &&((Number) result.get(i).getValue()).doubleValue() - ((Number) result.get(i).getLowerBound()).doubleValue() < c * s[i])
                    c = (((Number) result.get(i).getValue()).doubleValue() - ((Number) result.get(i).getLowerBound()).doubleValue()) / s[i];
            }

            c *= rand.nextDouble();
            for (int i = 0; i < s.length; ++i) {
                result.get(i).add(c * s[i]);
            }

        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return result;
    }
}
