package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
import ar.com.sauce.colegio.rest.repository.projection.DeudaIndividualProjection;
import ar.com.sauce.colegio.rest.repository.projection.NovedadCursoProjection;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IConceptoRepository extends JpaRepository<Concepto, Long> {
    @Query(value = "SELECT " +
            "  COALESCE(c.descripcion, 'Sin Asignar') AS descripcion, " +
            "  ac.importe AS importe, " +
            "  CAST(ac.fecha_registro AS DATE) AS fecha_registro " + // 👈 Usamos tu columna real con alias snake_case
            "FROM factura f " +
            "INNER JOIN alumnos_conceptos ac ON f.id_facturas = ac.id_facturas " +
            "LEFT JOIN conceptos c ON ac.id_concepto = c.id_concepto " +
            "WHERE f.nro_factura = :nroFactura", nativeQuery = true)
    List<ConceptoDetalleProjection> findByNroFactura(@Param("nroFactura") Long nroFactura);

    @Query(value = "SELECT " +
            "  ac.fecha_estado AS fechaEstado, " +
            "  COALESCE(c.descripcion, 'Sin Asignar') AS concepto, " +
            "  CASE WHEN ac.id_estado = 4 THEN 'Concepto FACTURADO' ELSE te.descripcion END AS estado, " +
            "  ac.importe AS importe, " +
            "  CAST(ac.fecha_registro AS DATE) AS fechaRegistro, " +
            "  p.descripcion AS periodo " +
            "FROM alumnos_conceptos ac " +
            "INNER JOIN conceptos c ON ac.id_concepto = c.id_concepto " +
            "INNER JOIN factura f ON ac.id_facturas = f.id_facturas " +
            "LEFT JOIN tipos_estado te ON ac.id_estado = te.id_estado " +
            "LEFT JOIN periodos p ON ac.id_periodo = p.id_periodo " +
            "WHERE ac.id_alumno = :alumnoId " +
            "  AND f.id_estado = 2", nativeQuery = true) // 👈 FILTRO CRÍTICO: Solo lo vinculado a facturas NO pagadas
    List<DeudaIndividualProjection> findDeudaIndividualByAlumnoId(@Param("alumnoId") Long alumnoId);

    @Query(value = "SELECT " +
            "  ac.fecha_registro AS fechaEstado, " +
            "  c.descripcion AS concepto, " +
            "  te.descripcion AS estado, " +
            "  ac.importe AS importe, " +
            "  ac.fecha_registro AS fechaRegistro, " +
            "  p.descripcion AS periodo " +
            "FROM alumnos_conceptos ac " +
            "INNER JOIN conceptos c ON ac.id_concepto = c.id_concepto " +
            "INNER JOIN periodos p ON ac.id_periodo = p.id_periodo " +
            "INNER JOIN tipos_estado te ON ac.id_estado = te.id_estado " + // 👈 Corregido a id_estado
            "WHERE ac.id_alumno = :alumnoId " +
            "  AND UPPER(TRIM(p.descripcion)) LIKE UPPER(CONCAT('%', :periodoNombre, '%')) " + // 👈 Filtro por Nombre String
            "ORDER BY ac.fecha_registro DESC", nativeQuery = true)
    List<Object[]> findNovedadesByAlumnoYPeriodoNombre(
            @Param("alumnoId") Long alumnoId,
            @Param("periodoNombre") String periodoNombre);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO alumnos_conceptos " +
            "  (id_alumno, id_periodo, id_concepto, importe, id_estado, fecha_estado, fecha_registro) " +
            "VALUES " +
            "  (:alumnoId, :periodoId, :conceptoId, :importe, 4, CAST(CURRENT_DATE AS DATE), CAST(CURRENT_DATE AS DATE))",
            nativeQuery = true)
    void registrarNovedadManual(
            @Param("alumnoId") Long alumnoId,
            @Param("periodoId") Long periodoId,
            @Param("conceptoId") Long conceptoId,
            @Param("importe") BigDecimal importe);

    @Query(value = "SELECT " +
            "  ac.id_alumno AS legajo, " +
            "  CONCAT(a.apellido, ', ', a.nombre) AS alumno, " +
            "  cur.descripcion AS cursoNombre, " + // 🌟 Enviamos el nombre del curso al Front
            "  c.descripcion AS concepto, " +
            "  ac.importe AS importe, " +
            "  te.descripcion AS estado, " +
            "  ac.fecha_registro AS fechaRegistro " +
            "FROM alumnos_conceptos ac " +
            "INNER JOIN alumnos a ON ac.id_alumno = a.id_alumno " +
            "INNER JOIN conceptos c ON ac.id_concepto = c.id_concepto " +
            "INNER JOIN periodos p ON ac.id_periodo = p.id_periodo " +
            "INNER JOIN tipos_estado te ON ac.id_estado = te.id_estado " +
            "INNER JOIN cursos cur ON UPPER(TRIM(a.curso)) = UPPER(TRIM(cur.descripcion)) " +
            "INNER JOIN ciclo cic ON cur.ciclo_id = cic.ciclo_id " + // 🌟 JOIN con ciclos
            "WHERE cur.id_cursos = :cursoId " +
            "  AND p.descripcion = :periodoNombre " +
            "  AND cic.nombre = :cicloNombre", nativeQuery = true) // 🌟 Parámetro de Ciclo
    List<NovedadCursoProjection> findNovedadesPorCursoYPeriodo(
            @Param("cursoId") Long cursoId,
            @Param("periodoNombre") String periodoNombre,
            @Param("cicloNombre") String cicloNombre
    );
}
