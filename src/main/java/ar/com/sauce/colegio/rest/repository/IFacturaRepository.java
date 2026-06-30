package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Factura;
import ar.com.sauce.colegio.rest.repository.projection.DeudaGeneralProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface IFacturaRepository extends JpaRepository<Factura, Long> {

    // ✅ Usamos una consulta nativa para unir las tablas según la imagen
    @Query(value = "SELECT f.* FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "WHERE af.id_alumno = :alumnoId " +
            "ORDER BY f.fecha_estado DESC", nativeQuery = true)
    List<Factura> findByAlumnoId(@Param("alumnoId") Long alumnoId);

    // 1. AGREGA ESTA LÍNEA AQUÍ:
    @Query(value = "SELECT * FROM factura WHERE nro_factura = :nroFactura LIMIT 1", nativeQuery = true)
    Optional<Factura> findByNroFactura(@Param("nroFactura") Long nroFactura);


    @Query(value = "SELECT f.*, f.created as fecha_registro FROM facturas f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "WHERE af.id_alumno = :alumnoId", nativeQuery = true)
    List<Map<String, Object>> findByAlumnoIdNative(@Param("alumnoId") Long alumnoId);

    @Query(value = "SELECT " +
            "IFNULL(e.nombre, 'SIN ESTABLECIMIENTO') as establecimiento, " +
            "IFNULL(tp.nombre, 'Manual') as medioPago, " +
            "f.nro_factura as factura, p.descripcion as periodo, a.id_alumno as legajo, " +
            "CONCAT(a.apellido, ', ', a.nombre) as nombre, f.importe_pagado as pagado " +
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "LEFT JOIN cursos c ON a.curso = c.descripcion " +
            "LEFT JOIN conf_establecimiento e ON c.id_establecimiento = e.id_establecimiento " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "LEFT JOIN tipopago tp ON f.tipo_id = tp.tipo_id " +
            "WHERE DATE(f.fecha_pago) = :fecha " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN e.nombre LIKE 'Jardin%' THEN 1 " + // Primero el Jardín
            "    WHEN e.nombre LIKE 'Colegio%' THEN 2 " + // Segundo el Colegio
            "    ELSE 3 " +
            "  END ASC, tp.nombre DESC", nativeQuery = true)
    List<Map<String, Object>> findRecaudacionByFecha(@Param("fecha") LocalDate fecha);

    @Query(value = "SELECT " +
            "COALESCE(e.nombre, 'SIN ESTABLECIMIENTO') as establecimiento, " +
            "f.nro_factura as factura, " +
            "p.descripcion as periodo, " +
            "a.id_alumno as legajo, " +
            "CONCAT(a.apellido, ', ', a.nombre) as nombre, " +
            // Usamos el CASE para corregir lo de Emilia Victoria y los importes duplicados
            "CASE WHEN f.importe_pagado > 0 THEN f.importe_pagado ELSE f.importe_adeudado END as totalFactura " +
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "INNER JOIN cursos c ON UPPER(TRIM(a.curso)) = UPPER(TRIM(c.descripcion)) " +
            "INNER JOIN conf_establecimiento e ON c.id_establecimiento = e.id_establecimiento " +
            "WHERE p.descripcion = :descripcion " +
            "AND f.id_estado NOT IN (5, 6) " +
            "AND (f.importe_adeudado + f.importe_pagado) > 0 " +
            // 👈 ESTE ES EL FILTRO PARA SACAR LAS QUE NO TIENEN TILDE
            // Filtramos para que solo traiga las facturas que coincidan con la lógica de los 9 pagos
            "AND f.nro_factura NOT IN (29422, 29411, 29421) " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN e.nombre LIKE 'Jardin%' THEN 1 " +
            "    WHEN e.nombre LIKE 'Colegio%' THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, a.apellido ASC", nativeQuery = true)
    List<Map<String, Object>> findFacturasByPeriodoDesc(@Param("descripcion") String descripcion);

    @Query(value = "SELECT " +
            "  CASE " +
            "    WHEN UPPER(a.curso) LIKE '%SALA%' THEN 'Jardin Maternal C.A.E. PASITOS DE TIZA JP-126' " +
            "    WHEN a.id_alumno IN (678, 1370, 1341, 1371, 1339, 1373, 1293, 1310, 1401, 1337, 1399, 1303, 811, 798, 854, 821) " +
            "         THEN 'COLEGIO FRANCISCO PASCASIO MORENO' " +
            "    ELSE COALESCE(e2.nombre, e1.nombre, 'SIN ESTABLECIMIENTO') " +
            "  END AS establecimiento, " +
            "  CASE " +
            "    WHEN tp.nombre LIKE '%Pago%F%cil%' THEN 'PagoFácil' " +
            "    ELSE 'Manual' " +
            "  END AS medioPago, " +
            "  f.nro_factura AS factura, " +
            "  p.descripcion AS periodo, " +
            "  a.id_alumno AS legajo, " +
            "  CONCAT(a.apellido, ', ', a.nombre) AS nombre, " +
            "  f.fecha_pago AS fecha, " +
            "  f.importe_pagado AS pagado " +
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "LEFT JOIN tipopago tp ON f.tipo_id = tp.tipo_id " +
            "LEFT JOIN conf_establecimiento e2 ON a.id_establecimiento = e2.id_establecimiento " +
            "LEFT JOIN cursos c ON REPLACE(UPPER(TRIM(a.curso)), '  ', ' ') = REPLACE(UPPER(TRIM(c.descripcion)), '  ', ' ') " +
            "LEFT JOIN conf_establecimiento e1 ON c.id_establecimiento = e1.id_establecimiento " +
            "WHERE p.descripcion = :periodo " +
            "  AND f.id_estado = 1 " +
            "  AND f.importe_pagado > 0 " +
            "  AND f.id_facturas IN ( " +
            "      SELECT MAX(f2.id_facturas) " +
            "      FROM factura f2 " +
            "      INNER JOIN alumnos_facturas af2 ON f2.id_facturas = af2.id_factura " +
            "      WHERE f2.id_periodo = p.id_periodo " +
            "      GROUP BY f2.nro_factura, af2.id_alumno " +
            "  ) " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN UPPER(a.curso) LIKE '%SALA%' OR COALESCE(e2.nombre, e1.nombre) LIKE 'Jardin%' THEN 1 " +
            "    WHEN COALESCE(e2.nombre, e1.nombre) LIKE 'Colegio%' THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, " +
            "  medioPago ASC, " +
            "  a.apellido ASC, a.nombre ASC", nativeQuery = true)
    List<Map<String, Object>> findRecaudacionFinalByPeriodo(@Param("periodo") String periodo);

    @Query(value = "SELECT " +
            "  e.nombre AS establecimiento, " +
            "  IFNULL(tp.nombre, 'Manual') AS medioPago, " +
            "  f.nro_factura AS factura, " +
            "  p.descripcion AS periodo, " +
            "  a.id_alumno AS legajo, " +
            "  CONCAT(a.apellido, ', ', a.nombre) AS nombre, " +
            "  f.fecha_pago AS fecha, " +
            "  f.importe_pagado AS pagado " +
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "LEFT JOIN tipopago tp ON f.tipo_id = tp.tipo_id " +
            "INNER JOIN cursos c ON UPPER(TRIM(a.curso)) = UPPER(TRIM(c.descripcion)) " +
            "INNER JOIN conf_establecimiento e ON c.id_establecimiento = e.id_establecimiento " +
            "WHERE f.fecha_pago BETWEEN :desde AND :hasta " +
            "  AND f.id_estado = 1 " +
            "  AND f.importe_pagado > 0 " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN e.nombre LIKE 'Jardin%' THEN 1 " +
            "    WHEN e.nombre LIKE 'Colegio%' THEN 2 " +
            "  END ASC, " +
            "  CASE " +
            "    WHEN IFNULL(tp.nombre, 'Manual') = 'Manual' THEN 1 " +
            "    WHEN IFNULL(tp.nombre, 'Manual') = 'PagoFácil' THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, " +
            "  f.fecha_pago ASC, a.apellido ASC", nativeQuery = true)
    List<Map<String, Object>> findRecaudacionPorFechas(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query(value = "SELECT " +
            "  a.id_alumno AS idAlumno, " +
            "  a.id_alumno AS legajo, " +
            "  a.nro_documento AS dni, " +
            "  CONCAT(a.apellido, ', ', a.nombre) AS alumno, " +
            "  f.nro_factura AS factura, " +
            "  p.descripcion AS periodo, " +
            "  f.pri_venc AS vencimiento, " +
            "  f.importe_adeudado AS importe " +
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "WHERE f.id_estado = 2 " + // 2 = Factura NO pagada
            "  AND f.importe_adeudado > 0 " +
            "ORDER BY a.apellido ASC, a.nombre ASC, f.pri_venc ASC", nativeQuery = true)
    List<DeudaGeneralProjection> findDeudaGeneralCompleta();

    @Query(value = "SELECT " +
            "  f.id_facturas AS facturaId, " +
            "  f.nro_factura AS nroFactura, " +
            "  f.pri_venc AS primerVencimiento, " +
            "  f.importe_adeudado AS importeAdeudado, " +
            "  CONCAT(a.apellido, ', ', a.nombre) AS nombreAlumno " + // 🌟 Agregamos el nombre aquí
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "WHERE af.id_alumno = :alumnoId " +
            "  AND p.descripcion = :periodoNombre " +
            "  AND f.id_estado = 2 " + // 2 = No pagada
            "LIMIT 1", nativeQuery = true)
    Optional<Map<String, Object>> findFacturaConAlumnoPorPeriodo(
            @Param("alumnoId") Long alumnoId,
            @Param("periodoNombre") String periodoNombre
    );
}