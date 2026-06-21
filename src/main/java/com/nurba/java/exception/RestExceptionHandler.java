package com.nurba.java.exception;

import com.nurba.java.payment.PayPalApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        detail.setTitle("Validation Failed");
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }

    @ExceptionHandler(PublicationValidationException.class)
    public ResponseEntity<ProblemDetail> handlePublicationValidation(PublicationValidationException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Design cannot be published");
        detail.setTitle("Design Not Ready");
        detail.setProperty("code", "DESIGN_NOT_READY");
        detail.setProperty("errors", ex.getErrors());
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        // Surface duplicate garment type constraint as a readable error
        if (msg != null && msg.contains("uq_design_garment_type")) {
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "A variant of this garment type already exists for this design.");
            detail.setTitle("Duplicate Garment Type");
            detail.setProperty("code", "DUPLICATE_GARMENT_TYPE");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
        }
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Data integrity constraint violated.");
        detail.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handlePessimisticLock(PessimisticLockingFailureException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Запрос конкурирует с другой операцией. Пожалуйста, повторите попытку.");
        detail.setTitle("Conflict");
        detail.setProperty("code", "LOCK_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(PayPalApiException.class)
    public ResponseEntity<ProblemDetail> handlePayPalApi(PayPalApiException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "PayPal API error: " + ex.getMessage());
        detail.setTitle("Payment Provider Error");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(detail);
    }
}
