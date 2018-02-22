package optimizer.objective;


import optimizer.main.Main;
import org.apache.log4j.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by peterkiss on 01/04/17.
 */
public class ObjectiveContainer implements Cloneable {
    public ObjectiveContainer() {
        this.objectives = new LinkedList<>();
    }

    @Override
    public String toString() {
        return "ObjectiveContainer{" +
                "objectives=" + objectives +
                '}';
    }

    public ObjectiveContainer(List<Objective> objectives) {

        this.objectives = objectives;
    }

    public List<Objective> getObjectiveClones() throws CloneNotSupportedException {
        List<Objective> copy = new LinkedList<>();
        for(Objective o : this.objectives)
            copy.add((Objective) o.clone());
        return copy;
    }
    public List<Objective> getObjectiveListReference() {
        return this.objectives;
    }

    List<Objective> objectives;

    public boolean terminated(){
        if(objectives.size() == 0)
            return false;
        boolean result = true;
        for(Objective e : objectives) {
            result &= e.met();
            if (e.terminator)
                return true;
        }
        return result;
    }

    public static ObjectiveContainer setBadObjectiveValue( ObjectiveContainer lastObjectiveContainer){
        lastObjectiveContainer.objectives.stream().forEach(
                objective -> {
                    if(objective.getTarget() instanceof Float) {
                        if(objective.getRelation().equals(Relation.GREATER_THAN)||objective.getRelation().equals(Relation.MAXIMIZE)||objective.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE))
                            objective.setValue(Float.MIN_VALUE);
                        else if(objective.getRelation().equals(Relation.LESS_THAN)||objective.getRelation().equals(Relation.MINIMIZE)||objective.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE))
                            objective.setValue(Float.MAX_VALUE);
                        else //// TODO: 16/09/17  what to put here in case of equals or others
                            objective.setValue(Float.MAX_VALUE);
                    }

                    if(objective.getTarget() instanceof Integer) {
                        if(objective.getRelation().equals(Relation.GREATER_THAN)||objective.getRelation().equals(Relation.MAXIMIZE)||objective.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE))
                            objective.setValue(Integer.MIN_VALUE);
                        else if(objective.getRelation().equals(Relation.LESS_THAN)||objective.getRelation().equals(Relation.MINIMIZE)||objective.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE))
                            objective.setValue(Integer.MAX_VALUE);
                        else //// TODO: 16/09/17  what to put here in case of equals or others
                            objective.setValue(Float.POSITIVE_INFINITY);
                    }

                }
        );
        return lastObjectiveContainer;

    }

    /**
     * Static method to read output of the algorithm to be optimized.
      * @param resultBufferedReader
     * @param lastObjectiveContainer
     * @return
     * @throws IOException
     */
    public static  ObjectiveContainer
    readObjectives(BufferedReader resultBufferedReader, BufferedReader errorReader,ObjectiveContainer lastObjectiveContainer) throws IOException {


        List<String> objNameList = lastObjectiveContainer.objectives.stream().map(o->o.getName()).collect(Collectors.toList());
        Map<String,String> map = new HashMap<>();
        for (String sn : objNameList) map.put(sn,null);
        String objLine = "";
        while ((objLine = resultBufferedReader.readLine())!=null) {
            Main.log(Level.INFO,"Readed from sout of runned alg: "+ objLine);
            //String objLine = sc.nextLine();
            if(objLine.trim().split(" ").length==2) {
                Scanner s1 = new Scanner(objLine);
                String objectiveName = s1.next().trim();
                String objectiveValue = s1.next().trim();
                if(map.containsKey(objectiveName)) {
                    map.put(objectiveName, objectiveValue);
                    Main.log(Level.INFO,"Objective recognized: "+ objLine);
                }
            }
        }
        if(errorReader!=null) {
            while ((objLine = errorReader.readLine()) != null) {
                Main.log(Level.INFO, "Readed from stderr of runned alg: " + objLine);
            }
        }
        for(Map.Entry<String,String> element : map.entrySet()) {
            String objectiveValue = element.getValue();
            String objectiveName = element.getKey();
            // TODO: 15/06/17  if objective is not specified so basically we aren't interested in obj file-entry handle elsewhere
            Class<?> objType = null;
            Optional<Objective> o = lastObjectiveContainer.objectives.stream().filter(e -> objectiveName.equals(e.getName())).findFirst();
            if (o.isPresent())
                objType = o.get().getType();
            else
                continue;
            if(objectiveValue == null) {
                Main.log(Level.INFO, "Missing objective: " + objectiveName);
                objectiveValue = "0";
            }
            Double readedval = Double.parseDouble(objectiveValue);
            if (objType.equals(Integer.class)) {

                ((Objective<Integer>) o.get()).setValue(readedval.intValue());
            }
            else if (objType.equals(Float.class) ) {
                ((Objective<Float>) o.get()).setValue(readedval.floatValue());
            }
            //not supported anymore
//            else if (objType.equals(Boolean.class)) {
//                boolean val =objectiveValue!=null? Boolean.parseBoolean(objectiveValue):false;
//                ((Objective<Boolean>) o.get()).setValue(val);
//            }
            else
                Main.log(Level.ERROR,"Type error!!!!!!");
        }
        return lastObjectiveContainer;
    }
    @Override
    public Object clone() throws CloneNotSupportedException {
        ObjectiveContainer cloned = (ObjectiveContainer)super.clone();
        cloned.objectives = new LinkedList<>();
        for(Objective entry :this.objectives)
            cloned.objectives.add( (Objective)(entry.clone()));
        return cloned;

    }


}
