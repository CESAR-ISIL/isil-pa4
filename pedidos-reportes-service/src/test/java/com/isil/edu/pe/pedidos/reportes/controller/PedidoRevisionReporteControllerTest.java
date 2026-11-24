package com.isil.edu.pe.pedidos.reportes.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.isil.edu.pe.pedidos.reportes.exception.ApiExceptionHandler;
import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import com.isil.edu.pe.pedidos.reportes.service.PedidoRevisionReporteService;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevision;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * PA4 - Contrato REST (JSON + códigos HTTP) de los recursos nuevos, aislando el Service con Mockito.
 */
class PedidoRevisionReporteControllerTest {

  private PedidoRevisionReporteService revisionService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    revisionService = mock(PedidoRevisionReporteService.class);
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PedidoRevisionReporteController(revisionService))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("GET /revisiones?estado=REVISADO devuelve 200 con observación y fecha de revisión")
  void returnReviewedOrdersWithObservationWhenEstadoIsRevisado() throws Exception {
    // Arrange
    PedidoRevision revisado = new PedidoRevision(10L, "Ana Torres", "LAP-001", "Laptop", 2,
        new BigDecimal("5000.00"), LocalDateTime.of(2026, 11, 20, 9, 0), true,
        "Validado con el cliente", LocalDateTime.of(2026, 11, 21, 10, 30));
    when(revisionService.listarRevisiones("REVISADO")).thenReturn(List.of(revisado));

    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "REVISADO"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].revisado").value(true))
        .andExpect(jsonPath("$[0].estadoRevision").value("REVISADO"))
        .andExpect(jsonPath("$[0].observacion").value("Validado con el cliente"))
        .andExpect(jsonPath("$[0].fechaRevision").exists());
    verify(revisionService).listarRevisiones("REVISADO");
  }

  @Test
  @DisplayName("GET /revisiones?estado=PENDIENTE no incluye observación ni fecha de revisión")
  void omitObservationFieldsWhenOrderIsPending() throws Exception {
    // Arrange
    PedidoRevision pendiente = new PedidoRevision(11L, "Carlos Perez", "MOU-001", "Mouse", 3,
        new BigDecimal("150.00"), LocalDateTime.of(2026, 11, 20, 9, 0), false, null, null);
    when(revisionService.listarRevisiones("PENDIENTE")).thenReturn(List.of(pendiente));

    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "PENDIENTE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].estadoRevision").value("PENDIENTE"))
        .andExpect(jsonPath("$[0].observacion").doesNotExist())
        .andExpect(jsonPath("$[0].fechaRevision").doesNotExist());
  }

  @Test
  @DisplayName("GET /revisiones con estado inválido responde 400 ProblemDetail")
  void returnBadRequestWhenEstadoIsInvalid() throws Exception {
    // Arrange
    when(revisionService.listarRevisiones("ANULADO"))
        .thenThrow(new EstadoRevisionInvalidoException("ANULADO"));

    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "ANULADO"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Parámetro inválido"));
  }

  @Test
  @DisplayName("GET /estadisticas/revisiones devuelve el DTO calculado por el Service")
  void returnStatisticsDtoFromService() throws Exception {
    // Arrange
    when(revisionService.obtenerEstadisticas())
        .thenReturn(new EstadisticaRevision(4, 1, 3, new BigDecimal("25.00")));

    // Act + Assert
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/revisiones"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPedidos").value(4))
        .andExpect(jsonPath("$.totalRevisados").value(1))
        .andExpect(jsonPath("$.totalPendientes").value(3))
        .andExpect(jsonPath("$.porcentajeRevisado").value(25.00));
  }
}
