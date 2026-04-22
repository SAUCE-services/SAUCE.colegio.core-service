package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Concepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IConceptoRepository extends JpaRepository<Concepto, Long> {
    @Query(value = "SELECT c.* FROM conceptos c " +
            "JOIN alumnos_conceptos ac ON c.id_concepto = ac.id_concepto " +
            "WHERE ac.id_facturas = :facturaId", nativeQuery = true)
    List<Concepto> findByFacturaId(@Param("facturaId") Long facturaId);
}