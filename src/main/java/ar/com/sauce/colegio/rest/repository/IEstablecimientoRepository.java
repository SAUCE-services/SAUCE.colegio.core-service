package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Establecimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEstablecimientoRepository extends JpaRepository<Establecimiento,Long> {
}
