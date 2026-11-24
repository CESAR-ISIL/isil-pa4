package com.isil.edu.pe.spedidos.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.isil.edu.pe.spedidos.exception.revision.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.revision.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.revision.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.SupervisionPedidosService;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import com.isil.edu.pe.spedidos.service.dto.SupervisionPedidosVista;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * PA4 - Controller MVC aislado con Mockito: delega en Service, construye el Model
 * y aplica Post/Redirect/Get con mensajes flash.
 */
class SupervisionPedidoControllerTest {

  private SupervisionPedidosService supervisionService;
  private PedidoRevisionService revisionService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    supervisionService = mock(SupervisionPedidosService.class);
    revisionService = mock(PedidoRevisionService.class);
    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/WEB-INF/views/", ".jsp");
    mockMvc = MockMvcBuilders
        .standaloneSetup(new SupervisionPedidoController(supervisionService, revisionService))
        .setViewResolvers(viewResolver)
        .build();
  }

  @Test
  @DisplayName("GET /supervision/pedidos construye el Model con la vista del Service")
  void buildModelFromServiceOnGet() throws Exception {
    SupervisionPedidosVista vista = new SupervisionPedidosVista(List.of(), null, "PENDIENTE", true, null);
    when(supervisionService.obtenerVista("PENDIENTE")).thenReturn(vista);

    mockMvc.perform(get("/supervision/pedidos").param("estado", "PENDIENTE"))
        .andExpect(status().isOk())
        .andExpect(view().name("supervision-pedidos"))
        .andExpect(model().attribute("vista", vista))
        .andExpect(model().attributeExists("observacionMax"));
    verify(supervisionService).obtenerVista("PENDIENTE");
  }

  @Test
  @DisplayName("POST válido: delega en PedidoRevisionService y redirige (PRG) con mensaje de éxito")
  void delegateToServiceAndRedirectOnValidReview() throws Exception {
    when(revisionService.revisarPedido(5L, "Conforme"))
        .thenReturn(new RevisionPedidoResultado(5L, 1L, "Conforme", LocalDateTime.now()));

    mockMvc.perform(post("/supervision/pedidos/5/revisar").param("observacion", "Conforme"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/supervision/pedidos"))
        .andExpect(flash().attribute("tipoMensaje", "exito"))
        .andExpect(flash().attribute("codigoResultado", "REVISADO"));
    verify(revisionService).revisarPedido(5L, "Conforme");
  }

  @Test
  @DisplayName("POST duplicado: mensaje controlado equivalente a 409 y PRG")
  void showConflictMessageWhenOrderAlreadyReviewed() throws Exception {
    when(revisionService.revisarPedido(5L, "Otra"))
        .thenThrow(new PedidoYaRevisadoException("El pedido #5 ya fue revisado."));

    mockMvc.perform(post("/supervision/pedidos/5/revisar").param("observacion", "Otra"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/supervision/pedidos"))
        .andExpect(flash().attribute("tipoMensaje", "error"))
        .andExpect(flash().attribute("codigoResultado", "YA_REVISADO_409"))
        .andExpect(flash().attribute("mensaje", "El pedido #5 ya fue revisado."));
  }

  @Test
  @DisplayName("POST con ID no numérico: no invoca el Service")
  void rejectNonNumericIdWithoutCallingService() throws Exception {
    mockMvc.perform(post("/supervision/pedidos/abc/revisar").param("observacion", "Ok"))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("codigoResultado", "DATOS_INVALIDOS"));
    verifyNoInteractions(revisionService);
  }

  @Test
  @DisplayName("POST con observación vacía o ID inexistente: mensajes comprensibles")
  void showReadableMessagesForValidationAndUnknownId() throws Exception {
    when(revisionService.revisarPedido(5L, ""))
        .thenThrow(new RevisionInvalidaException("La observación de la revisión es obligatoria."));
    when(revisionService.revisarPedido(999L, "Ok"))
        .thenThrow(new PedidoNoEncontradoException("No existe el pedido con ID 999."));

    mockMvc.perform(post("/supervision/pedidos/5/revisar").param("observacion", ""))
        .andExpect(flash().attribute("codigoResultado", "DATOS_INVALIDOS"))
        .andExpect(flash().attribute("mensaje", "La observación de la revisión es obligatoria."));
    mockMvc.perform(post("/supervision/pedidos/999/revisar").param("observacion", "Ok"))
        .andExpect(flash().attribute("codigoResultado", "NO_ENCONTRADO"));
  }
}
