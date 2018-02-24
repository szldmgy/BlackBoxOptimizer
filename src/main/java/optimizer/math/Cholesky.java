package optimizer.math;

/**
 * Matrix in Cholesky form
 * Created by david on 2018. 02. 22..
 */
public class Cholesky {

    protected Matrix L;

    /**
     * Create Cholesky from @param M
     */
    public Cholesky(Matrix M) {
        if (M.getNrows() != M.getNcols()) {
            throw new UnsupportedOperationException("Cholesky constructor on a non-square matrix");
        }
        this.L = M;
    }

    /**
     * Returns with the inverse of L.
     * @return L^(-1)
     */
    public Matrix inverse() {
        int n = L.getNrows();
        Matrix inv = Matrix.identity(n);
        solve(inv);
        return inv;
    }

    /**
     * Solve the equation L * x = b
     * @param b column vector represented by an array. b will be overwritten by x.
     */
    public void solve(double[] b) {
        Matrix B = new Matrix(b);
        solve(B);
    }

    /**
     * Solve the equation L * X = B
     * @param B matrix, B will be overwritten by X.
     */
    public void solve(Matrix B) {
        if (B.getNrows() != L.getNrows()) {
            throw new IllegalArgumentException(String.format("Row dimensions do not agree"));
        }

        int n = B.getNrows();
        int nrhs = B.getNcols();

        // Solve L*Y = B;
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < nrhs; j++) {
                for (int i = 0; i < k; i++) {
                    B.sub(k, j, B.get(i, j) * L.get(k, i));
                }
                B.div(k, j, L.get(k, k));
            }
        }

        // Solve L'*X = Y;
        for (int k = n - 1; k >= 0; k--) {
            for (int j = 0; j < nrhs; j++) {
                for (int i = k + 1; i < n; i++) {
                    B.sub(k, j, B.get(i, j) * L.get(i, k));
                }
                B.div(k, j, L.get(k, k));
            }
        }
    }

}
