package com.isil.edu.pe.pedidos.reportes.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * PA4 - Integración Controller -> Service -> Repository JDBC -> H2 de los recursos nuevos.
 */
@SpringBootTest
class PedidoRevisionReporteIntegrationTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    jdbcTemplate.update("DELETE FROM pedido_revision");
    jdbcTemplate.update("DELETE FROM pedido");

    long ana = insertarPedido("Ana Torres", "LAP-001", "Laptop", 2, "5000.00", true);
    long carlos = insertarPedido("Carlos Perez", "MOU-001", "Mouse", 3, "150.00", true);
    insertarPedido("Lucia Diaz", "TEC-001", "Teclado", 1, "80.00", false);
    insertarRevision(ana, "Conforme");
    insertarRevision(carlos, "Dirección verificada");
  }

  @Test
  @DisplayName("Filtra REVISADO desde H2 e incluye observación")
  void returnOnlyReviewedOrdersFromDatabase() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "REVISADO"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].revisado").value(true))
        .andExpect(jsonPath("$[0].observacion").exists())
        .andExpect(jsonPath("$[1].observacion").exists());
  }

  @Test
  @DisplayName("Filtra PENDIENTE desde H2 sin observación")
  void returnOnlyPendingOrdersFromDatabase() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "PENDIENTE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].cliente").value("Lucia Diaz"))
        .andExpect(jsonPath("$[0].observacion").doesNotExist());
  }

  @Test
  @DisplayName("Estado inválido responde 400 en la aplicación completa")
  void returnBadRequestForInvalidStateInFullContext() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "X"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Estadísticas calculadas sobre H2: 3 pedidos, 2 revisados, 66.67 %")
  void returnStatisticsComputedFromDatabase() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/revisiones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPedidos").value(3))
        .andExpect(jsonPath("$.totalRevisados").value(2))
        .andExpect(jsonPath("$.totalPendientes").value(1))
        .andExpect(jsonPath("$.porcentajeRevisado").value(66.67));
  }

  private long insertarPedido(String cliente, String codigo, String nombre, int cantidad,
      String total, boolean revisado) {
    jdbcTemplate.update("""
        INSERT INTO pedido (cliente, producto_codigo, producto_nombre, precio_unitario, cantidad,
                            total, fecha, revisado)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, cliente, codigo, nombre, new java.math.BigDecimal(total), cantidad,
        new java.math.BigDecimal(total), Timestamp.valueOf(LocalDateTime.now()), revisado);
    Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM pedido", Long.class);
    return id != null ? id : 0L;
  }

  private void insertarRevision(long pedidoId, String observacion) {
    jdbcTemplate.update(
        "INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision) VALUES (?, ?, ?)",
        pedidoId, observacion, Timestamp.valueOf(LocalDateTime.now()));
  }
}
