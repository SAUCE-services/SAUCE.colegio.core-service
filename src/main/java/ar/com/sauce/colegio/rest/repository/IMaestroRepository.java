package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Maestro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMaestroRepository extends JpaRepository<Maestro, Long> {
}