package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Periodo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IPeriodoRepository extends JpaRepository<Periodo, Long> {
    Page<Periodo> findAllByMes(LocalDate mes, Pageable pageable);
    Page<Periodo> findAllByFechaSegundo(LocalDate fechaSegundo, Pageable pageable);
    Page<Periodo> findAllByMesAndFechaSegundo(LocalDate mes, LocalDate fechaSegundo, Pageable pageable);
}