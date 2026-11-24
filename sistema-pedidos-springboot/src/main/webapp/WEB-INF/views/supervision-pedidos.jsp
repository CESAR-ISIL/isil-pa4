<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  PA4 - Vista de supervisión y revisión de pedidos.
  Solo presenta datos del Model (atributo "vista"); no contiene SQL ni lógica de negocio.
  Flujo de lectura : Controller -> SupervisionPedidosService -> PedidoReporteClient -> HTTP -> pedidos-reportes-service
  Flujo de escritura: POST .../{id}/revisar -> PedidoRevisionService -> Repository JDBC -> MySQL (PRG)
--%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Supervisión de Pedidos - ISIL</title>
    <c:url var="cssUrl" value="/assets/css/app.css"/>
    <link rel="stylesheet" href="${cssUrl}">
    <style>
        .estado { display:inline-block; padding:2px 10px; border-radius:999px; font-size:.8rem; font-weight:600; }
        .estado-PENDIENTE { background:#fff4d6; color:#8a5a00; }
        .estado-REVISADO  { background:#dff5e3; color:#1d6b33; }
        .form-revisar { display:flex; gap:6px; align-items:center; margin:0; }
        .form-revisar input[type=text] { min-width:200px; }
        .kpis { display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:12px; }
        .kpi { border:1px solid #e3e6ea; border-radius:10px; padding:12px; }
        .kpi strong { display:block; font-size:1.5rem; }
        .filtros a { margin-right:10px; }
        .alerta.aviso { background:#fff4d6; color:#6b4a00; border:1px solid #f0d58c; }
        .codigo { font-family:monospace; font-size:.8rem; opacity:.75; }
    </style>
</head>
<body>
<div class="contenedor">
    <header class="cabecera">
        <div>
            <p class="eyebrow">PA4 · Spring MVC + Spring JDBC + RestClient</p>
            <h1>Supervisión de pedidos</h1>
            <p class="subtitulo">
                Lectura desde pedidos-reportes-service (HTTP). La revisión se confirma en una transacción local:
                UPDATE pedido + INSERT pedido_revision.
            </p>
        </div>
        <div class="acciones-cabecera">
            <c:url var="pedidosUrl" value="/pedidos"/>
            <a href="${pedidosUrl}" class="boton-link boton-link-secundario">
                <span aria-hidden="true">←</span> Volver a pedidos
            </a>
            <c:url var="logoutUrl" value="/logout"/>
            <form method="post" action="${logoutUrl}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button type="submit">Cerrar sesión</button>
            </form>
        </div>
        <div class="flujo">
            Navegador → Spring Security → DispatcherServlet → SupervisionPedidoController →
            PedidoRevisionService → PedidoRevisionJdbcRepository → MySQL
        </div>
    </header>

    <%-- Resultado del POST (atributos flash del patrón Post/Redirect/Get) --%>
    <c:if test="${not empty mensaje}">
        <div class="alerta ${tipoMensaje == 'exito' ? 'exito' : 'error'}" role="status">
            <c:out value="${mensaje}"/>
            <span class="codigo">[<c:out value="${codigoResultado}"/>]</span>
        </div>
    </c:if>

    <c:if test="${not empty vista.aviso}">
        <div class="alerta aviso" role="alert"><c:out value="${vista.aviso}"/></div>
    </c:if>

    <section class="tarjeta">
        <h2>Resumen de revisiones</h2>
        <c:choose>
            <c:when test="${not empty vista.estadisticas}">
                <div class="kpis">
                    <div class="kpi">Total pedidos <strong><c:out value="${vista.estadisticas.totalPedidos}"/></strong></div>
                    <div class="kpi">Revisados <strong><c:out value="${vista.estadisticas.totalRevisados}"/></strong></div>
                    <div class="kpi">Pendientes <strong><c:out value="${vista.estadisticas.totalPendientes}"/></strong></div>
                    <div class="kpi">% revisado
                        <strong><fmt:formatNumber value="${vista.estadisticas.porcentajeRevisado}"
                                                  minFractionDigits="2" maxFractionDigits="2"/> %</strong>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <p class="sin-datos">Estadísticas no disponibles: pedidos-reportes-service no responde.</p>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="tarjeta">
        <div class="titulo-tabla-acciones">
            <h2>Pedidos</h2>
            <c:url var="supervisionUrl" value="/supervision/pedidos"/>
            <p class="filtros">
                Filtrar:
                <a href="${supervisionUrl}">Todos</a>
                <a href="${supervisionUrl}?estado=PENDIENTE">Pendientes</a>
                <a href="${supervisionUrl}?estado=REVISADO">Revisados</a>
                <c:if test="${not empty vista.estadoFiltro}">
                    · actual: <strong><c:out value="${vista.estadoFiltro}"/></strong>
                </c:if>
                · origen: <strong>${vista.reportesDisponible ? 'pedidos-reportes-service' : 'respaldo local (MySQL)'}</strong>
            </p>
        </div>

        <c:set var="esOperador" value="${pageContext.request.isUserInRole('OPERADOR')}"/>

        <div class="tabla-scroll">
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Cliente</th>
                    <th>Producto</th>
                    <th>Cant.</th>
                    <th>Total</th>
                    <th>Estado</th>
                    <th>Observación / fecha de revisión</th>
                    <th>Acción</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${vista.pedidos}">
                    <tr>
                        <td><c:out value="${item.id}"/></td>
                        <td><c:out value="${item.cliente}"/></td>
                        <td><c:out value="${item.productoNombre}"/></td>
                        <td><c:out value="${item.cantidad}"/></td>
                        <td>S/ <fmt:formatNumber value="${item.total}" minFractionDigits="2" maxFractionDigits="2"/></td>
                        <td><span class="estado estado-${item.estadoRevision}"><c:out value="${item.estadoRevision}"/></span></td>
                        <td>
                            <c:if test="${item.revisado}">
                                <c:out value="${item.observacion}"/><br>
                                <small><c:out value="${fn:replace(item.fechaRevision, 'T', ' ')}"/></small>
                            </c:if>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${item.revisable and esOperador}">
                                    <c:url var="revisarUrl" value="/supervision/pedidos/${item.id}/revisar"/>
                                    <form method="post" action="${revisarUrl}" class="form-revisar">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input type="text" name="observacion" maxlength="${observacionMax}"
                                               placeholder="Observación de la revisión"
                                               aria-label="Observación para el pedido ${item.id}">
                                        <button type="submit">Revisar</button>
                                    </form>
                                </c:when>
                                <c:when test="${item.revisable}">
                                    <small>Solo OPERADOR</small>
                                </c:when>
                                <c:otherwise>
                                    <small>—</small>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty vista.pedidos}">
                    <tr><td colspan="8" class="sin-datos">No hay pedidos para el filtro seleccionado.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>
</div>
</body>
</html>
