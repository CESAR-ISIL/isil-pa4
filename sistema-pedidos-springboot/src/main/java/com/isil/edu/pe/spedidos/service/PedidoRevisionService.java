package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.exception.revision.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.revision.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.revision.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.dto.PedidoSupervisionItem;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.util.List;

/**
 * PA4 - Caso de uso "revisión de pedido".
 *
 * <pre>
 * SupervisionPedidoController -> PedidoRevisionService -> PedidoRevisionJdbcRepository -> MySQL
 * </pre>
 */
public interface PedidoRevisionService {

  /** Longitud máxima de la observación (coincide con VARCHAR(500) de pedido_revision). */
  int OBSERVACION_MAX = 500;

  /**
   * Marca un pedido como revisado y registra su trazabilidad en una sola
   * transacción local (UPDATE pedido + INSERT pedido_revision).
   *
   * @throws RevisionInvalidaException   si {@code pedidoId <= 0} o la observación está vacía / excede 500 caracteres
   * @throws PedidoNoEncontradoException si el pedido no existe
   * @throws PedidoYaRevisadoException   si el pedido ya tiene una revisión confirmada
   */
  RevisionPedidoResultado revisarPedido(long pedidoId, String observacion);

  /**
   * Lista local de pedidos con su estado de revisión (respaldo cuando
   * pedidos-reportes-service no está disponible).
   */
  List<PedidoSupervisionItem> listarPedidosLocales();
}
