package com.isil.edu.pe.spedidos.exception.revision;

import com.isil.edu.pe.spedidos.exception.PedidoException;

/**
 * PA4 - El pedido ya tiene una revisión confirmada. Equivale a HTTP 409 Conflict.
 *
 * <p>Es una excepción de ejecución (RuntimeException): si se lanza dentro del método
 * {@code @Transactional}, Spring revierte la transacción.</p>
 */
public class PedidoYaRevisadoException extends PedidoException {

  public PedidoYaRevisadoException(String message) {
    super(message);
  }

  public PedidoYaRevisadoException(String message, Throwable cause) {
    super(message, cause);
  }
}
