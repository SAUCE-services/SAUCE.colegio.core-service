package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
import ar.com.sauce.colegio.rest.repository.projection.DeudaIndividualProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
