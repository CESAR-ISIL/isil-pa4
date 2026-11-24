package com.isil.edu.pe.spedidos.client.reportes.dto;

import java.math.BigDecimal;

/**
 * PA4 - DTO de transporte para {@code GET /api/v1/pedidos/estadisticas/revisiones}.
 */
public record EstadisticaRevisionResponse(
    long totalPedidos,
    long totalRevisados,
    long totalPendientes,
    BigDecimal porcentajeRevisado
) {
  // Getters JavaBean para JSP EL.
  public long getTotalPedidos() { return totalPedidos; }
  public long getTotalRevisados() { return totalRevisados; }
  public long getTotalPendientes() { return totalPendientes; }
  public BigDecimal getPorcentajeRevisado() { return porcentajeRevisado; }
}
