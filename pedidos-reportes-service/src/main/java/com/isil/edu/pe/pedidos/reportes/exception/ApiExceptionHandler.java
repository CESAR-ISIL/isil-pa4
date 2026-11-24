package com.isil.edu.pe.pedidos.reportes.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(PedidoNoEncontradoException.class)
  public ProblemDetail pedidoNoEncontrado(PedidoNoEncontradoException e) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    problem.setTitle("Pedido no encontrado");
    return problem;
  }

  /** PA4 - estado distinto de PENDIENTE/REVISADO -> 400 Bad Request. */
  @ExceptionHandler(EstadoRevisionInvalidoException.class)
  public ProblemDetail estadoRevisionInvalido(EstadoRevisionInvalidoException e) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problem.setTitle("Parámetro inválido");
    return problem;
  }

  /**
   * PA4 - La base de datos no responde o falla la consulta -> 503 Service Unavailable.
   * El detalle técnico se registra en el log, pero no se envía al cliente.
   */
  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail errorBaseDatos(DataAccessException e) {
    log.error("Error de acceso a datos en pedidos-reportes-service", e);
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
        "El repositorio de reportes no está disponible temporalmente.");
    problem.setTitle("Servicio no disponible");
    return problem;
  }
}
