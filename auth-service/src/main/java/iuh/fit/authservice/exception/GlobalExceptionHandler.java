package iuh.fit.authservice.exception;

import iuh.fit.common.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            UserAlreadyExistsException.class
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        return new ErrorResponse(
                LocalDateTime.now(),
                409,
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            UnauthorizedException.class
    )
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {

        return new ErrorResponse(
                LocalDateTime.now(),
                401,
                "UNAUTHORIZED",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            InvalidTokenException.class
    )
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {

        return new ErrorResponse(
                LocalDateTime.now(),
                401,
                "INVALID_TOKEN",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            BadCredentialsException.class
    )
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {

        return new ErrorResponse(
                LocalDateTime.now(),
                401,
                "BAD_CREDENTIALS",
                "Invalid username or password",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(
            HttpStatus.INTERNAL_SERVER_ERROR
    )
    public ErrorResponse handleException(
            Exception ex,
            HttpServletRequest request
    ) {

        return new ErrorResponse(
                LocalDateTime.now(),
                500,
                "INTERNAL_SERVER_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        );
    }
}