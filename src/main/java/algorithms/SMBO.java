package algorithms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import smile.math.kernel.GaussianKernel;
import utils.GaussianProcessRegressionWithVariance;
import utils.IterationResult;
import utils.Param;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Created by david on 2017. 08. 14..
 */
public class SMBO extends AlgorithmFI{
    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,1000,0,"number_of_random_points"));
        this.optimizerParams.add(new Param(1.f, Float.MAX_VALUE,0.0001f, "sigma_of_Gaussian_kernel"));
        this.optimizerParams.add(new Param(0.01, 1, 0, "lambda"));
        this.optimizerParams.add(new Param(1000, 1, 1000, "grid_size"));
    }



    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {

            if(is.randomPoints < (int)optimizerParams.get(0).getValue()) {
                Random rand = new Random();
                for(Param entry : parameterMap) {
                    float lb = ((Number)entry.getLowerBound()).floatValue();
                    float ub =((Number)entry.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    float newVal = lb + r * (ub - lb);
                    entry.setInitValue(newVal);
                }
                ++is.randomPoints;
                return;
            }
            if(is.randomPoints == (int)optimizerParams.get(0).getValue()) {
                is.fmin = landscape.get(0).getFitness();
                for(int i = 0; i < is.randomPoints; ++i) {
                    if(landscape.get(i).getFitness() < is.fmin )
                        is.fmin = landscape.get(i).getFitness();
                }
                ++is.randomPoints;
            }

            if(landscape.get(landscape.size()-1).getFitness() < is.fmin )
                is.fmin = landscape.get(landscape.size()-1).getFitness();

            GaussianProcessRegressionWithVariance< double[] > gp = buildModel(landscape);

            int n = parameterMap.size();
            double opt[] = new double[n];
            double lb[] = new double[n];
            double stepsize[] = new double[n];

            int gridsize = (int)optimizerParams.get(3).getValue();

            for(int i = 0; i < n; ++i) {
                lb[i] = ((Number)parameterMap.get(i).getLowerBound()).floatValue();
                opt[i] = ((Number) parameterMap.get(i).getLowerBound()).floatValue();
                stepsize[i] = (((Number)parameterMap.get(i).getUpperBound()).floatValue() - lb[i])/gridsize;
            }

            double optvalue = acquisition(opt, gp);

            int gridstate[] = new int[n];
            for(int i : gridstate) i = 0;
            int j = n-1;

            double d[] = new double[n];
            double v;

            while(j >= 0) {
                if(gridstate[j] < gridsize) {
                    ++ gridstate[j];
                    for(int i = 0; i < n; ++i)
                        d[i] = lb[i] + gridstate[i] * stepsize[i];
                    v = acquisition(d,gp);
                    if(v < optvalue) {
                        optvalue = v;
                        for(int i = 0; i < n; ++i)
                            opt[i] = d[i];
                    }
                    j = n-1;
                }
                else {
                    gridstate[j] = 0;
                    --j;
                }
            }

            for(int i = 0; i < n; ++i) {
                parameterMap.get(i).setInitValue(opt[i]);
            }

    }

    GaussianProcessRegressionWithVariance< double[] > buildModel(List<IterationResult> landscape) throws CloneNotSupportedException {

        double[][] tests = new double[landscape.size()][landscape.get(0).getConfiguration().size()];
        double[] results = new double[landscape.size()];

        for(int i = 0; i < landscape.size(); ++i ) {
            for(int j = 0; j < landscape.get(0).getConfiguration().size(); ++j) {
                tests[i][j] = ((Number)landscape.get(i).getConfiguration().get(j).getValue()).doubleValue();
            }
            results[i] = ((Number)landscape.get(i).getFitness()).doubleValue();
        }

        GaussianKernel gk = new GaussianKernel(((Number)optimizerParams.get(1).getValue()).doubleValue());

        GaussianProcessRegressionWithVariance<double[]> gaussp = new GaussianProcessRegressionWithVariance<>();

        try {
            GaussianProcessRegressionWithVariance<double[]> gp = new GaussianProcessRegressionWithVariance<>( tests , results, gk, ((Number)optimizerParams.get(2).getValue()).doubleValue());
            gaussp = gp;
        }
        catch (Exception e) {
            int alma = 0;
            System.out.println(alma);
        }
        return gaussp;
    }


    double phi(double x) {
        return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }

    double Phi(double z) {
        if (z < -8.0) return 0.0;
        if (z >  8.0) return 1.0;
        double sum = 0.0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum  = sum + term;
            term = term * z * z / i;
        }
        return 0.5 + sum * phi(z);
    }

    double acquisition(double[] x, GaussianProcessRegressionWithVariance<double[]> gp) {

        double v = gp.variance(x);
        double z = (is.fmin - gp.predict(x)) / v;
        double EI = v > 0 ? v * (z*Phi(z)+phi(z)) : 0;

        return EI > 0 ? EI : 0;
    }


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
        int randomPoints;
        double fmin;
    }
}
