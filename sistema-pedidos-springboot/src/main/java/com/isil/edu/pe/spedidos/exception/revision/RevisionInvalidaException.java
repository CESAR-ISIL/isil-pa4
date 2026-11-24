package com.isil.edu.pe.spedidos.exception.revision;

import com.isil.edu.pe.spedidos.exception.PedidoException;

/**
 * PA4 - Datos de entrada inválidos para la revisión (pedidoId <= 0, observación vacía o demasiado larga). Equivale a HTTP 400.
 *
 * <p>Es una excepción de ejecución (RuntimeException): si se lanza dentro del método
 * {@code @Transactional}, Spring revierte la transacción.</p>
 */
public class RevisionInvalidaException extends PedidoException {

  public RevisionInvalidaException(String message) {
    super(message);
  }

  public RevisionInvalidaException(String message, Throwable cause) {
    super(message, cause);
  }
}
