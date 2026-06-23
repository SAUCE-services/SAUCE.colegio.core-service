package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Ciclo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ICicloRepository extends JpaRepository<Ciclo, Long> {
    @Query("SELECT COALESCE(MAX(c.cicloId), 0) + 1 FROM Ciclo c")
    Long obtenerProximoId();

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO ciclo (ciclo_id, auto_id, nombre, desde, hasta, uuid, created, updated) " +
            "VALUES (:cicloId, :autoId, :nombre, :desde, :hasta, :uuid, NOW(), NOW())", nativeQuery = true)
    void insertarCicloNativo(@Param("cicloId") Long cicloId,
                             @Param("autoId") Long autoId,
                             @Param("nombre") String nombre,
                             @Param("desde") java.time.LocalDate desde,
                             @Param("hasta") java.time.LocalDate hasta,
                             @Param("uuid") String uuid);
}