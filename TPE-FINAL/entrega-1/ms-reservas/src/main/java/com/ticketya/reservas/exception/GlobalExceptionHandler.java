package com.ticketya.reservas.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return armar(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler({OperacionInvalidaException.class, LugarNoDisponibleException.class})
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest req) {
        return armar(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /**
     * Si dos transacciones chocan a nivel base (lock o version) lo tratamos igual que
     * un lugar no disponible: el cliente tiene que elegir otro asiento o reintentar.
     */
    @ExceptionHandler({PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> concurrencia(RuntimeException ex, HttpServletRequest req) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return armar(HttpStatus.CONFLICT, "El lugar fue tomado por otra compra, intentá nuevamente", req, null);
    }

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<ErrorResponse> limite(LimiteExcedidoException ex, HttpServletRequest req) {
        return armar(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccesoDenegadoException ex, HttpServletRequest req) {
        return armar(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<ErrorResponse> servicioExterno(ServicioExternoException ex, HttpServletRequest req) {
        log.error("Error comunicandose con otro servicio: {}", ex.getMessage());
        return armar(HttpStatus.BAD_GATEWAY, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return armar(HttpStatus.BAD_REQUEST, "Datos invalidos", req, detalles);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> requestInvalido(Exception ex, HttpServletRequest req) {
        return armar(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    private ResponseEntity<ErrorResponse> armar(HttpStatus status, String mensaje, HttpServletRequest req,
                                                List<String> detalles) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(body);
    }
}
