package optimizer.math;

/**
 * Created by david on 2017. 09. 22..
 */
public class GaussianProcessRegressionWithVariance<T> {

    private T[] knots;
    private double[] w;
    private MercerKernel<T> kernel;
    private double lambda;
    private Matrix KInverse;

    public GaussianProcessRegressionWithVariance() {}

    public GaussianProcessRegressionWithVariance(T[] x, double[] y, MercerKernel<T> kernel, double lambda) {
        if(x.length != y.length) {
            throw new IllegalArgumentException(String.format("The sizes of X and Y don't match: %d != %d", new Object[]{Integer.valueOf(x.length), Integer.valueOf(y.length)}));
        } else if(lambda < 0.0D) {
            throw new IllegalArgumentException("Invalid regularization parameter lambda = " + lambda);
        } else {
            this.kernel = kernel;
            this.lambda = lambda;
            this.knots = x;
            int n = x.length;
            Matrix K = new Matrix(n, n);

            for(int i = 0; i < n; ++i) {
                for(int j = 0; j <= i; ++j) {
                    double k = kernel.k(x[i], x[j]);
                    K.set(i, j, k);
                    K.set(j, i, k);
                }

                K.add(i, i, lambda);
            }

            Cholesky cholesky = K.cholesky();
            KInverse = cholesky.inverse();
            this.w = y.clone();
            this.w = KInverse.ax(y);
        }
    }


    public double[] coefficients() {
        return this.w;
    }

    public double shrinkage() {
        return this.lambda;
    }

    public double predict(T x) {
        double f = 0.0D;

        for(int i = 0; i < this.knots.length; ++i) {
            f += this.w[i] * this.kernel.k(x, this.knots[i]);
        }

        return f;
    }

    public double variance(T x) {
        int n = knots.length;
        double[] k = new double[n];
        for(int i = 0; i < n; ++i) {
            k[i] = kernel.k(x,knots[i]);
        }
        return KInverse.xax(k);
    }
}
