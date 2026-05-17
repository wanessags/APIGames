package senac.tsi.games.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import senac.tsi.games.exceptions.ApiKeyNotFoundException;
import senac.tsi.games.exceptions.CategoryNotFoundException;
import senac.tsi.games.exceptions.ConflictException;
import senac.tsi.games.exceptions.GameDetailNotFoundException;
import senac.tsi.games.exceptions.GameNotFoundException;
import senac.tsi.games.exceptions.PlatformNotFoundException;
import senac.tsi.games.exceptions.ReviewNotFoundException;
import senac.tsi.games.exceptions.SearchResultNotFoundException;
import senac.tsi.games.exceptions.UserNotFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ApiKeyNotFoundException.class,
            CategoryNotFoundException.class,
            GameDetailNotFoundException.class,
            GameNotFoundException.class,
            PlatformNotFoundException.class,
            ReviewNotFoundException.class,
            SearchResultNotFoundException.class,
            UserNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos na requisição.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(error ->
                fields.put(error.getPropertyPath().toString(), error.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos na requisição.", request, fields);
    }

    @ExceptionHandler({
            ConversionFailedException.class,
            HttpMessageConversionException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, buildBadRequestMessage(ex), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Operação viola uma restrição de integridade dos dados."
                : ex.getMessage();
        return build(HttpStatus.CONFLICT, message, request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String message = rootMessage(ex);
        if (message.contains("CategoryType") || message.contains("Cannot deserialize value of type") || message.contains("No enum constant")) {
            return build(HttpStatus.BAD_REQUEST, buildBadRequestMessage(ex), request, Map.of());
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição.", request, Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request, Map<String, String> fields) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fields
        );
        return ResponseEntity.status(status).body(error);
    }

    private String buildBadRequestMessage(Exception ex) {
        String message = rootMessage(ex);

        if (message != null && message.contains("CategoryType")) {
            return "Tipo de categoria inválido. Use: RPG, ACTION, ADVENTURE, SPORTS ou STRATEGY.";
        }

        if (ex instanceof MethodArgumentTypeMismatchException mismatch && mismatch.getRequiredType() != null && mismatch.getRequiredType().isEnum()) {
            Object[] values = mismatch.getRequiredType().getEnumConstants();
            return "Valor inválido para " + mismatch.getName() + ". Use: " + java.util.Arrays.toString(values) + ".";
        }

        if (message == null || message.isBlank()) {
            return "Requisição inválida. Verifique os dados enviados.";
        }

        return message;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
