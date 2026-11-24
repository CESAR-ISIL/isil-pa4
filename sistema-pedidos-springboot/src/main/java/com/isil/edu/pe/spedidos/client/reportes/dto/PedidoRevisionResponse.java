package com.isil.edu.pe.spedidos.client.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PA4 - DTO de transporte para {@code GET /api/v1/pedidos/revisiones}.
 *
 * <p>Representa el contrato JSON publicado por pedidos-reportes-service. No es una
 * entidad JPA: las aplicaciones no comparten entidades, solo contratos HTTP.</p>
 * <p>{@code observacion} y {@code fechaRevision} llegan solo cuando el pedido está revisado.</p>
 */
public record PedidoRevisionResponse(
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
) { }
