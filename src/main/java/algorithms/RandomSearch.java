package algorithms;

import utils.IterationResult;
import utils.Param;

import java.util.List;
import java.util.Random;

/**
 * Created by peterkiss on 17/10/16.
 */
public class RandomSearch extends  AlgorithmFI {

    {
        this.allowedTypes.add(Integer.class);
        this.allowedTypes.add(Boolean.class);
        this.allowedTypes.add(String.class);
    }

    @Override
    public void updateParameters(List< Param> parameterMap, List<IterationResult> landscape/*, List<Param > optimizerParams*/) {
        System.out.println(parameterMap);
        Random rand = new Random();
        for(Param entry : parameterMap)
        {
            if(entry.isActive()) {
                if (entry.getValue() instanceof Float || entry.getValue() instanceof Double) {
                    float lb = ((Number)entry.getLowerBound()).floatValue();
                    float ub = ((Number)entry.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    float newVal = lb + r * (ub - lb);
                    entry.setInitValue(newVal);
                    //main.Main.log(Level.INFO,newVal);
                }
                if (entry.getValue() instanceof Integer) {
                    int newVal = (Integer) entry.getLowerBound() + rand.nextInt((Integer) entry.getUpperBound()-(Integer) entry.getLowerBound()) ;
                   entry.setInitValue(newVal);
                    //main.Main.log(Level.INFO,newVal);
                }
                if (entry.getValue() instanceof Boolean) {
                    boolean newVal = rand.nextBoolean();
                   entry.setInitValue(newVal);
                    //main.Main.log(Level.INFO,newVal);
                }
                if(entry.isEnumeration()){
                    int enumPos = rand.nextInt(entry.getActiveValueArray().length-1);
                    entry.setInitValue(entry.getActiveValueArray()[enumPos]);
                }
            }
        }

    }
    @Override
    public List<Param> getConfig(){
        return this.optimizerParams;
    }

}
