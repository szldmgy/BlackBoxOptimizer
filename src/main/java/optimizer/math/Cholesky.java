package optimizer.math;

/**
 * Created by david on 2018. 02. 22..
 */
public class Cholesky {

    protected Matrix L;


    public Cholesky(Matrix M) {
        if (M.getNrows() != M.getNcols()) {
            throw new UnsupportedOperationException("Cholesky constructor on a non-square matrix");
        }
        this.L = M;
    }

    public Matrix inverse() {
        int n = L.getNrows();
        Matrix inv = Matrix.identity(n);
        solve(inv);
        return inv;
    }

    public void solve(double[] b) {
        Matrix B = new Matrix(b);
        solve(B);
    }

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
