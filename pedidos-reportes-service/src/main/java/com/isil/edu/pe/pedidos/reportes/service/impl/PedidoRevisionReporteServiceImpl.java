package com.isil.edu.pe.pedidos.reportes.service.impl;

import com.isil.edu.pe.pedidos.reportes.repository.PedidoRevisionReporteJdbcRepository;
import com.isil.edu.pe.pedidos.reportes.repository.projection.EstadisticaRevisionProjection;
import com.isil.edu.pe.pedidos.reportes.service.PedidoRevisionReporteService;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadoRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PA4 - Lógica de lectura: valida el filtro y calcula las estadísticas.
 * El Controller solo transforma el resultado a DTO JSON.
 */
@Service
@Transactional(readOnly = true)
public class PedidoRevisionReporteServiceImpl implements PedidoRevisionReporteService {

  private static final BigDecimal CIEN = BigDecimal.valueOf(100);

  private final PedidoRevisionReporteJdbcRepository repository;

  public PedidoRevisionReporteServiceImpl(PedidoRevisionReporteJdbcRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PedidoRevision> listarRevisiones(String estado) {
    EstadoRevision filtro = EstadoRevision.desdeParametro(estado);
    if (filtro == null) {
      return repository.listarTodos();
    }
    return repository.listarPorRevisado(filtro.comoRevisado());
  }

  @Override
  public EstadisticaRevision obtenerEstadisticas() {
    EstadisticaRevisionProjection conteo = repository.contarRevisiones();
    long total = conteo.totalPedidos();
    long revisados = conteo.totalRevisados();
    long pendientes = total - revisados;

    BigDecimal porcentaje = total == 0
        ? BigDecimal.ZERO.setScale(2)
        : BigDecimal.valueOf(revisados).multiply(CIEN)
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

    return new EstadisticaRevision(total, revisados, pendientes, porcentaje);
  }
}
