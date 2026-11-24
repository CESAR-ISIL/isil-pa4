package com.isil.edu.pe.pedidos.reportes.service.domain;

import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import java.util.Locale;

/**
 * PA4 - Estados de revisión publicados por el recurso {@code /api/v1/pedidos/revisiones}.
 */
public enum EstadoRevision {
  PENDIENTE,
  REVISADO;

  /**
   * Convierte el parámetro {@code estado} (sin distinguir mayúsculas).
   *
   * @return el estado, o {@code null} si no se envió (= todos)
   * @throws EstadoRevisionInvalidoException si el valor no es PENDIENTE ni REVISADO (HTTP 400)
   */
  public static EstadoRevision desdeParametro(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return EstadoRevision.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new EstadoRevisionInvalidoException(valor);
    }
  }

  public boolean comoRevisado() {
    return this == REVISADO;
  }
}
