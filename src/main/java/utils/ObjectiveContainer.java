package utils;


import main.Main;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by peterkiss on 01/04/17.
 */
public class ObjectiveContainer implements Cloneable {
    // TODO: 02/04/17 name of objective redundant 
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

    public static  class Objective<T> implements Cloneable{
        Utils.Relation relation;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Objective)) return false;

            Objective<?> objective = (Objective<?>) o;

            return name.equals(objective.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override

        public String toString() {
            return "Objective{" +
                    "relation=" + relation +
                    ", terminator=" + terminator +
                    ", weight=" + weight +
                    ", name='" + name + '\'' +
                    ", dummy=" + dummy +
                    ", value=" + value +
                    ", target=" + target +
                    '}';
        }

        boolean terminator;



        public String getRelationStr() {
            return relation.toString();
        }

        public Utils.Relation getRelation() {return relation;}

        public boolean isTerminator() {
            return terminator;
        }

        public String getName() {
            return name;
        }

        public T getValue() {
            return value;
        }

        public T getTarget() {
            return target;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        private float weight;
        private String name;
        private T dummy;
        private T value;
        private T target;
        private T lastvalue;
        public void setValue(T value){
            this.value = value;
        }
        public boolean met(){
            return  Utils.comply(relation,value,target);
        }
        //// TODO: 28/08/17 same as getTypeName? 
        public Class<?> getType(){
            return dummy.getClass(); //before first load value
        }
        public String getTypeName(){ return target!=null?target.getClass().getName():dummy.getClass().getTypeName();}

        public Objective(Utils.Relation relation, boolean terminator, String name, T value, T target, T dummynull, float weight) {
            this.relation = relation;
            this.terminator = terminator;
            this.dummy = dummynull;
            this.name = name;
            this.value = value;
            this.target = target;
            this.weight = weight;
        }

        public Objective() {
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return (Objective)super.clone();
        }
    }

    public List<Objective> getObjectives() throws CloneNotSupportedException {
        List<Objective> copy = new LinkedList<>();
        for(Objective o : this.objectives)
            copy.add((Objective) o.clone());
        return copy;
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
//    public static ObjectiveContainer initObjectives(){
//
//    }

    public static ObjectiveContainer setBadObjectiveValue( ObjectiveContainer lastObjectiveContainer){
        lastObjectiveContainer.objectives.stream().forEach(
                objective -> {
                if(objective.getTarget() instanceof Float) {
                    if(objective.getRelation().equals(Utils.Relation.GREATER_THEN)||objective.getRelation().equals(Utils.Relation.MAXIMIZE)||objective.getRelation().equals(Utils.Relation.MAXIMIZE_TO_CONVERGENCE))
                        objective.setValue(Float.MIN_VALUE);
                    else if(objective.getRelation().equals(Utils.Relation.LESS_THEN)||objective.getRelation().equals(Utils.Relation.MINIMIZE)||objective.getRelation().equals(Utils.Relation.MINIMIZE_TO_CONVERGENCE))
                        objective.setValue(Float.MAX_VALUE);
                    else //// TODO: 16/09/17  what to put here in case of equals or others
                        objective.setValue(Float.MAX_VALUE);
                }
                    if(objective.getTarget() instanceof Double) {
                        if(objective.getRelation().equals(Utils.Relation.GREATER_THEN)||objective.getRelation().equals(Utils.Relation.MAXIMIZE)||objective.getRelation().equals(Utils.Relation.MAXIMIZE_TO_CONVERGENCE))
                            objective.setValue(Float.MIN_VALUE);
                        else if(objective.getRelation().equals(Utils.Relation.LESS_THEN)||objective.getRelation().equals(Utils.Relation.MINIMIZE)||objective.getRelation().equals(Utils.Relation.MINIMIZE_TO_CONVERGENCE))
                            objective.setValue(Float.MAX_VALUE);
                        else //// TODO: 16/09/17  what to put here in case of equals or others
                            objective.setValue(Float.MAX_VALUE);
                    }
                    if(objective.getTarget() instanceof Integer) {
                        if(objective.getRelation().equals(Utils.Relation.GREATER_THEN)||objective.getRelation().equals(Utils.Relation.MAXIMIZE)||objective.getRelation().equals(Utils.Relation.MAXIMIZE_TO_CONVERGENCE))
                            objective.setValue(Integer.MIN_VALUE);
                        else if(objective.getRelation().equals(Utils.Relation.LESS_THEN)||objective.getRelation().equals(Utils.Relation.MINIMIZE)||objective.getRelation().equals(Utils.Relation.MINIMIZE_TO_CONVERGENCE))
                            objective.setValue(Integer.MAX_VALUE);
                        else //// TODO: 16/09/17  what to put here in case of equals or others
                            objective.setValue(Float.POSITIVE_INFINITY);
                    }

        }
        );
        return lastObjectiveContainer;

    }
    // TODO: 02/04/17 Typecheck, hacking due to missing MAIN - no obj name handling not really efficient collect names all the time
    public static ObjectiveContainer
    readObjectives(BufferedReader resultBufferedReader, ObjectiveContainer lastObjectiveContainer) throws IOException {


        //File f = new File(filename);
        //if(f.exists()) {
            //Scanner sc = new Scanner(f);
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
            for(Map.Entry<String,String> element : map.entrySet()) {
                String objectiveValue = element.getValue();
                String objectiveName = element.getKey();
                // TODO: 15/06/17  if objective is not specified so basically we arent interested in obj file-entry handle elsewhere
                Class<?> objType = null;
                Optional<Objective> o = lastObjectiveContainer.objectives.stream().filter(e -> objectiveName.equals(e.name)).findFirst();
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

                    /*int val = 0;
                    try {
                        val = Integer.parseInt(objectiveValue);
                    }catch (NumberFormatException e){
                        val = Integer.MAX_VALUE;
                    }*/
                    ((Objective<Integer>) o.get()).setValue(readedval.intValue());
                }
                // TODO: 06/06/17 type hack
                else if (objType.equals(Float.class) || objType.equals(Double.class)) {
                   // Float val = objectiveValue!=null?Float.parseFloat(objectiveValue):0f;
                    ((Objective<Float>) o.get()).setValue(readedval.floatValue());
                } else if (objType.equals(Boolean.class)) { //not supported anymore
                    boolean val =objectiveValue!=null? Boolean.parseBoolean(objectiveValue):false;
                    ((Objective<Boolean>) o.get()).setValue(val);
                } else
                    Main.log(Level.ERROR,"Type error!!!!!!");
            }
        //}
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
