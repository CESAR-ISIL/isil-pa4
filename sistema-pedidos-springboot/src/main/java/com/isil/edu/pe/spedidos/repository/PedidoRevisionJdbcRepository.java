package com.isil.edu.pe.spedidos.repository;

import com.isil.edu.pe.spedidos.service.dto.PedidoSupervisionItem;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * PA4 - Repositorio Spring JDBC del módulo de revisión de pedidos.
 *
 * <p>Todas las sentencias usan parámetros posicionales ({@code ?}); ningún valor
 * ingresado por el usuario se concatena en el SQL, lo que evita inyección SQL.</p>
 *
 * <p>Este repositorio NO abre ni confirma transacciones: participa en la
 * transacción iniciada por {@code PedidoRevisionServiceImpl} (anotada con
 * {@code @Transactional}). Así, {@link #marcarComoRevisado(long)} y
 * {@link #insertarRevision(long, String, LocalDateTime)} se confirman o se revierten
 * juntas.</p>
 */
@Repository
public class PedidoRevisionJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  public PedidoRevisionJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Consulta el estado de revisión actual de un pedido.
   *
   * @param pedidoId identificador del pedido
   * @return {@code Optional.empty()} si el pedido no existe; en otro caso su valor de {@code revisado}
   */
  public Optional<Boolean> buscarEstadoRevision(long pedidoId) {
    String sql = "SELECT revisado FROM pedido WHERE id = ?";
    return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBoolean("revisado"), pedidoId)
        .stream()
        .findFirst();
  }

  /**
   * Escritura 1 de la transacción: marca el pedido como revisado.
   *
   * <p>La condición {@code AND revisado = FALSE} hace que la actualización sea
   * idempotente y segura ante concurrencia: si otro operador lo revisó primero,
   * se afectan 0 filas.</p>
   *
   * @param pedidoId identificador del pedido
   * @return filas afectadas (1 si se marcó; 0 si no existe o ya estaba revisado)
   */
  public int marcarComoRevisado(long pedidoId) {
    String sql = "UPDATE pedido SET revisado = TRUE WHERE id = ? AND revisado = FALSE";
    return jdbcTemplate.update(sql, pedidoId);
  }

  /**
   * Escritura 2 de la transacción: registra la trazabilidad de la revisión.
   *
   * @param pedidoId       pedido revisado
   * @param observacion    observación ingresada por el operador (ya validada)
   * @param fechaRevision  fecha y hora de la revisión
   * @return identificador generado para la fila de {@code pedido_revision}
   */
  public long insertarRevision(long pedidoId, String observacion, LocalDateTime fechaRevision) {
    String sql = "INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision) VALUES (?, ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
      ps.setLong(1, pedidoId);
      ps.setString(2, observacion);
      ps.setTimestamp(3, Timestamp.valueOf(fechaRevision));
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : 0L;
  }

  /**
   * Cuenta las revisiones registradas para un pedido (útil para verificaciones).
   *
   * @param pedidoId identificador del pedido
   * @return número de filas en {@code pedido_revision} para ese pedido
   */
  public int contarRevisiones(long pedidoId) {
    Integer total = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pedido_revision WHERE pedido_id = ?", Integer.class, pedidoId);
    return total != null ? total : 0;
  }

  /**
   * Lista local de pedidos con su estado de revisión. Se utiliza únicamente como
   * respaldo (fallback) de la vista de supervisión cuando
   * {@code pedidos-reportes-service} no está disponible.
   *
   * @return pedidos ordenados por id descendente
   */
  public List<PedidoSupervisionItem> listarPedidosConRevision() {
    String sql = """
        SELECT p.id, p.cliente, p.producto_nombre, p.cantidad, p.total, p.revisado,
               r.observacion, r.fecha_revision
        FROM pedido p
        LEFT JOIN pedido_revision r ON r.pedido_id = p.id
        ORDER BY p.id DESC
        """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> {
      Timestamp fechaRevision = rs.getTimestamp("fecha_revision");
      return new PedidoSupervisionItem(
          rs.getLong("id"),
          rs.getString("cliente"),
          rs.getString("producto_nombre"),
          rs.getInt("cantidad"),
          rs.getBigDecimal("total"),
          rs.getBoolean("revisado"),
          rs.getString("observacion"),
          fechaRevision != null ? fechaRevision.toLocalDateTime() : null
      );
    });
  }
}
