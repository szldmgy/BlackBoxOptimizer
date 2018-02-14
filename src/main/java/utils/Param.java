package utils;






import javax.script.ScriptException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by peterkiss on 15/10/16.
 */
public class Param<T> implements Cloneable, Comparable<Param>{

    private String name;
    private String typeName;
    private T initValue;


    public List<ParameterDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ParameterDependency> dependencies) {
        this.dependencies = dependencies;
    }

    List<ParameterDependency> dependencies;

    public T getValue() {
        return initValue;
    }
//valuetypename -
    public String getName() {
        return name.replace("$","");
    }
    public String getValueTypeName(){return this.typeName;}
    public Class<?> getParamGenericType(){
        return this.initValue.getClass();
    }
    public String getParamGenericTypeName(){
        return this.initValue.getClass().getName();
    }

    /**
     * override this for introduce new paramtype for Web UI
     * @return type of param: enum, function or in general case the generic type of Param
     */
    public String getParamTypeName(){
        //return typeName;
        // TODO: 18/10/17 hack -> should be solved at unmarshalling!!
        if(this.isEnumeration())
            return "Enum";
        return getParamGenericTypeName();
    }
    public String getAdditionalInfo(){return "";};

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Param)) return false;

        Param<?> param = (Param<?>) o;
        if(param.getParamGenericType()!=this.getParamGenericType())
            return false;

        return name != null ? name.equals(param.name) : param.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public void setInitValue(T initValue) {
        this.initValue = initValue;
    }

    //// TODO: 22/09/17 hack 
    public T getUpperBound() {
        return this.getActiveRange()!=null?(T)this.getActiveRange().getUpperBound():(T)this.getOuterRange().getUpperBound();
        //return (T)this.getActiveRange().upperBound;
    }

    // TODO: 22/09/17 hack 
    public T getLowerBound() {
        return this.getActiveRange()!=null?(T)this.getActiveRange().getLowerBound():(T)this.getOuterRange().getLowerBound();
        //return (T)this.getActiveRange().lowerBound;
    }





    public Param(T value ,T upper, T lower,String name) {
        this.name = name;
        this.dependencies = new LinkedList<>();
        this.dependencies.add(new ParameterDependency(lower,upper));
        this.initValue = value;
        this.typeName = getParamGenericTypeName();

    }

    //we added the dependency param before and now we add its range
    //needed?
    public <T2>void addDependencyToNodBoundedRange(Param<T2> p,T2 lower,T2 upper ){
        ParameterDependency freePd = null;
        for(ParameterDependency dep : this.dependencies ){
            if(dep.rangeOfOther == null)
                freePd = dep;
        }
        if(p.isEnumeration())
            freePd.rangeOfOther = new Range(p.getAllValueArray(), upper,lower);
        else

            freePd.rangeOfOther = new Range(upper,lower);
        freePd.setP(p);

    }
    //todo hack
    //if inactive at a given start value it will be null!!
   // public boolean isEnumeration(){return this.getActiveRange().valueArray != null;}
    //actually can be good
    public boolean isEnumeration(){return this.dependencies.get(0).rangeOfThis.getValueArray()!= null;}

    // TODO: 04/05/17 we should have some factory to check things...
    // TODO: 04/05/17 maybe polimorphism for enum
    public Param(T value ,T[] values,String name) {
        this.name = name;
        this.dependencies = new LinkedList<>();
        this.dependencies.add(new ParameterDependency<T,T>(values));//fisrt T could be Object, there is no boundary on it
        this.initValue = value;
        this.typeName = getParamGenericTypeName();

    }

    public  T[] getActiveValueArray(){
        return (T[])this.getActiveRange().getValueArray();
    } 
    
    //// TODO: 22/09/17 ouch... 0 indexed one should contain  all value -> see the constructor
    public  T[] getAllValueArray(){
        return (T[])this.dependencies.get(0).rangeOfThis.getValueArray();
    }
    public  String getAllValueString(){
        T[] a = getAllValueArray();
        if(a == null)
            return "";
        StringJoiner sj = new StringJoiner(";");
        for(T e:a)
            sj.add(e.toString());
        return sj.toString();
    }

    // TODO: 18/10/17 most probably this is incorrect 
    public  String getAllPossibleValueArrayString(){
        Set<T> ret = new HashSet<T>();
        for(ParameterDependency pd : this.dependencies)
        {
            ret.addAll(
                    pd.getRangeOfOther()==null?new ArrayList<T>():
                    Arrays.asList(pd.getRangeOfOther().getValueArray()==null?null:(T[])(pd.getRangeOfOther().getValueArray()) )
            );
        }
        StringJoiner sj = new StringJoiner(";");
        for(T e: ret)
            sj.add(e.toString());
        return sj.toString();
    }

    

    public <T2 >void addDependency(T lower,T upper ,Param<T2> p, T2 dependencyLower, T2 dependencyUpper){
        this.dependencies.add(new ParameterDependency(lower,upper,p,dependencyLower,dependencyLower));


    }

    public <T2 >void addDependency(T[] range){
        this.dependencies.add(new ParameterDependency(range));


    }

    @Deprecated
    public <T2 >void addDependency(T lower,T upper ) throws ScriptException {
        if(lower instanceof String)
            addDependency1((String) lower,(String) upper,"","");
        else
            this.dependencies.add(new ParameterDependency(lower,upper));


    }
    public <T2 >void addDependency1(String lower,String upper,String funcString ,String funcRunningCountStr) throws ScriptException {
        Object lowerO=null, upperO = null;
        String typeName = this.getParamGenericTypeName();
        ParameterDependency pd = null;
        if(typeName.equals(Integer.class.getName())){
            lowerO = Integer.parseInt(lower);
            upperO = Integer.parseInt(upper);
            pd = new ParameterDependency(lowerO,upperO);
        }
        else if(funcRunningCountStr!=null && !funcRunningCountStr.equals("")){ // if there is something it must be func
            pd = new ParameterDependency<>(Utils.evalFunction(funcString,Integer.parseInt(funcRunningCountStr)));
        }
        else if(typeName.equals(Float.class.getName())){ //crash here
            lowerO = Float.parseFloat(lower);
            upperO = Float.parseFloat(upper);
            pd = new ParameterDependency(lowerO,upperO);
        }
        else if(typeName.equals(Boolean.class.getName())){
            lowerO = Boolean.parseBoolean(lower);
            upperO = Boolean.parseBoolean(upper);
            pd = new ParameterDependency(lowerO,upperO);
        }

        this.dependencies.add(pd);


    }

    // TODO: 14/04/17 is that correct ????
    public boolean isActive(){
       return getActiveRange() != null;
    }

    public boolean isInRange() throws Exception {
//// TODO: 2018. 01. 24. was only check for upper border
        if(this.getValue() instanceof Number)
            return /*this.getActiveRange()!=null&&*/((Number)(this.getActiveRange().getUpperBound())).floatValue()>= ((Number)this.getValue()).floatValue() && ((Number)(this.getActiveRange().getLowerBound())).floatValue()<= ((Number)this.getValue()).floatValue();
        else if (this.isEnumeration())
            return /*this.getActiveValueArray()!=null&&*/Arrays.asList(getActiveValueArray()).contains(this.getValue());
        else if (this.getValue() instanceof Boolean)
            return ((this.getActiveRange().getUpperBound())).equals( this.getValue()) || ((this.getActiveRange().getLowerBound())).equals( this.getValue());
        throw new Exception("not implemented!!");

    }

