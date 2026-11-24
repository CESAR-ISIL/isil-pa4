package com.isil.edu.pe.spedidos.client.reportes;

/**
 * PA4 - pedidos-reportes-service respondió 4xx (por ejemplo, 400 por un estado inválido).
 * Se diferencia de {@link PedidoReporteNoDisponibleException} (timeout, conexión o 5xx).
 */
public class PedidoReporteSolicitudInvalidaException extends RuntimeException {

  private final int status;

  public PedidoReporteSolicitudInvalidaException(int status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
