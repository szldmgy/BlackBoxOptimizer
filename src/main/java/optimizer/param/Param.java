package optimizer.param;






import optimizer.utils.Utils;

import javax.script.ScriptException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents parameter class that ids used all over the BlaBoO, in the configuration of the BBF, as well as at the that of the optimizer optimizer.algorithms.
 * Generic type represents the basic java type wrapped in the Param object.
 * Created by peterkiss on 15/10/16.
 */
public class Param<T> implements Cloneable, Comparable<Param>{
    public static final Class<?>[] allowedClasses = {Float.class,Integer.class,String.class,Boolean.class};


    /**
     * name of the objectives, along with the T type parameter identifies the param.
     */
    private String name;
    /**
     * String representation of Type parameter T ({@link Class#getCanonicalName()}) for correct deserialization.
     */
    private String typeName;
    /**
     * Recent value of the parameter
     */
    private T initValue;
    /**
     * List of boundaries({@link ParameterDependency}) of a given Param. That specifies the upper and lower bound possibly depending on the {@code initValue} of another Parameter.
     */
    List<ParameterDependency> dependencies;



    public List<ParameterDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ParameterDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public T getValue() {
        return initValue;
    }

    /**
     * Returns the name of the parameter, if it has been instantiated with a name containing `$` indicator, that will be removed.
     * @return
     */
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
        if(this.isEnumeration())
            return "Enum";
        return getParamGenericTypeName();
    }

    /**
     * Possible future feature to give a description of the parameter rather in case of configuring {@link optimizer.algorithms.AbstractAlgorithm}
     * @return
     */
    public String getAdditionalInfo(){return "";}

    /**
     * We regard two parameters as equal, if the name and generic type are the same.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Param)) return false;

        Param<?> param = (Param<?>) o;
        if(param.getParamGenericType()!=this.getParamGenericType())
            return false;

        return name != null ? name.equals(param.name) : param.name == null;

    }

    /**
     * Returns hash of the {@code name}
     * @return
     */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public void setInitValue(T initValue) {
        this.initValue = initValue;
    }


    /**
     * Returns the recent upper bound on the param, that possibly depends on {@code initValue} of a bounding {@link Param}.
     * @return
     * todo: usage of {@code getOuterRange} is not very consistent
     */
    public T getUpperBound() {
        return this.getActiveRange()!=null?(T)this.getActiveRange().getUpperBound():(T)this.getOuterRange().getUpperBound();
    }

    /**
     * Returns the recent lower bound on the param, that possibly depends on {@code initValue} of a bounding {@link Param}.
     * @return
     * todo: usage of {@code getOuterRange} is not very consistent
     */    public T getLowerBound() {
        return this.getActiveRange()!=null?(T)this.getActiveRange().getLowerBound():(T)this.getOuterRange().getLowerBound();
        //return (T)this.getActiveRange().lowerBound;
    }


    /**
     * Constructor for creating a Param without dependence on another.
     * @param value Initial value
     * @param upper Upper bound on {@code initValue}.
     * @param lower Lower bound on {@code initValue}.
     * @param name Name of the {@link Param}.
     */
    public Param(T value ,T upper, T lower,String name) {
        this.name = name;
        this.dependencies = new LinkedList<>();
        this.dependencies.add(new ParameterDependency(lower,upper));
        this.initValue = value;
        this.typeName = getParamGenericTypeName();

    }

    /**
     * Adds a dependency to the param, we should only get here if we knew that there will be another bounding param on which this will depend.
     * @param p the other param on which the current range will depend.
     * @param lower The lower bound of the bounding param.
     * @param upper The lower bound of the bounding param.
     * @param <T2> Gneric type of the bounding param.
     */
    //we added the dependency param before and now we add its range
    public <T2>void addDependencyToNotBoundedRange(Param<T2> p, T2 lower, T2 upper ){
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
        this.dependencies.add(new ParameterDependency<T,T>(values));//first T could be Object, there is no boundary on it
        this.initValue = value;
        this.typeName = getParamGenericTypeName();

    }

    /**
     * In case of finite value set returns the active set of values.
     * @return
     */
    public  T[] getActiveValueArray(){
        return (T[])this.getActiveRange().getValueArray();
    }

    /**
     * In case of finite value set returns the set of all values.
     * @return
     */
    public  T[] getAllValueArray(){
        //0 indexed one should contain  all value -> see the constructor
        return (T[])this.dependencies.get(0).rangeOfThis.getValueArray();
    }
    @Deprecated
    public  String getAllValueString(){
        T[] a = getAllValueArray();
        if(a == null)
            return "";
        StringJoiner sj = new StringJoiner(";");
        for(T e:a)
            sj.add(e.toString());
        return sj.toString();
    }

    @Deprecated
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


    /**
     * Adds a {@link ParameterDependency} to {@code dependencies}.
     * @param lower The lower bound of the parameter if the value of the bounding param is between {@code dependencyLower} and {@code dependencyUpper}
     * @param upper The upper bound of the parameter if the value of the bounding param is between {@code dependencyLower} and {@code dependencyUpper}
     * @param p The bounding parameter.
     * @param dependencyLower The lower bound of value of {@code p}, when the {@code lower} - {@code upper } bound will be valid.
     * @param dependencyUpper The upper bound of value of {@code p}, when the {@code lower} - {@code upper } bound will be valid.
     * @param <T2> The generic type of the bounding parameter.
     */
    public <T2 >void addDependency(T lower,T upper ,Param<T2> p, T2 dependencyLower, T2 dependencyUpper){
        this.dependencies.add(new ParameterDependency(lower,upper,p,dependencyLower,dependencyLower));


    }

    /**
     * Adds a not bounded range to finite value set parameter.
     * @param range The set in which the value can move.
     * @param <T2> The generic type of the parameter, now it can be String and Float.
     */
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

    /**
     * Adds a not bounded range to parameter. This can be the range for values of an independent parameter,
     * or can be a default range for values in case non of the bounding parameters are in the range which would have an impact on
     * this one.
     * @param lower The default lower range.
     * @param upper The default upper range.
     * @param funcString The optional javascript function for generating series of values, for enabling {@link FunctionParam }
     * @param funcRunningCountStr If this is {@link FunctionParam}, it specifies the number of element to generate using the {@code funcString}.
     * @throws ScriptException
     */
    public void addDependency1(String lower,String upper,String funcString ,String funcRunningCountStr) throws ScriptException {
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
        else if(typeName.equals(Float.class.getName())){
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

    /**
     * Method to query whether the parameter is active. Being active is equivalent to having a range in which the value can move.
     * @return Whether this active range exists.
     */
    public boolean isActive(){
        return getActiveRange() != null;
    }

    /**
     * Method to query whether the recent value of the parameter is within the actual boundaries.
     * @return
     * @throws Exception
     */
    public boolean isInRange() throws Exception {
        if(this.getValue() instanceof Number)
            return /*this.getActiveRange()!=null&&*/((Number)(this.getActiveRange().getUpperBound())).floatValue()>= ((Number)this.getValue()).floatValue() && ((Number)(this.getActiveRange().getLowerBound())).floatValue()<= ((Number)this.getValue()).floatValue();
        else if (this.isEnumeration())
            return /*this.getActiveValueArray()!=null&&*/Arrays.asList(getActiveValueArray()).contains(this.getValue());
        else if (this.getValue() instanceof Boolean)
            return ((this.getActiveRange().getUpperBound())).equals( this.getValue()) || ((this.getActiveRange().getLowerBound())).equals( this.getValue());
        throw new Exception("not implemented!!");

    }

    /**
     * Method to query the range that is active due to the value of bounding parameters.
     * @param <T> The generic type of the parameter and the {@link Range} object that will be returned.
     * @param <T2> Second generic type of the {@link Range} object, not used here.
     * @return
     */
    public <T,T2> Range<T>  getActiveRange(){
        //list only for sake of lambda
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
    // TODO: 21/09/17 should use availible ranges graph
    @Deprecated
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


    /**
     * Method to query the absolute boundaries of the param in a {@link Range} object. That is that the values of Param should be any time within this range.
     * @return
     */
    public Range<T> getOuterRange(){
        if(!this.isEnumeration()) {
            T[] lower =(T[]) new Object[]{Float.MAX_VALUE};
            T[] upper = (T[]) new Object[]{Float.MIN_VALUE};
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

    /**
     * Helper method to decide whether the Param wraps a numeric value.
     * @return
     */
    public boolean isNumeric() {
        if(this instanceof FunctionParam)
            return false;
        return initValue instanceof Integer || initValue instanceof Float || initValue instanceof Double;
    }

    /**
     * Helper method to decide whether the Param wraps a boolean value.
     * @return
     */
    public boolean isBoolean() {
        return initValue instanceof Boolean;
    }

    /**
     * Method to increase or decrease the value of numeric wrapper Params(see {@code isNumeric})
     */
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

    /**
     * Helper method for building up Parameter structure from the GUI. If the bounding Parameter is not initialized yet, we
     * ass a dummy one as bounding param. Then every time we create a
     * new Parameter look through the params and substitute the dummy with the actual one if their name agree.
     * @param param The newly created Param we want check.
     */
    public void updateDependencies(Param param) {
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

                toRemoveList.add(d);
                toAddList.add(new ParameterDependency(d.getRangeOfThis().getLowerBound(), d.getRangeOfThis().getUpperBound(), param,dependencyLower,dependencyUpper));


            }
        }
        for(ParameterDependency pd : toRemoveList)
            this.dependencies.remove(pd);
        for(ParameterDependency pd : toAddList)
            this.dependencies.add(pd);

    }

    /**
     * Copies the clones of a list {@code newList} to another list {@code orig}
     * @param orig The overwritten/refilled list.
     * @param newList The list containing Params to be copied.
     * @throws CloneNotSupportedException
     */
    public static void refillList(List<Param> orig, List<Param> newList) throws CloneNotSupportedException {
        orig.clear();
        for(Param p : newList)
            orig.add((Param) p.clone());
    }

    /**
     * Clones of a list {@code orig} to another list {@code list}
     * @param orig The list to be cloned.
     * @return The new cloned {@code list}.
     * @throws CloneNotSupportedException
     */
    public static List<Param> cloneParamList( List<Param> orig) throws CloneNotSupportedException {
        List<Param> list =  new LinkedList<Param>();
        for(Param p : orig)
            list.add((Param) p.clone());
        return list;

    }

    /**
     * Clones the Parameter, calling the {@link ParameterDependency#clone()} method for the dependencies.
     * @return The cloned Param.
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        Param p =   (Param)super.clone();
        p.dependencies = new LinkedList<>();
        for(ParameterDependency pd : this.dependencies)
            p.dependencies.add(pd.clone());
        return p;

    }

    /**
     * default String representation of the Param object.
     * @return
     */
    @Override
    public String toString() {
        return "Param{" +
                "dependencies=" + dependencies +
                ", name='" + name + '\'' +
                ", initValue=" + initValue +
                '}';
    }
}


