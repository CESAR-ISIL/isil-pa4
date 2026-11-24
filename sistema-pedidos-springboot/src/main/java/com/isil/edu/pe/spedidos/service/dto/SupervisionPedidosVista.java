package com.isil.edu.pe.spedidos.service.dto;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import java.util.List;

/**
 * PA4 - Datos que necesita la vista {@code supervision-pedidos.jsp}.
 *
 * @param pedidos             filas a mostrar
 * @param estadisticas        resumen del servicio de reportes; {@code null} si no está disponible
 * @param estadoFiltro        filtro aplicado (PENDIENTE, REVISADO o {@code null} = todos)
 * @param reportesDisponible  {@code true} si los datos provienen de pedidos-reportes-service
 * @param aviso               mensaje para el usuario cuando se usó el respaldo local o el filtro fue inválido
 */
public record SupervisionPedidosVista(
    List<PedidoSupervisionItem> pedidos,
    EstadisticaRevisionResponse estadisticas,
    String estadoFiltro,
    boolean reportesDisponible,
    String aviso
) {
  public List<PedidoSupervisionItem> getPedidos() { return pedidos; }
  public EstadisticaRevisionResponse getEstadisticas() { return estadisticas; }
  public String getEstadoFiltro() { return estadoFiltro; }
  public boolean isReportesDisponible() { return reportesDisponible; }
  public String getAviso() { return aviso; }
}
