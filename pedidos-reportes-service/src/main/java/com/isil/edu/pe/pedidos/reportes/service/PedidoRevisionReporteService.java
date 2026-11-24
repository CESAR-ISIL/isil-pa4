package com.isil.edu.pe.pedidos.reportes.service;

import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevision;
import java.util.List;

/** PA4 - Casos de uso de lectura sobre el estado de revisión de pedidos. */
public interface PedidoRevisionReporteService {

  /**
   * @param estado PENDIENTE, REVISADO o {@code null}/vacío para todos
   * @throws EstadoRevisionInvalidoException si el estado no es válido
   */
  List<PedidoRevision> listarRevisiones(String estado);

  /** Calcula totalPedidos, totalRevisados, totalPendientes y porcentajeRevisado. */
  EstadisticaRevision obtenerEstadisticas();
}
