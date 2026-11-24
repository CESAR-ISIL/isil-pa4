package com.isil.edu.pe.spedidos.client.reportes;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoReporteResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ProductoVendidoResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ResumenPedidosResponse;
import java.util.List;

public interface PedidoReporteClient {

  ResumenPedidosResponse obtenerResumen();

  List<PedidoReporteResponse> buscarPorCliente(String cliente);

  List<ProductoVendidoResponse> listarProductosMasVendidos();

  // ===================== PA4 =====================

  /**
   * Consume {@code GET /api/v1/pedidos/revisiones?estado=PENDIENTE|REVISADO}.
   *
   * @param estado PENDIENTE, REVISADO o {@code null} para todos
   * @throws PedidoReporteNoDisponibleException     timeout, error de conexión o 5xx
   * @throws PedidoReporteSolicitudInvalidaException respuesta 4xx del servicio
   */
  List<PedidoRevisionResponse> listarRevisiones(String estado);

  /**
   * Consume {@code GET /api/v1/pedidos/estadisticas/revisiones}.
   *
   * @throws PedidoReporteNoDisponibleException timeout, error de conexión, 5xx o respuesta vacía
   */
  EstadisticaRevisionResponse obtenerEstadisticasRevision();

}
