package elotech.taskmanager.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse response = buildResponse("NOT_FOUND", HttpStatus.NOT_FOUND, ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex,
                        HttpServletRequest request) {
                ErrorResponse response = buildResponse("BAD_REQUEST", HttpStatus.BAD_REQUEST, ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(BusinessRuleException.class)
        public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex,
                        HttpServletRequest request) {
                ErrorResponse response = buildResponse(ex.getErrorCode(), HttpStatus.BAD_REQUEST, ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex,
                        HttpServletRequest request) {
                ErrorResponse response = buildResponse("FORBIDDEN", HttpStatus.FORBIDDEN, ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
                        HttpServletRequest request) {
                ErrorResponse response = buildResponse("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Access denied",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                ErrorResponse response = buildResponse("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, message,
                                request.getRequestURI());
                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
                ErrorResponse response = buildResponse("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                                "Unexpected internal error",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        private ErrorResponse buildResponse(String code, HttpStatus status, String message, String path) {
                return ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .code(code)
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .message(message)
                                .path(path)
                                .build();
        }
}
