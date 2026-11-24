package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.service.dto.SupervisionPedidosVista;

/**
 * PA4 - Arma los datos de la vista de supervisión consumiendo
 * pedidos-reportes-service mediante {@code PedidoReporteClient}.
 *
 * <pre>
 * SupervisionPedidoController -> SupervisionPedidosService -> PedidoReporteClient -> HTTP -> pedidos-reportes-service
 * </pre>
 */
public interface SupervisionPedidosService {

  /**
   * @param estado PENDIENTE, REVISADO o vacío/{@code null} para todos
   * @return datos de la vista; nunca lanza excepciones técnicas por indisponibilidad del servicio remoto
   */
  SupervisionPedidosVista obtenerVista(String estado);
}
