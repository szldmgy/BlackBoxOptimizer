package optimizer.exception;

public class AlgorithmException extends RuntimeException {
    public AlgorithmException(String algorithm_error) {
        super(algorithm_error);
    }
}
