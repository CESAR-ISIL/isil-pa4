package com.isil.edu.pe.spedidos.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PA4 - Fila que muestra la vista de supervisión.
 *
 * <p>Es un modelo de presentación propio de sistema-pedidos: se construye a partir
 * del DTO HTTP de pedidos-reportes-service o, como respaldo, desde la consulta JDBC
 * local. La JSP no conoce el origen de los datos.</p>
 */
public record PedidoSupervisionItem(
    Long id,
    String cliente,
    String productoNombre,
    int cantidad,
    BigDecimal total,
    boolean revisado,
    String observacion,
    LocalDateTime fechaRevision
) {

  /*
   * Propiedades calculadas para la JSP.
   * Tomcat 11 (Jakarta EL 6) resuelve los records con RecordELResolver, que busca un
   * método con el MISMO nombre de la propiedad (estadoRevision(), revisable()) y NO
   * los getters JavaBean. Por eso se declaran ambas formas.
   */

  /** Etiqueta legible del estado (PENDIENTE / REVISADO). */
  public String estadoRevision() {
    return revisado ? "REVISADO" : "PENDIENTE";
  }

  /** La acción "Revisar" solo está disponible para pedidos pendientes. */
  public boolean revisable() {
    return !revisado;
  }

  public String getEstadoRevision() {
    return estadoRevision();
  }

  public boolean isRevisable() {
    return revisable();
  }

  // Getters estilo JavaBean para que JSP EL pueda leer los componentes del record.
  public Long getId() { return id; }
  public String getCliente() { return cliente; }
  public String getProductoNombre() { return productoNombre; }
  public int getCantidad() { return cantidad; }
  public BigDecimal getTotal() { return total; }
  public boolean isRevisado() { return revisado; }
  public String getObservacion() { return observacion; }
  public LocalDateTime getFechaRevision() { return fechaRevision; }
}