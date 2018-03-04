package optimizer.algorithms;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import optimizer.param.DummyParam;
import optimizer.trial.IterationResult;
import optimizer.param.Param;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Grid Search
 * Supported parameter types: float, integer, boolean, enumeration
 * Parallelizable
 *
 * Search on a points of a grid. The step size in the direction of the i-th axis is stored in optimizerparams.get(i).
 * Created by david on 17/02/17.
 */
public class GridSearch extends AbstractAlgorithm {


    InternalState is = new InternalState();

    {
        this.parallelizable = true;
        this.optimizerParams = new LinkedList<>();
        this.allowedTypes.add(Integer.class);
        this.allowedTypes.add(Boolean.class);
        this.allowedTypes.add(String.class);


    }
    //@Override
// TODO: 01/08/17 enumertion types, boolean
    @Override
    public void updateParameters(List< Param> parameterMap, List<IterationResult> landscape/*, List<Param > optimizerParams*/) {
        //List<Param> lastConfiguration = null;

            if(is.lastConfiguration==null)
                is.lastConfiguration = parameterMap;
        //this is not working in multithread setup!!! -> removed
        /*try {
            if(landscape.isEmpty())
                lastConfiguration = parameterMap;
            else
                lastConfiguration = landscape.get(landscape.size()-1).getConfigurationClone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }*/
        int i = is.lastConfiguration.size()-1;
        while(i >= 0) {
            if(optimizerParams.get(i).getValue().equals("dummy")) {
                i--;
                continue; //we dont care about unreachable variables
            }
            if(this.config.getScriptParametersReference().get(i).isActive()) {
                if(this.config.getScriptParametersReference().get(i).isNumeric())
                {
                        if(((Number)is.lastConfiguration.get(i).getValue()).doubleValue() + ((Number)optimizerParams.get(i).getValue()).doubleValue() <=
                            ((Number)is.lastConfiguration.get(i).getUpperBound()).doubleValue()) {
                        this.config.getScriptParametersReference().get(i).add(optimizerParams.get(i).getValue());
                        break;
                    }
                    else
                        this.config.getScriptParametersReference().get(i).setInitValue(is.lastConfiguration.get(i).getLowerBound());
                }
                else if(is.lastConfiguration.get(i).isBoolean()) {
                    if ((Boolean) is.lastConfiguration.get(i).getValue()) {
                        this.config.getScriptParametersReference().get(i).setInitValue(false);
                    } else {
                        this.config.getScriptParametersReference().get(i).setInitValue(true);
                        break;
                    }
                }
                else if(is.lastConfiguration.get(i).isEnumeration()){
                    int actInd = -1;
                    for(int j = 0;j<is.lastConfiguration.get(i).getActiveValueArray().length;++j)
                        if(is.lastConfiguration.get(i).getActiveValueArray()[j].equals(is.lastConfiguration.get(i).getValue()))
                            actInd=j;
                    if(actInd+(int)optimizerParams.get(i).getValue()<is.lastConfiguration.get(i).getActiveValueArray().length)
                        this.config.getScriptParametersReference().get(i).setInitValue(is.lastConfiguration.get(i).getActiveValueArray()[actInd+(int)optimizerParams.get(i).getValue()]);
                    else
                        this.config.getScriptParametersReference().get(i).setInitValue(is.lastConfiguration.get(i).getActiveValueArray()[actInd+(int)optimizerParams.get(i).getValue()-is.lastConfiguration.get(i).getActiveValueArray().length]);


                }
            }
            --i;
        }
        try {
            is.lastConfiguration = Param.cloneParamList(this.config.getScriptParametersReference());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        //if(i < 0)
        //    this.iterationCounter = this.config.getIterationCount().get();
    }
    @Override
    public List<Param> getConfig(){

       /* List<Param> dummyPList = new LinkedList<>();
       // for(Param p : optimizerParams)
       //     dummyPList.add(new Param<Double>(0.1,10.,13.,"gsparamDouble_1");)

        Param<Double> pd  = new Param<Double>(5.,10.,13.,"gsparamDouble_1");
        Param<Integer> pd1  = new Param<Integer>(6,10,13,"gsparamInt_1");

        dummyPList.add(pd);
        dummyPList.add(pd1);
        return dummyPList;*/
       return this.optimizerParams;
    }



    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, PatternSearch.InternalState.class);

        }

    }

    //nothing to save here
    @Override
    public void saveState(String internalStateBackupFileName) {

       /* if (this.config.getOptimizerStateBackupFilename() == null)
            return;
        else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, PatternSearch.InternalState.class);
            try{
                //PrintWriter writer = new PrintWriter(getAlgorithmSimpleName()+"_params.json", "UTF-8");
                PrintWriter writer = new PrintWriter(this.config.getOptimizerStateBackupFilename(), "UTF-8");
                writer.println(s);
                //writer.println("The second line");
                writer.close();
            } catch (IOException e) {
                // do something
            }

        }*/


    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //// TODO: 21/09/17 reconsider the solution for stepsize boundaries
         for(Param p : algParams){
             if(p.isValid()) {
                 if (p.getParamTypeName().equals("Enum") || p.getParamTypeName().equals("Function"))
                     this.optimizerParams.add(new Param<Integer>(1, p.getOuterRange().getValueArray().length - 1, 1, p.getName() + "_step_size"));
                 else if (p.getParamTypeName().equals("java.lang.Integer"))
                     this.optimizerParams.add(new Param<Integer>(1, (Integer) p.getOuterRange().getUpperBound() - (Integer) p.getOuterRange().getLowerBound(), 1, p.getName() + "_step_size"));
                 else  if (p.getParamTypeName().equals("java.lang.Float") )
                     this.optimizerParams.add(new Param<Float>(0.0001f, ((Number) p.getOuterRange().getUpperBound()).floatValue() - ((Number) p.getOuterRange().getLowerBound()).floatValue(), 0.0001f, p.getName() + "_step_size"));
                 else  if (p.getParamTypeName().equals("java.lang.Boolean") ) //only for display and placeholder
                     this.optimizerParams.add(new Param<Integer>(1, 1, 1, p.getName() + "_step_size"));
             }else
                     this.optimizerParams.add(new DummyParam(p.getName()+"_step_size")); //placeholder for the alorithm, these are unreacheable params, that is that no valid range for them..

         }

    }
    //no inner state
    class InternalState {
        List<Param> lastConfiguration;
                            //PETI IS NOT WORKING :P
    }
}
