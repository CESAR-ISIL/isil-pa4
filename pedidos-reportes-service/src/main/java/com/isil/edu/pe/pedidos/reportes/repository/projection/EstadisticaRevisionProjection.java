package com.isil.edu.pe.pedidos.reportes.repository.projection;

/** PA4 - Conteos crudos obtenidos con SQL agregado. */
public record EstadisticaRevisionProjection(
    long totalPedidos,
    long totalRevisados
) { }
