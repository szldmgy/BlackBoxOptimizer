package optimizer.exception;

public class JSONReadException extends RuntimeException {
    public JSONReadException(String error_during_deserialization_of_json) {
        super(error_during_deserialization_of_json);
    }
}
