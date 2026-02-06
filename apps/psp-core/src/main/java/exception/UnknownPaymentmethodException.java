package exception;

public class UnknownPaymentmethodException extends RuntimeException {
    public UnknownPaymentmethodException(String methodName) {
        super("Nepoznata metoda plaćanja: " + methodName);
    }
}