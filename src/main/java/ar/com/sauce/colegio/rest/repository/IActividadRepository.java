package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IActividadRepository extends JpaRepository<Actividad, Long> {}