package com.isil.edu.pe.pedidos.reportes.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevision;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PA4 - Contrato JSON de {@code GET /api/v1/pedidos/revisiones}.
 * {@code observacion} y {@code fechaRevision} se incluyen solo si el pedido está revisado.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PedidoRevisionDto(
    Long id,
    String cliente,
    String productoCodigo,
    String productoNombre,
    int cantidad,
    BigDecimal total,
    LocalDateTime fecha,
    boolean revisado,
    String estadoRevision,
    String observacion,
    LocalDateTime fechaRevision
) {
  public static PedidoRevisionDto from(PedidoRevision p) {
    return new PedidoRevisionDto(
        p.id(), p.cliente(), p.productoCodigo(), p.productoNombre(), p.cantidad(), p.total(),
        p.fecha(), p.revisado(), p.estado().name(),
        p.revisado() ? p.observacion() : null,
        p.revisado() ? p.fechaRevision() : null);
  }
}
