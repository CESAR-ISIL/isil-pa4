package com.isil.edu.pe.pedidos.reportes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import com.isil.edu.pe.pedidos.reportes.repository.PedidoRevisionReporteJdbcRepository;
import com.isil.edu.pe.pedidos.reportes.repository.projection.EstadisticaRevisionProjection;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** PA4 - Reglas de cálculo y validación del Service (Repository aislado con Mockito). */
class PedidoRevisionReporteServiceImplTest {

  private PedidoRevisionReporteJdbcRepository repository;
  private PedidoRevisionReporteServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(PedidoRevisionReporteJdbcRepository.class);
    service = new PedidoRevisionReporteServiceImpl(repository);
  }

  @Test
  @DisplayName("Calcula pendientes y porcentaje revisado con 2 decimales (2 de 3 = 66.67)")
  void calculatePendingAndPercentageWithTwoDecimals() {
    // Arrange
    when(repository.contarRevisiones()).thenReturn(new EstadisticaRevisionProjection(3, 2));

    // Act
    EstadisticaRevision resultado = service.obtenerEstadisticas();

    // Assert
    assertEquals(3, resultado.totalPedidos());
    assertEquals(2, resultado.totalRevisados());
    assertEquals(1, resultado.totalPendientes());
    assertEquals(new BigDecimal("66.67"), resultado.porcentajeRevisado());
  }

  @Test
  @DisplayName("Sin pedidos el porcentaje es 0.00 (sin división entre cero)")
  void returnZeroPercentageWhenThereAreNoOrders() {
    // Arrange
    when(repository.contarRevisiones()).thenReturn(new EstadisticaRevisionProjection(0, 0));

    // Act
    EstadisticaRevision resultado = service.obtenerEstadisticas();

    // Assert
    assertEquals(0, resultado.totalPendientes());
    assertEquals(new BigDecimal("0.00"), resultado.porcentajeRevisado());
  }

  @Test
  @DisplayName("Estado inválido lanza excepción y no consulta el Repository")
  void rejectInvalidStateWithoutQueryingRepository() {
    // Act + Assert
    assertThrows(EstadoRevisionInvalidoException.class, () -> service.listarRevisiones("ANULADO"));
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Estado 'revisado' (minúsculas) consulta solo pedidos revisados")
  void queryReviewedOrdersWhenEstadoIsLowercase() {
    // Arrange
    when(repository.listarPorRevisado(true)).thenReturn(List.of());

    // Act
    service.listarRevisiones("revisado");

    // Assert
    verify(repository).listarPorRevisado(true);
  }
}
