package optimizer.trial;

import optimizer.objective.Relation;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.param.Param;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

/**
 * This class represents the important data regarding to a single run of the black box function:
 * the parameter setup and the corresponding objective values stored in a {@link java.util.List< Param >} and an {@link ObjectiveContainer}
 * Created by peterkiss on 20/04/17.
 */
public class IterationResult implements Comparable<IterationResult>{

    /**
     * Recent parametrization of the BBF .
     */
    private List<Param> configuration;
    /**
     * Results of the run given the {@code configuration}.
     */
    private ObjectiveContainer objectives;
    /**
     * Start time of the optimization task.
     */
    private long timeStamp;

    /**
     * Getter for {@link #timeStamp}.
     * @return
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Constructor:  creating a new IterationResult object by cloning Params and the ObjectiveContainer
     * @param scriptParameters
     * @param objectiveContainer
     * @param startime
     * @param delta
     * @throws CloneNotSupportedException
     */
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
        for(Objective o : this.objectives.getObjectiveClones()) {
            try {
                if ((o.getRelation().equals(Relation.LESS_THAN) || (o.getRelation().equals(Relation.MINIMIZE))) && ((Number) o.getValue()).floatValue() == Float.MAX_VALUE)
                    return true;
                else if ((o.getRelation().equals(Relation.GREATER_THAN) || (o.getRelation().equals(Relation.MAXIMIZE))) && ((Number) o.getValue()).floatValue() == Float.MIN_VALUE)
                    return true;
            }catch (Exception e){
                // TODO: 2018. 02. 24.
                System.out.println("Bad objective value.");
            }
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
        for(Objective o : objectives.getObjectiveClones()) {
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
                    case LESS_THAN:
                        d += val < tar ? 0 :w * (val - tar);
                    case GREATER_THAN:
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
        for(Objective o : objectives.getObjectiveClones())
            sj.add(o.getValue()!=null?o.getValue().toString():"0");
        return  sj.toString();
    }


    public String getCSVHeaderString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner(",");
        sj.add("timestamp");
        for(Param p:configuration)
            sj.add(p.getName());
        for(Objective o : objectives.getObjectiveClones())
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

    @Override
    public int compareTo(IterationResult o) {
        return (int)(this.timeStamp-o.timeStamp);
    }
}
