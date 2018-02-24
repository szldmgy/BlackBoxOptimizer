package optimizer.math;

/**
 * Interface for kernels used by Gaussian processes
 * Created by david on 2018. 02. 22..
 */

public interface MercerKernel<T> {
    public double k(T x, T y);
}
