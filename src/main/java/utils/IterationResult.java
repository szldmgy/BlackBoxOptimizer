package utils;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Created by peterkiss on 20/04/17.
 */
public class IterationResult {

    //values of parameters
    private List<Param> configuration;
    private ObjectiveContainer objectives;
    private long timeStamp;

    //fitness value(s) atb given parameters
    //private ObjectiveContainer fitness;


    public long getTimeStamp() {
        return timeStamp;
    }

    //creating a new IterationResult object by cloning Params and the ObjectiveContainer
    public IterationResult(List<Param> scriptParameters, ObjectiveContainer objectiveContainer,long startime, long delta) throws CloneNotSupportedException {
        this.setConfiguration(scriptParameters);
        this.setObjectives(objectiveContainer);
        this.timeStamp = System.currentTimeMillis()-startime+delta;
    }

    public List<Param> getConfiguration() throws CloneNotSupportedException {
        List<Param> list =  new LinkedList<Param>();
        for(Param p : configuration)
            list.add((Param) p.clone());
        return list;

    }

    public void setConfiguration(List<Param> configuration) throws CloneNotSupportedException {
        List<Param> list =  new LinkedList<Param>();
        for(Param p : configuration)
            list.add((Param) p.clone());

        this.configuration = list;
    }

    public boolean badConfig() throws CloneNotSupportedException {
        for(Objective o : this.objectives.getObjectives()) {

            if ((o.getRelation().equals(Utils.Relation.LESS_THEN) || (o.getRelation().equals(Utils.Relation.MINIMIZE))) && ((Number) o.getValue()).floatValue() == Float.MAX_VALUE)
                return true;
            else if ((o.getRelation().equals(Utils.Relation.GREATER_THEN) || (o.getRelation().equals(Utils.Relation.MAXIMIZE))) && ((Number) o.getValue()).floatValue() == Float.MIN_VALUE)
                return true;
        }
        return false;

    }

    public ObjectiveContainer getObjectives() throws CloneNotSupportedException {
        return (ObjectiveContainer)objectives.clone();
    }

    public void setObjectives(ObjectiveContainer objectives) throws CloneNotSupportedException {
        this.objectives = (ObjectiveContainer) objectives.clone();
    }

    public double getFitness() throws CloneNotSupportedException {
        double d = 0;
        for(Objective o : objectives.getObjectives()) {
            if(o.getValue() instanceof Number) {
                double val = ((Number)o.getValue()).doubleValue();
                double tar = ((Number)o.getTarget()).doubleValue();
                double w = (double) o.getWeight();

                switch (o.getRelation()) {
                   /* case EQUALS:
                        d += w * Math.abs(tar - val);*/
                    case MAXIMIZE:
                        d -= w * val;
                    case MINIMIZE:
                        d += w * val;
                    case LESS_THEN:
                        d += val < tar ? 0 :w * (val - tar);
                    case GREATER_THEN:
                        d += val > tar ? 0 : w * (tar - val);
                }
            }
        }
        return d;
    }

    public boolean betterThan(IterationResult other) throws CloneNotSupportedException {
        return getFitness() < other.getFitness();
    }

    public String getCSVString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner(",");
        sj.add(String.valueOf(this.timeStamp));
        for(Param p:configuration)
            sj.add(p.getValue().toString());
        for(Objective o : objectives.getObjectives())
            sj.add(o.getValue()!=null?o.getValue().toString():"0");
        return  sj.toString();
    }


    public String getCSVHeaderString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner(",");
        sj.add("timestamp");
        for(Param p:configuration)
            sj.add(p.getName());
        for(Objective o : objectives.getObjectives())
            sj.add(o.getName());
        return  sj.toString();
    }


    @Override
    public String toString() {
        return "IterationResult{" +
                "configuration=" + configuration +
                ", fitness=" + objectives +
                '}';
    }
}
