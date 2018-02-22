package optimizer.math;

/**
 * Created by david on 2018. 02. 22..
 */
public class Matrix {
    protected double[][] M;

    public int getNrows() {
        return nrows;
    }

    public int getNcols() {
        return ncols;
    }

    protected int nrows;
    protected int ncols;

    public Matrix(int m, int n) {
        this.nrows = m;
        this.ncols = n;
        this.M = new double[m][n];
    }

    public Matrix(double[] b) {
        this.nrows = b.length;
        this.ncols = 1;
        this.M = new double[nrows][1];
        for(int i = 0; i < nrows; ++i) {
            this.M[i][0] = b[i];
        }
    }

    public void set(int i, int j, double k) {
        M[i][j] = k;
    }

    public double get(int i, int j) {
        return M[i][j];
    }

    public void add(int i, int j, double k) {
        M[i][j] += k;
    }

    public void sub(int i, int j, double k) {
        M[i][j] -= k;
    }

    public void mul(int i, int j, double k) {
        M[i][j] *= k;
    }

    public void div(int i, int j, double k) {
        M[i][j] /= k;
    }

    public Cholesky cholesky() {
        if (nrows != ncols) {
            throw new UnsupportedOperationException("Cholesky decomposition on non-square matrix");
        }

        int n = this.nrows;

        // Main loop.
        for (int j = 0; j < n; j++) {
            double d = 0.0;
            for (int k = 0; k < j; k++) {
                double s = 0.0;
                for (int i = 0; i < k; i++) {
                    s += M[k][i] * M[j][i];
                }
                s = (M[j][k] - s) / M[k][k];
                M[j][k] = s;
                d = d + s * s;
            }
            d = M[j][j] - d;

            if (d < 0.0) {
                throw new IllegalArgumentException("The matrix is not positive definite.");
            }
            M[j][j] = Math.sqrt(d);
        }
        return new Cholesky(this);
    }

    public static Matrix identity(int n) {
        Matrix m = new Matrix(n, n);
        for (int i = 0; i < n; i++) {
            m.set(i,i,1.0);
        }
        return m;
    }

    public  double[] ax(double[] x) {
        if (ncols != x.length) {
            throw new UnsupportedOperationException("Matrix and vector size do not match");
        }
        double[] y = new double[nrows];
        for(int i = 0; i < nrows; ++i) {
            for(int j = 0; j < ncols; ++j) {
                y[i] += M[i][j] * x[j];
            }
        }
        return y;
    }


    public double xax(double[] x) {
        if (nrows != ncols) {
            throw new IllegalArgumentException("Not square matrix");
        }

        if (nrows != x.length) {
            throw new IllegalArgumentException("Matrix and vector size do not match");
        }

        int n = x.length;
        double s = 0.0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                s += get(i, j) * x[i] * x[j];
            }
        }

        return s;
    }


}
