package com.isil.edu.pe.spedidos.client.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * PA4 - Cliente HTTP de los recursos nuevos: contrato JSON e indisponibilidad
 * (5xx, 4xx, conexión rechazada y timeout) traducidos a excepciones controladas.
 */
class PedidoReporteRestClientRevisionTest {

  private static final String BASE_URL = "http://localhost:8083/pedidos-reportes-service/api/v1/pedidos";

  private MockRestServiceServer server;
  private PedidoReporteRestClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new PedidoReporteRestClient(builder.build(), BASE_URL);
  }

  @Test
  @DisplayName("Deserializa el JSON de /revisiones en DTO de transporte")
  void mapRevisionesJsonToTransportDto() {
    // Arrange
    String json = """
        [{"id":7,"cliente":"Ana Torres","productoCodigo":"LAP-001","productoNombre":"Laptop",
          "cantidad":2,"total":5000.00,"fecha":"2026-11-20T09:00:00","revisado":true,
          "estadoRevision":"REVISADO","observacion":"Conforme","fechaRevision":"2026-11-21T10:30:00"},
         {"id":8,"cliente":"Luis","productoCodigo":"MOU-001","productoNombre":"Mouse",
          "cantidad":1,"total":50.00,"fecha":"2026-11-20T09:05:00","revisado":false,
          "estadoRevision":"PENDIENTE"}]
        """;
    server.expect(requestTo(BASE_URL + "/revisiones"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // Act
    List<PedidoRevisionResponse> respuesta = client.listarRevisiones(null);

    // Assert
    assertEquals(2, respuesta.size());
    assertTrue(respuesta.get(0).revisado());
    assertEquals("Conforme", respuesta.get(0).observacion());
    assertFalse(respuesta.get(1).revisado());
    assertNull(respuesta.get(1).observacion());
    server.verify();
  }

  @Test
  @DisplayName("Envía el filtro estado como parámetro de consulta")
  void sendEstadoAsQueryParameter() {
    server.expect(requestTo(BASE_URL + "/revisiones?estado=PENDIENTE"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    List<PedidoRevisionResponse> respuesta = client.listarRevisiones("PENDIENTE");

    assertTrue(respuesta.isEmpty());
    server.verify();
  }

  @Test
  @DisplayName("Deserializa /estadisticas/revisiones")
  void mapStatisticsJson() {
    server.expect(requestTo(BASE_URL + "/estadisticas/revisiones"))
        .andRespond(withSuccess(
            "{\"totalPedidos\":4,\"totalRevisados\":1,\"totalPendientes\":3,\"porcentajeRevisado\":25.00}",
            MediaType.APPLICATION_JSON));

    EstadisticaRevisionResponse estadisticas = client.obtenerEstadisticasRevision();

    assertEquals(4, estadisticas.totalPedidos());
    assertEquals(3, estadisticas.totalPendientes());
    assertEquals(0, new BigDecimal("25.00").compareTo(estadisticas.porcentajeRevisado()));
  }

  @Test
  @DisplayName("HTTP 500/503 del servicio remoto -> PedidoReporteNoDisponibleException con mensaje legible")
  void translateServerErrorsToControlledException() {
    server.expect(requestTo(BASE_URL + "/revisiones")).andRespond(withServerError());
    server.expect(requestTo(BASE_URL + "/estadisticas/revisiones"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    PedidoReporteNoDisponibleException e1 = assertThrows(PedidoReporteNoDisponibleException.class,
        () -> client.listarRevisiones(null));
    PedidoReporteNoDisponibleException e2 = assertThrows(PedidoReporteNoDisponibleException.class,
        () -> client.obtenerEstadisticasRevision());

    assertTrue(e1.getMessage().contains("no está disponible"));
    assertFalse(e1.getMessage().contains("Exception"), "No debe exponer trazas técnicas");
    assertTrue(e2.getMessage().contains("8083"));
  }

  @Test
  @DisplayName("HTTP 400 del servicio remoto -> PedidoReporteSolicitudInvalidaException")
  void translateClientErrorToInvalidRequestException() {
    server.expect(requestTo(BASE_URL + "/revisiones?estado=X")).andRespond(withBadRequest());

    PedidoReporteSolicitudInvalidaException error = assertThrows(
        PedidoReporteSolicitudInvalidaException.class, () -> client.listarRevisiones("X"));

    assertEquals(400, error.getStatus());
  }

  @Test
  @DisplayName("Puerto 8083 detenido (conexión rechazada) -> PedidoReporteNoDisponibleException")
  void translateConnectionRefusedToControlledException() throws IOException {
    // Arrange: se reserva un puerto libre y se cierra -> nadie escucha en él
    int puertoLibre;
    try (ServerSocket socket = new ServerSocket(0)) {
      puertoLibre = socket.getLocalPort();
    }
    PedidoReporteRestClient clienteSinServidor = new PedidoReporteRestClient(
        restClientConTimeouts(500, 500),
        "http://127.0.0.1:" + puertoLibre + "/pedidos-reportes-service/api/v1/pedidos");

    // Act + Assert
    assertThrows(PedidoReporteNoDisponibleException.class, () -> clienteSinServidor.listarRevisiones(null));
  }

  @Test
  @DisplayName("Servidor que no responde (read timeout) -> PedidoReporteNoDisponibleException")
  void translateReadTimeoutToControlledException() throws IOException {
    // Arrange: socket que acepta la conexión (backlog) pero nunca responde
    try (ServerSocket mudo = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      PedidoReporteRestClient clienteLento = new PedidoReporteRestClient(
          restClientConTimeouts(500, 300),
          "http://127.0.0.1:" + mudo.getLocalPort() + "/pedidos-reportes-service/api/v1/pedidos");

      // Act + Assert
      assertThrows(PedidoReporteNoDisponibleException.class,
          clienteLento::obtenerEstadisticasRevision);
    }
  }

  private static RestClient restClientConTimeouts(long connectMs, long readMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(connectMs));
    factory.setReadTimeout(Duration.ofMillis(readMs));
    return RestClient.builder().requestFactory(factory).build();
  }
}
