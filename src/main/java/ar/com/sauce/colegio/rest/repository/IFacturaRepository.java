package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IFacturaRepository extends JpaRepository<Factura, Long> {

    // ✅ Usamos una consulta nativa para unir las tablas según la imagen
    @Query(value = "SELECT f.* FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "WHERE af.id_alumno = :alumnoId " +
            "ORDER BY f.fecha_estado DESC", nativeQuery = true)
    List<Factura> findByAlumnoId(@Param("alumnoId") Long alumnoId);


    @Query(value = "SELECT f.*, f.created as fecha_registro FROM facturas f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "WHERE af.id_alumno = :alumnoId", nativeQuery = true)
    List<java.util.Map<String, Object>> findByAlumnoIdNative(@Param("alumnoId") Long alumnoId);
}