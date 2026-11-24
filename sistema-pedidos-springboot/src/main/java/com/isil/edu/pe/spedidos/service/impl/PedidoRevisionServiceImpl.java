package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.exception.revision.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.revision.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.revision.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.repository.PedidoRevisionJdbcRepository;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.dto.PedidoSupervisionItem;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PA4 - Implementación transaccional de la revisión de pedidos.
 *
 * <h2>Límite transaccional</h2>
 * <pre>
 * revisarPedido()  &lt;-- @Transactional (BEGIN)
 *   1. validar entrada                  (sin escrituras)
 *   2. SELECT revisado FROM pedido      (existe / ya revisado)
 *   3. UPDATE pedido SET revisado=TRUE  (escritura 1)
 *   4. INSERT INTO pedido_revision      (escritura 2)
 * fin del método   &lt;-- COMMIT si no hubo excepción; ROLLBACK ante RuntimeException
 * </pre>
 *
 * <p>Frontera: dentro de este método NO se realizan llamadas HTTP a
 * inventario-service ni a pedidos-reportes-service. La transacción es local a MySQL
 * y la gestiona el {@code DataSourceTransactionManager}/{@code JpaTransactionManager}
 * configurado por Spring Boot, compartiendo la misma conexión con {@code JdbcTemplate}.</p>
 */
@Service
public class PedidoRevisionServiceImpl implements PedidoRevisionService {

  private static final Logger log = LoggerFactory.getLogger(PedidoRevisionServiceImpl.class);

  private final PedidoRevisionJdbcRepository revisionRepository;

  public PedidoRevisionServiceImpl(PedidoRevisionJdbcRepository revisionRepository) {
    this.revisionRepository = revisionRepository;
  }

  @Override
  @Transactional
  public RevisionPedidoResultado revisarPedido(long pedidoId, String observacion) {
    String observacionNormalizada = validarEntrada(pedidoId, observacion);

    boolean yaRevisado = revisionRepository.buscarEstadoRevision(pedidoId)
        .orElseThrow(() -> new PedidoNoEncontradoException(
            "No existe el pedido con ID " + pedidoId + "."));

    if (yaRevisado) {
      throw new PedidoYaRevisadoException(
          "El pedido #" + pedidoId + " ya fue revisado. No se registró una segunda revisión.");
    }

    // Escritura 1: UPDATE condicional (revisado = FALSE) -> protege ante concurrencia.
    int filas = revisionRepository.marcarComoRevisado(pedidoId);
    if (filas != 1) {
      throw new PedidoYaRevisadoException(
          "El pedido #" + pedidoId + " fue revisado por otra operación en paralelo.");
    }

    // Escritura 2: INSERT de trazabilidad. Si falla, se revierte también la escritura 1.
    LocalDateTime fechaRevision = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    long revisionId;
    try {
      revisionId = revisionRepository.insertarRevision(pedidoId, observacionNormalizada, fechaRevision);
    } catch (DuplicateKeyException e) {
      // uk_pedido_revision_pedido: ya existe una revisión confirmada para el pedido.
      throw new PedidoYaRevisadoException(
          "El pedido #" + pedidoId + " ya tiene una revisión registrada.", e);
    }

    log.info("Revisión confirmada: pedidoId={}, revisionId={}", pedidoId, revisionId);
    return new RevisionPedidoResultado(pedidoId, revisionId, observacionNormalizada, fechaRevision);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PedidoSupervisionItem> listarPedidosLocales() {
    return revisionRepository.listarPedidosConRevision();
  }

  private String validarEntrada(long pedidoId, String observacion) {
    if (pedidoId <= 0) {
      throw new RevisionInvalidaException("El ID del pedido debe ser mayor que cero.");
    }
    if (observacion == null || observacion.isBlank()) {
      throw new RevisionInvalidaException("La observación de la revisión es obligatoria.");
    }
    String normalizada = observacion.trim();
    if (normalizada.length() > OBSERVACION_MAX) {
      throw new RevisionInvalidaException(
          "La observación no puede superar " + OBSERVACION_MAX + " caracteres.");
    }
    return normalizada;
  }
}
