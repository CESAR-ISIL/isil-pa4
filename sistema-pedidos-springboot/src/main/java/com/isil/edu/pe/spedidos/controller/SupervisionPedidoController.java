package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.exception.revision.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.revision.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.revision.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.SupervisionPedidosService;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * PA4 - Controller MVC de supervisión y revisión de pedidos.
 *
 * <p>Responsabilidades: recibir parámetros HTTP, delegar en la capa Service y
 * construir el {@link Model}. No ejecuta SQL ni accede a datos directamente.</p>
 *
 * <pre>
 * GET  /supervision/pedidos                -> SupervisionPedidosService -> PedidoReporteClient -> HTTP 8083
 * POST /supervision/pedidos/{id}/revisar   -> PedidoRevisionService -> PedidoRevisionJdbcRepository -> MySQL
 *      (Post/Redirect/Get: siempre responde 302 hacia GET /supervision/pedidos)
 * </pre>
 *
 * <p>El resultado del POST se comunica con atributos flash:
 * {@code tipoMensaje} (exito | error), {@code codigoResultado}
 * (REVISADO | DATOS_INVALIDOS | NO_ENCONTRADO | YA_REVISADO_409 | ERROR_BD) y {@code mensaje}.</p>
 */
@Controller
@RequestMapping("/supervision/pedidos")
public class SupervisionPedidoController {

  private static final Logger log = LoggerFactory.getLogger(SupervisionPedidoController.class);
  private static final String REDIRECT_SUPERVISION = "redirect:/supervision/pedidos";

  private final SupervisionPedidosService supervisionPedidosService;
  private final PedidoRevisionService pedidoRevisionService;

  public SupervisionPedidoController(SupervisionPedidosService supervisionPedidosService,
      PedidoRevisionService pedidoRevisionService) {
    this.supervisionPedidosService = supervisionPedidosService;
    this.pedidoRevisionService = pedidoRevisionService;
  }

  @GetMapping
  public String verSupervision(@RequestParam(name = "estado", required = false) String estado,
      Model model) {
    model.addAttribute("vista", supervisionPedidosService.obtenerVista(estado));
    model.addAttribute("observacionMax", PedidoRevisionService.OBSERVACION_MAX);
    return "supervision-pedidos";
  }

  @PostMapping("/{id}/revisar")
  public String revisar(@PathVariable("id") String id,
      @RequestParam(name = "observacion", required = false) String observacion,
      RedirectAttributes redirectAttributes) {

    long pedidoId;
    try {
      pedidoId = Long.parseLong(id.trim());
    } catch (NumberFormatException e) {
      return resultado(redirectAttributes, "error", "DATOS_INVALIDOS",
          "El identificador de pedido '" + id + "' no es válido.");
    }

    try {
      RevisionPedidoResultado r = pedidoRevisionService.revisarPedido(pedidoId, observacion);
      return resultado(redirectAttributes, "exito", "REVISADO",
          "Pedido #" + r.pedidoId() + " revisado correctamente (revisión #" + r.revisionId() + ").");
    } catch (RevisionInvalidaException e) {
      return resultado(redirectAttributes, "error", "DATOS_INVALIDOS", e.getMessage());
    } catch (PedidoNoEncontradoException e) {
      return resultado(redirectAttributes, "error", "NO_ENCONTRADO", e.getMessage());
    } catch (PedidoYaRevisadoException e) {
      return resultado(redirectAttributes, "error", "YA_REVISADO_409", e.getMessage());
    } catch (DataAccessException e) {
      log.error("Error de base de datos al revisar el pedido {}", pedidoId, e);
      return resultado(redirectAttributes, "error", "ERROR_BD",
          "No se pudo registrar la revisión. La operación fue revertida; intente nuevamente.");
    }
  }

  private String resultado(RedirectAttributes redirectAttributes, String tipo, String codigo,
      String mensaje) {
    redirectAttributes.addFlashAttribute("tipoMensaje", tipo);
    redirectAttributes.addFlashAttribute("codigoResultado", codigo);
    redirectAttributes.addFlashAttribute("mensaje", mensaje);
    return REDIRECT_SUPERVISION;
  }
}
