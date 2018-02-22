package optimizer.math;

/**
 * k(u,v) = exp(-||u-v||^2 / 2*sigma^2)
 */
public class GaussianKernel implements MercerKernel<double[]>{
    private double gamma;

    public GaussianKernel(double sigma) {
        if(sigma <= 0) {
            throw new IllegalArgumentException("sigma is not positive");
        } else {
            this.gamma = 0.5 / (sigma * sigma);
        }
    }

    public double k(double[] x, double[] y) {
        if(x.length != y.length) {
            throw new IllegalArgumentException("arrays have different length");
        } else {
            double s = 0;
            for(int i = 0; i < x.length; ++i)
                s += (x[i]-y[i]) * (x[i]-y[i]);
            return Math.exp(-this.gamma * s);
        }
    }
}
