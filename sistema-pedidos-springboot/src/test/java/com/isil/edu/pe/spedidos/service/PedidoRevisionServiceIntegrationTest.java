package com.isil.edu.pe.spedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.revision.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.revision.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRevisionJdbcRepository;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PA4 - Pruebas de integración de la transacción local de revisión (Spring JDBC + H2).
 *
 * <p>La clase NO está anotada con {@code @Transactional}: cada llamada al Service
 * confirma (commit) o revierte (rollback) de verdad, y luego se verifica la base de datos
 * con consultas independientes.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class PedidoRevisionServiceIntegrationTest {

  /** Observación que activa la restricción CHECK temporal usada para forzar el fallo del INSERT. */
  private static final String OBSERVACION_QUE_FALLA = "FORZAR_FALLO_INSERT";
  private static final String CHECK_TEMPORAL = "ck_pa4_forzar_fallo";

  @Autowired
  private PedidoRevisionService pedidoRevisionService;

  @Autowired
  private PedidoRevisionJdbcRepository revisionRepository;

  @Autowired
  private PedidoRepository pedidoRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void limpiarAntes() {
    limpiarTablas();
  }

  @AfterEach
  void limpiarDespues() {
    jdbcTemplate.execute("ALTER TABLE pedido_revision DROP CONSTRAINT IF EXISTS " + CHECK_TEMPORAL);
    limpiarTablas();
  }

  @Test
  @DisplayName("Commit: una revisión válida marca revisado=true y crea exactamente una fila en pedido_revision")
  void commitReviewUpdatesOrderAndInsertsExactlyOneTraceRow() {
    // Arrange
    long pedidoId = crearPedidoPendiente();

    // Act
    RevisionPedidoResultado resultado =
        pedidoRevisionService.revisarPedido(pedidoId, "  Pedido validado con el cliente  ");

    // Assert
    assertTrue(consultarRevisado(pedidoId));
    assertEquals(1, contarRevisiones(pedidoId));
    assertEquals("Pedido validado con el cliente",
        jdbcTemplate.queryForObject(
            "SELECT observacion FROM pedido_revision WHERE pedido_id = ?", String.class, pedidoId));
    assertTrue(resultado.revisionId() > 0);
  }

  @Test
  @DisplayName("Duplicado: la segunda revisión se rechaza sin crear otra fila ni alterar la primera")
  void rejectSecondReviewWithoutCreatingAnotherRow() {
    // Arrange
    long pedidoId = crearPedidoPendiente();
    pedidoRevisionService.revisarPedido(pedidoId, "Primera revisión");

    // Act
    PedidoYaRevisadoException error = assertThrows(PedidoYaRevisadoException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoId, "Segunda revisión"));

    // Assert
    assertTrue(error.getMessage().contains("ya fue revisado"));
    assertEquals(1, contarRevisiones(pedidoId));
    assertEquals("Primera revisión", jdbcTemplate.queryForObject(
        "SELECT observacion FROM pedido_revision WHERE pedido_id = ?", String.class, pedidoId));
  }

  @Test
  @DisplayName("ID no registrado: respuesta controlada y ninguna escritura")
  void rejectUnknownOrderWithoutWrites() {
    // Arrange
    long idInexistente = 999_999L;

    // Act + Assert
    assertThrows(PedidoNoEncontradoException.class,
        () -> pedidoRevisionService.revisarPedido(idInexistente, "Observación válida"));
    assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedido_revision", Integer.class));
  }

  @Test
  @DisplayName("Rollback real (@Transactional): falla el INSERT de pedido_revision después del UPDATE y revisado vuelve a false")
  void rollbackOrderUpdateWhenTraceInsertFailsInsideDeclarativeTransaction() {
    // Arrange: pedido pendiente + restricción CHECK que hará fallar SOLO el INSERT de la traza
    long pedidoId = crearPedidoPendiente();
    agregarRestriccionQueHaceFallarElInsert();

    // Act: el UPDATE (escritura 1) se ejecuta; el INSERT (escritura 2) viola la restricción
    DataAccessException error = assertThrows(DataAccessException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoId, OBSERVACION_QUE_FALLA));

    // Assert: la causa es la restricción de pedido_revision y no quedan cambios parciales
    String causa = String.valueOf(error.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
    assertTrue(causa.contains(CHECK_TEMPORAL), "La falla debe provenir del INSERT en pedido_revision");
    assertFalse(consultarRevisado(pedidoId), "El UPDATE de pedido debió revertirse");
    assertEquals(0, contarRevisiones(pedidoId), "No debe existir una revisión confirmada");
  }

  @Test
  @DisplayName("Rollback programático (TransactionTemplate): el UPDATE es visible dentro de la transacción y se revierte al fallar el INSERT")
  void rollbackIsVisibleWithProgrammaticTransactionTemplate() {
    // Arrange
    long pedidoId = crearPedidoPendiente();
    agregarRestriccionQueHaceFallarElInsert();

    // Act
    assertThrows(DataAccessException.class, () -> transactionTemplate.executeWithoutResult(status -> {
      assertEquals(1, revisionRepository.marcarComoRevisado(pedidoId));
      assertTrue(revisionRepository.buscarEstadoRevision(pedidoId).orElseThrow(),
          "Dentro de la transacción el pedido ya figura como revisado");
      revisionRepository.insertarRevision(pedidoId, OBSERVACION_QUE_FALLA, LocalDateTime.now());
    }));

    // Assert
    assertFalse(consultarRevisado(pedidoId));
    assertEquals(0, contarRevisiones(pedidoId));
  }

  // ----------------------------------------------------------------- helpers

  private long crearPedidoPendiente() {
    Pedido pedido = new Pedido("Ana Torres", "LAP-001", "Laptop",
        new BigDecimal("2500.00"), 2, new BigDecimal("5000.00"));
    return pedidoRepository.save(pedido).getId();
  }

  private void agregarRestriccionQueHaceFallarElInsert() {
    jdbcTemplate.execute("ALTER TABLE pedido_revision ADD CONSTRAINT " + CHECK_TEMPORAL
        + " CHECK (observacion <> '" + OBSERVACION_QUE_FALLA + "')");
  }

  private boolean consultarRevisado(long pedidoId) {
    Boolean revisado = jdbcTemplate.queryForObject(
        "SELECT revisado FROM pedido WHERE id = ?", Boolean.class, pedidoId);
    return Boolean.TRUE.equals(revisado);
  }

  private int contarRevisiones(long pedidoId) {
    Integer total = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pedido_revision WHERE pedido_id = ?", Integer.class, pedidoId);
    return total != null ? total : 0;
  }

  private void limpiarTablas() {
    jdbcTemplate.update("DELETE FROM pedido_revision");
    jdbcTemplate.update("DELETE FROM pedido");
  }
}
