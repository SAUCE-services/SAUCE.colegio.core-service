package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
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
}