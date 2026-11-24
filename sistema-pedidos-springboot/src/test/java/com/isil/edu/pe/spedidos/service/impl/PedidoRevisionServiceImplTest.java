package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.exception.revision.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.revision.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.revision.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.repository.PedidoRevisionJdbcRepository;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/**
 * PA4 - Reglas de negocio de la revisión con el Repository aislado mediante Mockito.
 */
class PedidoRevisionServiceImplTest {

  private PedidoRevisionJdbcRepository repository;
  private PedidoRevisionServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(PedidoRevisionJdbcRepository.class);
    service = new PedidoRevisionServiceImpl(repository);
  }

  @Test
  @DisplayName("pedidoId <= 0 se rechaza sin tocar el Repository")
  void rejectNonPositiveOrderIdWithoutRepositoryAccess() {
    RevisionInvalidaException error = assertThrows(RevisionInvalidaException.class,
        () -> service.revisarPedido(0, "Observación"));

    assertEquals("El ID del pedido debe ser mayor que cero.", error.getMessage());
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Observación vacía o en blanco se rechaza sin escrituras")
  void rejectBlankObservationWithoutWrites() {
    assertThrows(RevisionInvalidaException.class, () -> service.revisarPedido(5, "   "));
    assertThrows(RevisionInvalidaException.class, () -> service.revisarPedido(5, null));

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Observación mayor a 500 caracteres se rechaza")
  void rejectObservationLongerThanColumnSize() {
    String larga = "x".repeat(501);

    assertThrows(RevisionInvalidaException.class, () -> service.revisarPedido(5, larga));
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("Pedido inexistente: no se ejecuta UPDATE ni INSERT")
  void rejectUnknownOrderWithoutUpdateOrInsert() {
    when(repository.buscarEstadoRevision(77L)).thenReturn(Optional.empty());

    assertThrows(PedidoNoEncontradoException.class, () -> service.revisarPedido(77L, "Ok"));

    verify(repository, never()).marcarComoRevisado(anyLong());
    verify(repository, never()).insertarRevision(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("Pedido ya revisado: no se ejecuta UPDATE ni INSERT (409)")
  void rejectAlreadyReviewedOrderWithoutWrites() {
    when(repository.buscarEstadoRevision(8L)).thenReturn(Optional.of(true));

    assertThrows(PedidoYaRevisadoException.class, () -> service.revisarPedido(8L, "Ok"));

    verify(repository, never()).marcarComoRevisado(anyLong());
    verify(repository, never()).insertarRevision(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("Revisión concurrente: UPDATE afecta 0 filas y no se inserta la traza")
  void rejectConcurrentReviewWhenConditionalUpdateAffectsNoRows() {
    when(repository.buscarEstadoRevision(9L)).thenReturn(Optional.of(false));
    when(repository.marcarComoRevisado(9L)).thenReturn(0);

    assertThrows(PedidoYaRevisadoException.class, () -> service.revisarPedido(9L, "Ok"));

    verify(repository, never()).insertarRevision(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("Violación de la restricción única se traduce a PedidoYaRevisadoException")
  void translateDuplicateKeyToAlreadyReviewed() {
    when(repository.buscarEstadoRevision(10L)).thenReturn(Optional.of(false));
    when(repository.marcarComoRevisado(10L)).thenReturn(1);
    when(repository.insertarRevision(eq(10L), eq("Ok"), any(LocalDateTime.class)))
        .thenThrow(new DuplicateKeyException("uk_pedido_revision_pedido"));

    assertThrows(PedidoYaRevisadoException.class, () -> service.revisarPedido(10L, "Ok"));
  }

  @Test
  @DisplayName("Revisión válida: ejecuta UPDATE y luego INSERT con la observación normalizada")
  void executeUpdateThenInsertWithTrimmedObservation() {
    when(repository.buscarEstadoRevision(11L)).thenReturn(Optional.of(false));
    when(repository.marcarComoRevisado(11L)).thenReturn(1);
    when(repository.insertarRevision(eq(11L), eq("Conforme"), any(LocalDateTime.class))).thenReturn(3L);

    RevisionPedidoResultado resultado = service.revisarPedido(11L, "  Conforme ");

    assertEquals(11L, resultado.pedidoId());
    assertEquals(3L, resultado.revisionId());
    assertEquals("Conforme", resultado.observacion());
    verify(repository).marcarComoRevisado(11L);
    verify(repository).insertarRevision(eq(11L), eq("Conforme"), any(LocalDateTime.class));
  }
}
