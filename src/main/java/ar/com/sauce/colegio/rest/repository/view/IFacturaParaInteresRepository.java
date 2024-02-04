/**
 * 
 */
package ar.com.sauce.colegio.rest.repository.view;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.sauce.colegio.rest.model.view.FacturaParaInteres;
import ar.com.sauce.colegio.rest.model.view.pk.FacturaParaInteresPk;

/**
 * @author daniel
 *
 */
@Repository
public interface IFacturaParaInteresRepository extends JpaRepository<FacturaParaInteres, FacturaParaInteresPk> {
	public List<FacturaParaInteres> findAllByAlumnoId(Long alumnoId);
}
