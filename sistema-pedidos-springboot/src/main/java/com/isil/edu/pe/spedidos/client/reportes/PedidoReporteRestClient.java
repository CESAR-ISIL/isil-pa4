package com.isil.edu.pe.spedidos.client.reportes;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoReporteResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ProductoVendidoResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ResumenPedidosResponse;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PedidoReporteRestClient implements PedidoReporteClient {

  private final RestClient restClient;
  private final String baseUrl;

  public PedidoReporteRestClient(
      @Qualifier("pedidoReporteHttpClient") RestClient restClient,
      @Value("${pedido-reportes.api.base-url}") String baseUrl) {
    this.restClient = restClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public ResumenPedidosResponse obtenerResumen() {
    try {
      ResumenPedidosResponse response = restClient.get()
          .uri(baseUrl + "/estadisticas/resumen")
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(ResumenPedidosResponse.class);
      if (response == null) {
        throw new PedidoReporteNoDisponibleException("El servicio de reportes devolvió una respuesta vacía", null);
      }
      return response;
    } catch (RestClientResponseException | ResourceAccessException e) {
      throw noDisponible(e);
    }
  }

  @Override
  public List<PedidoReporteResponse> buscarPorCliente(String cliente) {
    if (cliente == null || cliente.isBlank()) {
      return List.of();
    }
    try {
      List<PedidoReporteResponse> response = restClient.get()
          .uri(baseUrl + "?cliente={cliente}", cliente.trim())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<PedidoReporteResponse>>() { });
      return response == null ? List.of() : response;
    } catch (RestClientResponseException | ResourceAccessException e) {
      throw noDisponible(e);
    }
  }

  @Override
  public List<ProductoVendidoResponse> listarProductosMasVendidos() {
    try {
      List<ProductoVendidoResponse> response = restClient.get()
          .uri(baseUrl + "/estadisticas/productos")
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<ProductoVendidoResponse>>() { });
      return response == null ? List.of() : response;
    } catch (RestClientResponseException | ResourceAccessException e) {
      throw noDisponible(e);
    }
  }

  // ===================== PA4 =====================

  @Override
  public List<PedidoRevisionResponse> listarRevisiones(String estado) {
    try {
      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + "/revisiones");
      if (estado != null && !estado.isBlank()) {
        // Valor enviado como parámetro de consulta codificado (no se concatena sin codificar).
        uriBuilder.queryParam("estado", estado.trim());
      }
      URI uri = uriBuilder.encode().build().toUri();

      List<PedidoRevisionResponse> response = restClient.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<PedidoRevisionResponse>>() { });
      return response == null ? List.of() : response;
    } catch (RestClientResponseException e) {
      throw traducirRespuestaError(e);
    } catch (RestClientException e) {
      // ResourceAccessException (timeout / conexión rechazada) y errores de lectura del cuerpo.
      throw noDisponible(e);
    }
  }

  @Override
  public EstadisticaRevisionResponse obtenerEstadisticasRevision() {
    try {
      EstadisticaRevisionResponse response = restClient.get()
          .uri(baseUrl + "/estadisticas/revisiones")
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(EstadisticaRevisionResponse.class);
      if (response == null) {
        throw new PedidoReporteNoDisponibleException(
            "El servicio de reportes devolvió una respuesta vacía", null);
      }
      return response;
    } catch (RestClientResponseException e) {
      throw traducirRespuestaError(e);
    } catch (RestClientException e) {
      throw noDisponible(e);
    }
  }

  /**
   * 4xx -> solicitud inválida (error del cliente); 5xx -> servicio no disponible.
   * El mensaje para el usuario nunca incluye trazas técnicas.
   */
  private RuntimeException traducirRespuestaError(RestClientResponseException e) {
    int status = e.getStatusCode().value();
    if (status >= 400 && status < 500) {
      return new PedidoReporteSolicitudInvalidaException(status,
          "El servicio de reportes rechazó la consulta (HTTP " + status + ").", e);
    }
    return noDisponible(e);
  }

  private PedidoReporteNoDisponibleException noDisponible(Exception e) {
    return new PedidoReporteNoDisponibleException(
        "pedidos-reportes-service no está disponible. Verifique que se ejecute en el puerto 8083.", e);
  }
}
