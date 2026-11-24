package com.isil.edu.pe.spedidos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * PA4 - Mapeo de la tabla {@code pedido_revision} (trazabilidad de revisiones).
 *
 * <p><strong>Importante:</strong> esta clase existe solo para que Hibernate genere
 * el esquema ({@code ddl-auto=update} en MySQL y {@code create-drop} en H2) con
 * sus restricciones:</p>
 * <ul>
 *   <li>{@code fk_pedido_revision_pedido}: integridad referencial hacia {@code pedido(id)}.</li>
 *   <li>{@code uk_pedido_revision_pedido}: impide más de una revisión confirmada por pedido.</li>
 * </ul>
 *
 * <p>Las lecturas y escrituras del módulo de revisión NO usan JPA: se realizan con
 * Spring JDBC en {@code PedidoRevisionJdbcRepository}. Esta entidad tampoco se comparte
 * con {@code pedidos-reportes-service}; ese servicio usa sus propios DTO.</p>
 *
 * <p>El script equivalente para MySQL está en {@code db/pa4-revision-schema.sql}.</p>
 */
@Entity
@Table(
    name = "pedido_revision",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pedido_revision_pedido",
        columnNames = "pedido_id"
    )
)
public class PedidoRevision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "pedido_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_pedido_revision_pedido")
  )
  private Pedido pedido;

  @Column(nullable = false, length = 500)
  private String observacion;

  @Column(name = "fecha_revision", nullable = false)
  private LocalDateTime fechaRevision;

  protected PedidoRevision() {
  }

  public Long getId() {
    return id;
  }

  public Pedido getPedido() {
    return pedido;
  }

  public String getObservacion() {
    return observacion;
  }

  public LocalDateTime getFechaRevision() {
    return fechaRevision;
  }
}
