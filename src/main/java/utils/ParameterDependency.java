package utils;

import java.util.LinkedList;
import java.util.List;

public class ParameterDependency<T2,T1> implements Cloneable{
    Range<T1> rangeOfThis;
    Param<T2> p;
    Range<T2> rangeOfOther;

    public Range<T1> getRangeOfThis() {
        return rangeOfThis;
    }

    public void setRangeOfThis(Range<T1> rangeOfThis) {
        this.rangeOfThis = rangeOfThis;
    }

    public Param<T2> getP() {
        return p;
    }

    public void setP(Param<T2> p) {
        this.p = p;
    }

    public Range<T2> getRangeOfOther() {
        return rangeOfOther;
    }

    public void setRangeOfOther(Range<T2> rangeOfOther) {
        this.rangeOfOther = rangeOfOther;
    }

    public <Boolean> boolean comply(){
        if(p==null)
            return true;
        return rangeOfOther.comply(p.getValue());
    }

    @Override
    public String toString() {
        return "ParameterDependency{" +
                "rangeOfThis=" + rangeOfThis.toString() +
                ", p=" + (p==null? "null" : p.getName() )+
                ", rangeOfOther=" + (p == null ? "null" : rangeOfOther) +
                '}';
    }

    public ParameterDependency(T1 lower, T1 upper, Param<T2> p, T2 lowerBound , T2 upperBound) {
        this.rangeOfThis = new Range<T1>(upper,lower);
        this.p = p;
        if(p!=null && lowerBound!=null && upperBound!=null) {
            if (p.isEnumeration()) {
                List<T2> res = new LinkedList<T2>();
                // TODO: 2018. 01. 25. this is a quick fix
                boolean afterFirst = false, beforeLast = true;
                for(T2 element : p.getAllValueArray()  ) {
                    if (element.equals(lowerBound))
                        afterFirst = true;
                    if(element.equals(upperBound)){
                        res.add(element);
                        beforeLast = false;
                    }
                    if(afterFirst&&beforeLast)
                        res.add(element);
                }
                this.rangeOfOther = new Range<T2>((T2[])res.toArray());
            }
            else
                this.rangeOfOther = new Range<T2>(lowerBound, upperBound);

        }
    }



    public ParameterDependency(T1[] array) {
        this.rangeOfThis = new Range<T1>(array);
        this.p = null;
        this.rangeOfOther = null;
    }
    public ParameterDependency(T1 lower, T1 upper) {
        this.rangeOfThis = new Range<T1>(upper,lower);
        this.p = null;
        this.rangeOfOther = null;
    }
    public ParameterDependency(){
        this.rangeOfThis = null;
        this.p = null;
        this.rangeOfOther = null;
    }

    public Object clone() throws CloneNotSupportedException {
        ParameterDependency pd =   (ParameterDependency)super.clone();
        pd.rangeOfThis = (Range) this.rangeOfThis.clone();
        if(this.p != null) {
            pd.rangeOfOther = (Range) this.rangeOfOther.clone();
            pd.p = (Param) this.p.clone();
        }
        return pd;

    }


}
