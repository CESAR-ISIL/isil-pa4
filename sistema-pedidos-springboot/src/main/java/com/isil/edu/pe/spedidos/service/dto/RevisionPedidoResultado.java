package com.isil.edu.pe.spedidos.service.dto;

import java.time.LocalDateTime;

/**
 * PA4 - Resultado de una revisión confirmada (commit).
 */
public record RevisionPedidoResultado(
    long pedidoId,
    long revisionId,
    String observacion,
    LocalDateTime fechaRevision
) { }
