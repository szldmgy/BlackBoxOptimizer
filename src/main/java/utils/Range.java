package utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Range<T> implements Cloneable{
    private  T upperBound;
    private T lowerBound;

    public T[] getValueArray() {
        return valueArray;
    }

    private T[] valueArray;

    public T getUpperBound() {
        return upperBound;
    }

    public T getLowerBound() {
        return lowerBound;
    }

    @Override
    public String toString() {
        return "Range{" +
                "upperBound=" + upperBound +
                ", lowerBound=" + lowerBound +
                '}';
    }

    public Range(T upperBound, T lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }
    public Range(T[] array, T from, T to) {

        List<T> arr1 = Arrays.asList(array);
        List<T> arr2 = new LinkedList<T>();
        for(int i = arr1.indexOf(from);i<=arr1.indexOf(to);i++)
            arr2.add(arr1.get(i));
        T[] arr = (T[]) arr2.toArray();
        this.lowerBound = arr[0];
        this.upperBound = arr[arr.length-1];
        this.valueArray = arr;
    }
    public Range(T[] array) {
        this.lowerBound = array[0];
        this.upperBound = array[array.length-1];
        this.valueArray = Arrays.copyOf(array,array.length);
    }
    @Override
    public Object clone() throws CloneNotSupportedException {
        return (Range)super.clone();
    }

    public static <T> Range<T> intersection(Range<T> r1, Range<T> r2){
        if(r1==null)
            return r2;
        if(r2==null)
            return r1;
        if(r1.lowerBound.getClass().equals(Integer.class)) {
            Range<Integer> r1n = (Range<Integer>) r1;
            Range<Integer> r2n = (Range<Integer>) r2;
            Range<Integer> min = (r1n.lowerBound < r2n.lowerBound ? r1n : r2n);
            Range<Integer> max = (min.equals(  r1n) ? r2n : r2n);

            //min ends before max starts -> no intersection
            if (min.upperBound < max.lowerBound)
                return null;//the ranges don't intersect

            return (Range<T>)new Range<Integer>( (min.upperBound < max.upperBound ? min.upperBound : max.upperBound),max.lowerBound);
        }
        if(r1.lowerBound.getClass().equals(Float.class)) {
            Range<Float> r1n = (Range<Float>) r1;
            Range<Float> r2n = (Range<Float>) r2;
            Range<Float> min = (r1n.lowerBound < r2n.lowerBound ? r1n : r2n);
            Range<Float> max = (min.equals(r1n) ? r2n : r2n);

            //min ends before max starts -> no intersection
            if (min.upperBound < max.lowerBound)
                return null;//the ranges don't intersect

            return (Range<T>)new Range<Float>( (min.upperBound < max.upperBound ? min.upperBound : max.upperBound),max.lowerBound);
        }
        if(r1.lowerBound.getClass().equals(Double.class)) {
            Range<Double> r1n = (Range<Double>) r1;
            Range<Double> r2n = (Range<Double>) r2;
            //Range<Double> min = (r1n.lowerBound < r2n.lowerBound ? r1n : r2n);
            //Range<Double> max = (min.equals(r1n) ? r2n : r2n);
            Double min = (r1n.lowerBound < r2n.lowerBound ? r2n.lowerBound : r1n.lowerBound);
            Double max = (r1n.upperBound > r2n.upperBound ? r2n.upperBound : r1n.upperBound );

            //min ends before max starts -> no intersection
            if (min > max)
                return null;//the ranges don't intersect

            return (Range<T>)new Range<Double>(max,min);
        }
        if(r1.lowerBound.getClass().equals(Boolean.class)) {
            Range<Boolean> r1n = (Range<Boolean>) r1;
            Range<Boolean> r2n = (Range<Boolean>) r2;
            return (Range<T>)new Range<Boolean>(r1n.lowerBound && r2n.lowerBound,r1n.lowerBound && r2n.lowerBound);
        }
        if(r1.lowerBound.getClass().equals(String.class)){

            int r1Lower = Arrays.asList(r1.valueArray).indexOf(r1.lowerBound);
            int r2Lower =  Arrays.asList(r2.valueArray).indexOf(r2.lowerBound);
            int r1Upper = Arrays.asList(r1.valueArray).indexOf(r1.upperBound);
            int r2Upper = Arrays.asList(r2.valueArray).indexOf(r2.upperBound);

           // int r1Lower = Arrays.binarySearch(r1.valueArray,r1.lowerBound);
           // int r2Lower = Arrays.binarySearch(r2.valueArray,r2.lowerBound);
           // int r1Upper = Arrays.binarySearch(r1.valueArray,r1.upperBound);
           // int r2Upper = Arrays.binarySearch(r2.valueArray,r2.upperBound);
            //// TODO: 22/09/17 is this always good what is empty intersection ??????

            int from = r1Lower<r2Lower?r2Lower:r1Lower;
            int to = r1Upper>r2Upper?r2Upper:r1Upper +1;
            return new Range<T>(Arrays.copyOfRange(r1.valueArray.length>r2.valueArray.length?r1.valueArray:r2.valueArray ,from,to));

        }
        return null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range<?> range = (Range<?>) o;

        if (!upperBound.equals(range.upperBound)) return false;
        return lowerBound.equals(range.lowerBound);

    }

    @Override
    public int hashCode() {
        int result = upperBound.hashCode();
        result = 31 * result + lowerBound.hashCode();
        return result;
    }

    public   boolean comply (T value){
        return  Utils.comply(value,lowerBound,upperBound);
    }

}
