package iuh.fit.authservice.exception;

public class UnauthorizedException
        extends RuntimeException {

    public UnauthorizedException(
            String message) {

        super(message);
    }
}