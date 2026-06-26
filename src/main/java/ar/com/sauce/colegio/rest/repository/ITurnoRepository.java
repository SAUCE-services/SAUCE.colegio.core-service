package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITurnoRepository extends JpaRepository<Turno, Long> {
}