/**
 * returns null if dependency conditions not met*/
    public <T,T2> Range<T>  getActiveRange(){
        LinkedList<Range<T>> l = new LinkedList<Range<T>>();
        l.add(null);
        dependencies.stream().filter(d -> d.comply()).forEach(new Consumer<ParameterDependency>() {
            @Override
            public void accept(ParameterDependency parameterDependency) {
                l.set(0, Range.intersection(l.get(0), (Range<T>)parameterDependency.rangeOfThis));
            }
        });
        return l.get(0);
    }
    //// TODO: 21/09/17 should use availible ranges graph
    public <T> List<Range<T>>  getAllRanges(){
       return this.dependencies.stream().map(d->(Range<T>)d.getRangeOfThis()).collect(Collectors.toList());
    }
    //something like parameter is reacheable at all
    public <T1,T2> boolean isValid(){
        for(ParameterDependency<T2,T1> pb :this.dependencies) {
            if( pb.p == null)
                return true; //  not bounded/default range exists
            for (ParameterDependency<?,T2> pb1 : pb.p.dependencies) {
                if (Range.intersection(pb.getRangeOfOther(), pb1.getRangeOfThis()) != null)
                    return true;
            }
        }
        return false;

    }



    public Range<T> getOuterRange(){

        //// TODO: 21/09/17 check this!!
        T[] lower =(T[]) new Object[]{Float.MAX_VALUE};
        T[] upper = (T[]) new Object[]{Float.MIN_VALUE};

        if(!this.isEnumeration()) {
            this.dependencies.stream().forEach(d ->
            {
                if ((d.getRangeOfThis().getLowerBound() instanceof Number) && 0 > Utils.compareNumbers((Number) d.getRangeOfThis().getLowerBound(), (Number) lower[0])) {
                    lower[0] = (T) d.getRangeOfThis().getLowerBound();
                }
                if (d.getRangeOfThis().getUpperBound() instanceof Number && 0 < Utils.compareNumbers((Number) d.getRangeOfThis().getUpperBound(), (Number) upper[0])) {
                    upper[0] = (T) d.getRangeOfThis().getUpperBound();
                }
            });
            return new Range<T>(upper[0],lower[0]);
        }
        else{
            List<T> res = new LinkedList<T>();
            this.dependencies.stream().forEach(d ->
            {
                res.addAll(Arrays.asList((T[]) d.getRangeOfThis().getValueArray()));
            });
            return new Range<T>((T[]) res.toArray());
        }
    }

    public boolean isNumeric() {
        if(this instanceof FunctionParam)
            return false;
        return initValue instanceof Integer || initValue instanceof Float || initValue instanceof Double;
    }

    public boolean isBoolean() {
        return initValue instanceof Boolean;
    }

    public void add(T b) {

        if(b instanceof Number) {
            Number n = (Number)b;
            if (initValue instanceof Integer) {
                Integer i = (Integer) initValue + n.intValue();
                setInitValue((T) i);
            }
            if (initValue instanceof Double) {
                Double i = (Double) initValue+n.doubleValue();
                //Double j = (Double) b;
                setInitValue((T)i);
            }
            if (initValue instanceof Float) {
                Float i = (Float) initValue + n.floatValue();
                setInitValue((T) i);
            }
        }
    }

    @Override
    public int compareTo(Param o) {
        Param<?> theOther = (Param)o;
        if(this.dependencies.stream().anyMatch( d-> d.p == null ? false : d.p.equals(theOther)))
            return -1;
        if(theOther.dependencies.stream().anyMatch( d-> d.p == null ? false : d.p.equals(this)))
            return 1;
        return 0;
    }

    //// TODO: 21/05/17 return type could be used 
    public boolean updateDependencies(Param param) {
        List<ParameterDependency> toRemoveList = new LinkedList<>();
        List<ParameterDependency> toAddList = new LinkedList<>();

        for(ParameterDependency d : this.getDependencies()){
            if(d.p == null) continue;
            if(d.p.getName().equals(param.getName())) {
                Class<?> cl =param.getParamGenericType();
                Object dependencyLower = d.rangeOfOther.getLowerBound();
                Object dependencyUpper = d.rangeOfOther.getUpperBound();



                    if(Integer.class.getName().equals(cl.getName())){
                        dependencyLower = Integer.parseInt(dependencyLower.toString());
                        dependencyUpper= Integer.parseInt(dependencyUpper.toString());
                    }
                    else if(Float.class.getName().equals(cl.getName())){
                        dependencyLower = Float.parseFloat(dependencyLower.toString());
                        dependencyUpper= Float.parseFloat(dependencyUpper.toString());
                    }
                    else if(Boolean.class.getName().equals(cl.getName())){
                        dependencyLower = Boolean.parseBoolean(dependencyLower.toString());
                        dependencyUpper= Boolean.parseBoolean(dependencyUpper.toString());
                    }
                    else if(Double.class.getName().equals(cl.getName())){
                        dependencyLower = Double.parseDouble(dependencyLower.toString());
                        dependencyUpper= Double.parseDouble(dependencyUpper.toString());
                    }
                    //if String-based no need for conversion
                    /*else {//String????
                        throw new RuntimeException("Not Implemented");
                    }*/
                //todo concurrentmodification!!!!!
                //this.dependencies.remove(d); 
                //this.dependencies.add(new ParameterDependency(d.getRangeOfThis().lowerBound, d.getRangeOfThis().upperBound, param,dependencyLower,dependencyUpper));
                toRemoveList.add(d);
                // TODO: 2018. 01. 28. quickfix for that 
                toAddList.add(new ParameterDependency(d.getRangeOfThis().getLowerBound(), d.getRangeOfThis().getUpperBound(), param,dependencyLower,dependencyUpper));

                
            }
        }
        for(ParameterDependency pd : toRemoveList)
            this.dependencies.remove(pd);
        for(ParameterDependency pd : toAddList)
            this.dependencies.add(pd);
        return true;
    }

    public static void refillList(List<Param> orig, List<Param> newList) throws CloneNotSupportedException {
        orig.clear();
        for(Param p : newList)
            orig.add((Param) p.clone());
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        Param p =   (Param)super.clone();
        p.dependencies = new LinkedList<>();
        for(ParameterDependency pd : this.dependencies)
            p.dependencies.add(pd.clone());
        return p;

    }

    @Override
    public String toString() {
        return "Param{" +
                "dependencies=" + dependencies +
                ", name='" + name + '\'' +
                ", initValue=" + initValue +
                '}';
    }
}


