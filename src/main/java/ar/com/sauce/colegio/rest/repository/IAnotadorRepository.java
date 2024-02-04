/**
 * 
 */
package ar.com.sauce.colegio.rest.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.sauce.colegio.rest.model.Anotador;

/**
 * @author daniel
 *
 */
public interface IAnotadorRepository extends JpaRepository<Anotador, Long> {
	public List<Anotador> findAllByAlumnoId(Long alumnoId, Sort sort);
}
