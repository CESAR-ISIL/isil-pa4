package com.isil.edu.pe.spedidos.exception.revision;

import com.isil.edu.pe.spedidos.exception.PedidoException;

/**
 * PA4 - El pedido a revisar no existe. Equivale a HTTP 404.
 *
 * <p>Es una excepción de ejecución (RuntimeException): si se lanza dentro del método
 * {@code @Transactional}, Spring revierte la transacción.</p>
 */
public class PedidoNoEncontradoException extends PedidoException {

  public PedidoNoEncontradoException(String message) {
    super(message);
  }

  public PedidoNoEncontradoException(String message, Throwable cause) {
    super(message, cause);
  }
}
