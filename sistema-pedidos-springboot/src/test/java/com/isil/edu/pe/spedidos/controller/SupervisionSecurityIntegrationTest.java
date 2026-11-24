package com.isil.edu.pe.spedidos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * PA4 - Integración MVC completa: Spring Security (roles + CSRF) -> DispatcherServlet ->
 * SupervisionPedidoController -> PedidoRevisionService -> Repository JDBC -> H2.
 * El cliente HTTP hacia pedidos-reportes-service se reemplaza con Mockito.
 */
@SpringBootTest
@ActiveProfiles("test")
class SupervisionSecurityIntegrationTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private PedidoRepository pedidoRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoBean
  private PedidoReporteClient pedidoReporteClient;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    limpiar();
  }

  @AfterEach
  void tearDown() {
    limpiar();
  }

  @Test
  @DisplayName("OPERADOR con CSRF revisa el pedido: 302 PRG, revisado=true y una fila de traza")
  void operatorWithCsrfReviewsOrderEndToEnd() throws Exception {
    long pedidoId = crearPedido();

    mockMvc.perform(post("/supervision/pedidos/{id}/revisar", pedidoId)
            .with(user("operador").roles("OPERADOR"))
            .with(csrf())
            .param("observacion", "Conforme"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/supervision/pedidos"))
        .andExpect(flash().attribute("codigoResultado", "REVISADO"));

    assertTrue(consultarRevisado(pedidoId));
    assertEquals(1, contarRevisiones(pedidoId));
  }

  @Test
  @DisplayName("CONSULTA no puede revisar (403) y la base no cambia")
  void consultaRoleCannotReview() throws Exception {
    long pedidoId = crearPedido();

    mockMvc.perform(post("/supervision/pedidos/{id}/revisar", pedidoId)
            .with(user("consulta").roles("CONSULTA"))
            .with(csrf())
            .param("observacion", "Intento"))
        .andExpect(status().isForbidden());

    assertFalse(consultarRevisado(pedidoId));
    assertEquals(0, contarRevisiones(pedidoId));
  }

  @Test
  @DisplayName("OPERADOR sin token CSRF es rechazado (403): CSRF sigue activo")
  void rejectReviewWithoutCsrfToken() throws Exception {
    long pedidoId = crearPedido();

    mockMvc.perform(post("/supervision/pedidos/{id}/revisar", pedidoId)
            .with(user("operador").roles("OPERADOR"))
            .param("observacion", "Sin token"))
        .andExpect(status().isForbidden());

    assertFalse(consultarRevisado(pedidoId));
  }

  @Test
  @DisplayName("Con pedidos-reportes-service caído la vista responde 200 con respaldo local")
  void renderSupervisionWithLocalFallbackWhenReportServiceIsDown() throws Exception {
    crearPedido();
    when(pedidoReporteClient.listarRevisiones(any()))
        .thenThrow(new PedidoReporteNoDisponibleException("no disponible", null));

    mockMvc.perform(get("/supervision/pedidos").with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(view().name("supervision-pedidos"))
        .andExpect(model().attributeExists("vista"));
  }

  private long crearPedido() {
    return pedidoRepository.save(new Pedido("Ana Torres", "LAP-001", "Laptop",
        new BigDecimal("2500.00"), 1, new BigDecimal("2500.00"))).getId();
  }

  private boolean consultarRevisado(long pedidoId) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "SELECT revisado FROM pedido WHERE id = ?", Boolean.class, pedidoId));
  }

  private int contarRevisiones(long pedidoId) {
    Integer n = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pedido_revision WHERE pedido_id = ?", Integer.class, pedidoId);
    return n != null ? n : 0;
  }

  private void limpiar() {
    jdbcTemplate.update("DELETE FROM pedido_revision");
    jdbcTemplate.update("DELETE FROM pedido");
  }
}
