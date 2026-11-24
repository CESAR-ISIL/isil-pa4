package com.isil.edu.pe.pedidos.reportes.exception;

/** PA4 - Valor de {@code estado} no permitido. Se traduce a HTTP 400. */
public class EstadoRevisionInvalidoException extends RuntimeException {

  public EstadoRevisionInvalidoException(String valor) {
    super("El parámetro estado='" + valor + "' no es válido. Valores permitidos: PENDIENTE, REVISADO.");
  }
}
