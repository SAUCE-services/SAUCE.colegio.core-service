package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Periodo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IPeriodoRepository extends JpaRepository<Periodo, Long> {

    @Query("SELECT p FROM Periodo p WHERE " +
            "(:mes IS NULL OR p.mes = :mes) AND " +
            "(:fechaSegundo IS NULL OR p.fechaSegundo = :fechaSegundo) AND " +
            "(:cicloNombre IS NULL OR p.ciclo.nombre LIKE %:cicloNombre%)")
    Page<Periodo> buscarConFiltros(
            @Param("mes") LocalDate mes,
            @Param("fechaSegundo") LocalDate fechaSegundo,
            @Param("cicloNombre") String cicloNombre,
            Pageable pageable);
}