package com.isil.edu.pe.pedidos.reportes.controller;

import com.isil.edu.pe.pedidos.reportes.controller.dto.EstadisticaRevisionDto;
import com.isil.edu.pe.pedidos.reportes.controller.dto.PedidoRevisionDto;
import com.isil.edu.pe.pedidos.reportes.service.PedidoRevisionReporteService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PA4 - Recursos REST nuevos de pedidos-reportes-service.
 *
 * <ul>
 *   <li>{@code GET /api/v1/pedidos/revisiones?estado=PENDIENTE|REVISADO} -> 200 JSON | 400 ProblemDetail</li>
 *   <li>{@code GET /api/v1/pedidos/estadisticas/revisiones} -> 200 JSON</li>
 * </ul>
 * <p>Errores de base de datos -> 503 ProblemDetail (ver {@code ApiExceptionHandler}).
 * Sin SQL ni cálculos aquí: todo se delega en {@link PedidoRevisionReporteService}.</p>
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoRevisionReporteController {

  private final PedidoRevisionReporteService revisionService;

  public PedidoRevisionReporteController(PedidoRevisionReporteService revisionService) {
    this.revisionService = revisionService;
  }

  @GetMapping(value = "/revisiones", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<PedidoRevisionDto> listarRevisiones(
      @RequestParam(name = "estado", required = false) String estado) {
    return revisionService.listarRevisiones(estado)
        .stream()
        .map(PedidoRevisionDto::from)
        .toList();
  }

  @GetMapping(value = "/estadisticas/revisiones", produces = MediaType.APPLICATION_JSON_VALUE)
  public EstadisticaRevisionDto obtenerEstadisticas() {
    return EstadisticaRevisionDto.from(revisionService.obtenerEstadisticas());
  }
}
