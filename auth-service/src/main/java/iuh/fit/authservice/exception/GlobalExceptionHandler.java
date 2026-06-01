package iuh.fit.authservice.exception;

import iuh.fit.common.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                400,
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return new ErrorResponse(
                LocalDateTime.now(),
                400,
                "VALIDATION_ERROR",
                message.isBlank() ? "Dữ liệu không hợp lệ" : message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                400,
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                404,
                "NOT_FOUND",
                ex.getMessage(),
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