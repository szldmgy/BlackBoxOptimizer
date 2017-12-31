package algorithms;

import utils.IterationResult;
import utils.Param;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 12..
 */
public class DifferentialEvolution extends AlgorithmFI {
    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"population_size"));
        this.optimizerParams.add(new Param(0.5,Float.MAX_VALUE,Float.MIN_VALUE,"phi"));
        this.optimizerParams.add(new Param(0.5,Float.MAX_VALUE,Float.MIN_VALUE,"kappa"));
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {

        try {
            Random rand = new Random();

            if(is.firstGeneration) {
                for(Param p : parameterMap) {
                    float lb = ((Number)p.getLowerBound()).floatValue();
                    float ub = ((Number)p.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    p.setInitValue(lb + r * (ub - lb));
                }
                ++is.actualIndividual;
                if(is.actualIndividual == ((Number)optimizerParams.get(0).getValue()).intValue()) {
                    is.firstGeneration = false;
                    is.firstMutation = true;
                    is.members = new int[(int)optimizerParams.get(0).getValue()];
                    is.newmembers = new int[(int)optimizerParams.get(0).getValue()];
                    for(int i = 0; i < is.members.length; ++i)
                        is.members[i] = i;
                    is.actualIndividual = 0;
                }
                return;
            }

            if(!is.firstMutation) {
                is.newmembers[is.actualIndividual] = landscape.get(landscape.size()-1).getFitness() > landscape.get(is.members[is.actualIndividual]).getFitness() ?
                        landscape.size()-1 : is.members[is.actualIndividual];
                ++is.actualIndividual;
                if(is.actualIndividual == is.members.length) {
                    is.members = is.newmembers.clone();
                    is.actualIndividual = 0;
                }
            }

            int a,b,c;
            a = rand.nextInt(is.members.length-1);
            b = rand.nextInt(is.members.length-1);
            c = rand.nextInt(is.members.length-1);

            List<Param> xm = new ArrayList<>();
            Param.refillList(xm,landscape.get(is.members[a]).getConfiguration());
            for(int i = 0; i < xm.size(); ++i) {
                xm.get(i).add((((Number)landscape.get(is.members[b]).getConfiguration().get(i).getValue()).floatValue() -
                        ((Number)landscape.get(is.members[c]).getConfiguration().get(i).getValue()).floatValue())*((Number)optimizerParams.get(1).getValue()).floatValue());
            }
            Param.refillList(is.xc,landscape.get(is.members[is.actualIndividual]).getConfiguration());
//// TODO: 23/09/17  
            for(int i = 0; i < is.members.length && i<is.xc.size(); ++i) {
                float r = rand.nextFloat();
                if(r < (float)optimizerParams.get(2).getValue()) {
                    is.xc.get(i).setInitValue(xm.get(i).getValue());
                }
            }


        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
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


    class InternalState {
        boolean firstGeneration = true;
        int actualIndividual = 0;
        int[] members;
        int[] newmembers;
        List<Param> xc = new ArrayList<>();
        boolean firstMutation = false;

    }
}
