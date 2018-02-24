package optimizer.algorithms;

import optimizer.trial.IterationResult;
import optimizer.param.Param;

import java.util.List;
import java.util.Random;

/**
 * Created by peterkiss on 17/10/16.
 */
public class RandomSearch extends AbstractAlgorithm {

    {
        this.parallelizable = true;
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
                if(entry.isEnumeration()){
                    int enumPos = rand.nextInt(entry.getActiveValueArray().length-1);
                    entry.setInitValue(entry.getActiveValueArray()[enumPos]);
                }
                else if (entry.getValue() instanceof Float ) {
                    float lb = ((Number)entry.getLowerBound()).floatValue();
                    float ub = ((Number)entry.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    float newVal = lb + r * (ub - lb);
                    entry.setInitValue(newVal);
                    //optimizer.main.Main.log(Level.INFO,newVal);
                }
                else if (entry.getValue() instanceof Integer) {
                    Integer u = (Integer) entry.getUpperBound();
                    Integer l = (Integer) entry.getLowerBound();
                    int newVal = l + rand.nextInt(u-l) ;
                   entry.setInitValue(newVal);
                    //optimizer.main.Main.log(Level.INFO,newVal);
                }
                else if (entry.getValue() instanceof Boolean) {
                    boolean newVal = rand.nextBoolean();
                   entry.setInitValue(newVal);
                    //optimizer.main.Main.log(Level.INFO,newVal);
                }

            }
        }

    }
    @Override
    public List<Param> getConfig(){
        return this.optimizerParams;
    }

}
