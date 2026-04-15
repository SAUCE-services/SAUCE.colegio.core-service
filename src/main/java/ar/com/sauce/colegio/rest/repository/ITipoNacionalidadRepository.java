package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.TipoNacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITipoNacionalidadRepository extends JpaRepository<TipoNacionalidad, Long> {

}