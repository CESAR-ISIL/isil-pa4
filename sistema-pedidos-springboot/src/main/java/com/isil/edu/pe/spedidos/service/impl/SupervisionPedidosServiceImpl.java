package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteSolicitudInvalidaException;
import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionResponse;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.SupervisionPedidosService;
import com.isil.edu.pe.spedidos.service.dto.PedidoSupervisionItem;
import com.isil.edu.pe.spedidos.service.dto.SupervisionPedidosVista;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PA4 - Fachada de lectura para la vista de supervisión.
 *
 * <ul>
 *   <li>Fuente principal: pedidos-reportes-service (listado + estadísticas) vía HTTP.</li>
 *   <li>Respaldo: si el servicio está caído (timeout / conexión / 5xx), se muestra la
 *       lista local obtenida por {@link PedidoRevisionService} (Service -> Repository JDBC)
 *       y un aviso comprensible. No se exponen trazas técnicas.</li>
 * </ul>
 *
 * <p>Este servicio NO es transaccional y NUNCA participa en la transacción de
 * escritura de la revisión: las llamadas HTTP quedan fuera de ese límite.</p>
 */
@Service
public class SupervisionPedidosServiceImpl implements SupervisionPedidosService {

  private static final Logger log = LoggerFactory.getLogger(SupervisionPedidosServiceImpl.class);
  private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "REVISADO");

  static final String AVISO_NO_DISPONIBLE =
      "El servicio de reportes (puerto 8083) no está disponible. "
          + "Se muestran los datos locales; las estadísticas no están disponibles.";

  private final PedidoReporteClient pedidoReporteClient;
  private final PedidoRevisionService pedidoRevisionService;

  public SupervisionPedidosServiceImpl(PedidoReporteClient pedidoReporteClient,
      PedidoRevisionService pedidoRevisionService) {
    this.pedidoReporteClient = pedidoReporteClient;
    this.pedidoRevisionService = pedidoRevisionService;
  }

  @Override
  public SupervisionPedidosVista obtenerVista(String estado) {
    String aviso = null;
    String filtro = normalizarEstado(estado);
    if (estado != null && !estado.isBlank() && filtro == null) {
      aviso = "El filtro de estado '" + estado.trim() + "' no es válido. Se muestran todos los pedidos.";
    }

    try {
      List<PedidoSupervisionItem> pedidos = pedidoReporteClient.listarRevisiones(filtro)
          .stream()
          .map(SupervisionPedidosServiceImpl::aItem)
          .toList();
      EstadisticaRevisionResponse estadisticas = pedidoReporteClient.obtenerEstadisticasRevision();
      return new SupervisionPedidosVista(pedidos, estadisticas, filtro, true, aviso);

    } catch (PedidoReporteNoDisponibleException | PedidoReporteSolicitudInvalidaException e) {
      log.warn("Supervisión en modo respaldo local: {}", e.getMessage());
      List<PedidoSupervisionItem> locales = filtrar(pedidoRevisionService.listarPedidosLocales(), filtro);
      return new SupervisionPedidosVista(locales, null, filtro, false, AVISO_NO_DISPONIBLE);
    }
  }

  private static String normalizarEstado(String estado) {
    if (estado == null || estado.isBlank()) {
      return null;
    }
    String valor = estado.trim().toUpperCase(Locale.ROOT);
    return ESTADOS_VALIDOS.contains(valor) ? valor : null;
  }

  private static List<PedidoSupervisionItem> filtrar(List<PedidoSupervisionItem> items, String filtro) {
    if (filtro == null) {
      return items;
    }
    boolean revisado = "REVISADO".equals(filtro);
    return items.stream().filter(item -> item.revisado() == revisado).toList();
  }

  private static PedidoSupervisionItem aItem(PedidoRevisionResponse r) {
    return new PedidoSupervisionItem(
        r.id(), r.cliente(), r.productoNombre(), r.cantidad(), r.total(),
        r.revisado(), r.observacion(), r.fechaRevision());
  }
}
