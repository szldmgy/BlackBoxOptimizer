package algorithms;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import utils.DummyParam;
import utils.IterationResult;
import utils.Param;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

// optimizerParams[i] : stepsize for the i-th parameter

public class GridSearch extends  AlgorithmFI{


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
        List<Param> lastConfiguration = null;
        try {
            if(landscape.isEmpty())
                lastConfiguration = parameterMap;
            else
                lastConfiguration = landscape.get(landscape.size()-1).getConfiguration();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        int i = lastConfiguration.size()-1;
        while(i >= 0) {
            if(this.config.getScriptParametersReference().get(i).isActive()) {
                if(this.config.getScriptParametersReference().get(i).isNumeric())
                {
                        if(((Number)lastConfiguration.get(i).getValue()).doubleValue() + ((Number)optimizerParams.get(i).getValue()).doubleValue() <=
                            ((Number)lastConfiguration.get(i).getUpperBound()).doubleValue()) {
                        this.config.getScriptParametersReference().get(i).add(optimizerParams.get(i).getValue());
                        break;
                    }
                    else
                        this.config.getScriptParametersReference().get(i).setInitValue(lastConfiguration.get(i).getLowerBound());
                }
                else if(lastConfiguration.get(i).isBoolean()) {
                    if ((Boolean) lastConfiguration.get(i).getValue()) {
                        this.config.getScriptParametersReference().get(i).setInitValue(false);
                    } else {
                        this.config.getScriptParametersReference().get(i).setInitValue(true);
                        break;
                    }
                }
                else if(lastConfiguration.get(i).isEnumeration()){
                    int actInd = -1;
                    for(int j = 0;j<lastConfiguration.get(i).getActiveValueArray().length;++j)
                        if(lastConfiguration.get(i).getActiveValueArray()[j].equals(lastConfiguration.get(i).getValue()))
                            actInd=j;
                    if(actInd+(int)optimizerParams.get(i).getValue()<lastConfiguration.get(i).getActiveValueArray().length)
                        this.config.getScriptParametersReference().get(i).setInitValue(lastConfiguration.get(i).getActiveValueArray()[actInd+(int)optimizerParams.get(i).getValue()]);
                    else
                        this.config.getScriptParametersReference().get(i).setInitValue(lastConfiguration.get(i).getActiveValueArray()[actInd+(int)optimizerParams.get(i).getValue()-lastConfiguration.get(i).getActiveValueArray().length]);


                }
            }
            --i;
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
                 if (p.getParamTypeName().equals("java.lang.Float") )
                     //this.optimizerParams.add(new Param<Float>(0.0001f,((Number)p.getUpperBound()).floatValue()-((Number)p.getLowerBound()).floatValue(),0.0001f,p.getName()+"_step_size"));
                     this.optimizerParams.add(new Param<Float>(0.0001f, ((Number) p.getOuterRange().getUpperBound()).floatValue() - ((Number) p.getOuterRange().getLowerBound()).floatValue(), 0.0001f, p.getName() + "_step_size"));
                 else if (p.getParamTypeName().equals("java.lang.Integer"))
                     this.optimizerParams.add(new Param<Integer>(1, (Integer) p.getOuterRange().getUpperBound() - (Integer) p.getOuterRange().getLowerBound(), 1, p.getName() + "_step_size"));
                 //this.optimizerParams.add(new Param<Integer>(1, p.ge, 1, p.getName() + "_step_size"));
                 else if (p.getParamTypeName().equals("Enum") || p.getParamTypeName().equals("Function"))
                     this.optimizerParams.add(new Param<Integer>(1, p.getOuterRange().getValueArray().length - 1, 1, p.getName() + "_step_size"));
             }else
                 this.optimizerParams.add(new DummyParam(p.getName()+"_step_size")); //placeholder for the alorithm

         }

    }
    //no inner state
    class InternalState {
                            //PETI IS NOT WORKING :P
    }
}
