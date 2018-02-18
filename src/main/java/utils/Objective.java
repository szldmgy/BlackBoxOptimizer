package utils;

public class Objective<T> implements Cloneable{
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
    private String typeName;
    public void setValue(T value){
        this.value = value;
    }
    public boolean met(){
        return  Utils.comply(relation,value,target);
    }

    public Class<?> getType(){
        return dummy.getClass(); //before first load value
    }
    public String getTypeName(){ return this.typeName;}

    public Objective(Utils.Relation relation, boolean terminator, String name, T value, T target, T dummynull, float weight) {
        this.relation = relation;
        this.terminator = terminator;
        this.dummy = dummynull;
        this.name = name;
        this.value = value;
        this.target = target;
        this.weight = weight;
        this.typeName = dummy.getClass().getCanonicalName();

    }

    public Objective() {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (Objective)super.clone();
    }
}
