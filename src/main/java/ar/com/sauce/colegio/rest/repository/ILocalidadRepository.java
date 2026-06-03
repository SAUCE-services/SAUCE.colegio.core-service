package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Localidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ILocalidadRepository extends JpaRepository<Localidad, Long> {}