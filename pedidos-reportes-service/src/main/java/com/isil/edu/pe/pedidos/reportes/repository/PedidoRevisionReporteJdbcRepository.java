package com.isil.edu.pe.pedidos.reportes.repository;

import com.isil.edu.pe.pedidos.reportes.repository.projection.EstadisticaRevisionProjection;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevision;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PA4 - Consultas Spring JDBC (solo lectura) sobre {@code pedido} y {@code pedido_revision}.
 *
 * <p>El filtro por estado se envía como parámetro con nombre ({@code :revisado});
 * nunca se concatenan valores recibidos por HTTP.</p>
 */
@Repository
public class PedidoRevisionReporteJdbcRepository {

  private static final String SELECT_BASE = """
      SELECT p.id, p.cliente, p.producto_codigo, p.producto_nombre, p.cantidad, p.total, p.fecha,
             p.revisado, r.observacion, r.fecha_revision
      FROM pedido p
      LEFT JOIN pedido_revision r ON r.pedido_id = p.id
      """;

  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  public PedidoRevisionReporteJdbcRepository(JdbcTemplate jdbcTemplate,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
  }

  /** Todos los pedidos con su estado de revisión. */
  public List<PedidoRevision> listarTodos() {
    return jdbcTemplate.query(SELECT_BASE + " ORDER BY p.id DESC", this::mapear);
  }

  /**
   * Pedidos filtrados por estado de revisión.
   *
   * @param revisado {@code true} = REVISADO, {@code false} = PENDIENTE
   */
  public List<PedidoRevision> listarPorRevisado(boolean revisado) {
    String sql = SELECT_BASE + " WHERE p.revisado = :revisado ORDER BY p.id DESC";
    return namedParameterJdbcTemplate.query(sql, Map.of("revisado", revisado), this::mapear);
  }

  /** Conteos agregados para el resumen estadístico. */
  public EstadisticaRevisionProjection contarRevisiones() {
    String sql = """
        SELECT COUNT(*) AS total_pedidos,
               COALESCE(SUM(CASE WHEN revisado = TRUE THEN 1 ELSE 0 END), 0) AS total_revisados
        FROM pedido
        """;
    return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
        new EstadisticaRevisionProjection(rs.getLong("total_pedidos"), rs.getLong("total_revisados")));
  }

  private PedidoRevision mapear(ResultSet rs, int rowNum) throws SQLException {
    Timestamp fecha = rs.getTimestamp("fecha");
    Timestamp fechaRevision = rs.getTimestamp("fecha_revision");
    return new PedidoRevision(
        rs.getLong("id"),
        rs.getString("cliente"),
        rs.getString("producto_codigo"),
        rs.getString("producto_nombre"),
        rs.getInt("cantidad"),
        rs.getBigDecimal("total"),
        fecha != null ? fecha.toLocalDateTime() : null,
        rs.getBoolean("revisado"),
        rs.getString("observacion"),
        fechaRevision != null ? fechaRevision.toLocalDateTime() : null
    );
  }
}
