package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException;
import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionResponse;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.dto.PedidoSupervisionItem;
import com.isil.edu.pe.spedidos.service.dto.SupervisionPedidosVista;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/**
 * PA4 - La vista de supervisión usa el cliente HTTP y, ante indisponibilidad,
 * responde de forma controlada con datos locales.
 */
class SupervisionPedidosServiceImplTest {

  private PedidoReporteClient client;
  private PedidoRevisionService revisionService;
  private SupervisionPedidosServiceImpl service;

  @BeforeEach
  void setUp() {
    client = mock(PedidoReporteClient.class);
    revisionService = mock(PedidoRevisionService.class);
    service = new SupervisionPedidosServiceImpl(client, revisionService);
  }

  @Test
  @DisplayName("Servicio disponible: usa listado y estadísticas de pedidos-reportes-service")
  void useRemoteDataWhenReportServiceIsAvailable() {
    PedidoRevisionResponse remoto = new PedidoRevisionResponse(1L, "Ana", "LAP-001", "Laptop", 1,
        new BigDecimal("2500.00"), LocalDateTime.now(), false, "PENDIENTE", null, null);
    EstadisticaRevisionResponse estadisticas =
        new EstadisticaRevisionResponse(1, 0, 1, new BigDecimal("0.00"));
    when(client.listarRevisiones("PENDIENTE")).thenReturn(List.of(remoto));
    when(client.obtenerEstadisticasRevision()).thenReturn(estadisticas);

    SupervisionPedidosVista vista = service.obtenerVista("pendiente");

    assertTrue(vista.reportesDisponible());
    assertEquals("PENDIENTE", vista.estadoFiltro());
    assertEquals(1, vista.pedidos().size());
    assertEquals(estadisticas, vista.estadisticas());
    verify(revisionService, never()).listarPedidosLocales();
  }

  @Test
  @DisplayName("Servicio caído: respuesta controlada con datos locales y aviso, sin excepción")
  void fallbackToLocalDataWhenReportServiceIsDown() {
    when(client.listarRevisiones(null)).thenThrow(new PedidoReporteNoDisponibleException(
        "pedidos-reportes-service no está disponible.", new ResourceAccessException("Connection refused")));
    PedidoSupervisionItem local = new PedidoSupervisionItem(3L, "Luis", "Mouse", 1,
        new BigDecimal("50.00"), false, null, null);
    when(revisionService.listarPedidosLocales()).thenReturn(List.of(local));

    SupervisionPedidosVista vista = service.obtenerVista(null);

    assertFalse(vista.reportesDisponible());
    assertNull(vista.estadisticas());
    assertEquals(List.of(local), vista.pedidos());
    assertEquals(SupervisionPedidosServiceImpl.AVISO_NO_DISPONIBLE, vista.aviso());
    assertFalse(vista.aviso().contains("Connection refused"), "No se exponen detalles técnicos");
  }

  @Test
  @DisplayName("Filtro de estado inválido: se ignora y se informa al usuario")
  void ignoreInvalidFilterAndWarnUser() {
    when(client.listarRevisiones(null)).thenReturn(List.of());
    when(client.obtenerEstadisticasRevision())
        .thenReturn(new EstadisticaRevisionResponse(0, 0, 0, BigDecimal.ZERO));

    SupervisionPedidosVista vista = service.obtenerVista("ANULADO");

    assertNull(vista.estadoFiltro());
    assertTrue(vista.aviso().contains("no es válido"));
    verify(client).listarRevisiones(null);
  }
}
