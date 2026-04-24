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
    @Query(value = "SELECT IFNULL(c.descripcion, 'Sin Asignar') as descripcion, ac.importe as importe " +
            "FROM alumnos_conceptos ac " +
            "LEFT JOIN conceptos c ON ac.id_concepto = c.id_concepto " +
            "WHERE ac.id_facturas = :facturaId", nativeQuery = true)
    List<ConceptoDetalleProjection> findByFacturaId(@Param("facturaId") Long facturaId);
}