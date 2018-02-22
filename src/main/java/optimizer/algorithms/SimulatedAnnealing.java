package optimizer.algorithms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import optimizer.param.FunctionParam;
import optimizer.trial.IterationResult;
import optimizer.param.Param;

import javax.script.ScriptException;
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

// optimizerParams[0] : max step size
// optimizerParams[1] : max number of runs
// optimizerParams[i] : T_{i-2}

public class SimulatedAnnealing extends AbstractAlgorithm {

    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10f,10f,0.0001f,"max_step_size"));
        this.optimizerParams.add(new Param(3,1000,1,"max_number_of_runs"));
        // TODO: 09/08/17 js compatible closed formula
        try {
            this.optimizerParams.add(new FunctionParam("temperature","1/(1+$alma)",100));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateParameters( List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {


        if (is.hits != 0) {
            Random gen = new Random();
            double p = gen.nextDouble();

           double c = landscape.get(landscape.size() - 1).getFitness() - (landscape.get(landscape.size() - 1 - is.hits)).getFitness();

            Float[] ti = (Float[])optimizerParams.get(2).getAllValueArray();
            c = Math.exp(-c / ti[is.centers]);
            if(p < c) {
                ++is.centers;
                is.hits = 0;
                updateParameters(parameterMap, landscape);
                return;
            }
        }


        double[] s = new double[parameterMap.size()];
        Random gen = new Random();
        for (int i = 0; i < s.length; ++i) {
            s[i] = (2 * gen.nextDouble()) - 1;
        }
        double c = ((Number) optimizerParams.get(0).getValue()).doubleValue();
        List<Param> center = landscape.get(landscape.size()-1-is.hits).getConfiguration();

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


        if(is.hits == (int)optimizerParams.get(1).getValue()) {
            this.iterationCounter = this.config.getIterationCount().get();
            return;
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
        int centers = 0;
    }
}
