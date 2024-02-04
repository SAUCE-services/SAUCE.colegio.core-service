/**
 * 
 */
package ar.com.sauce.colegio.rest.service.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.com.sauce.colegio.rest.model.view.FacturaParaInteres;
import ar.com.sauce.colegio.rest.repository.view.IFacturaParaInteresRepository;

/**
 * @author daniel
 *
 */
@Service
public class FacturaParaInteresService {
	@Autowired
	private IFacturaParaInteresRepository repository;

	public List<FacturaParaInteres> findAllByAlumnoId(Long alumnoId) {
		return repository.findAllByAlumnoId(alumnoId);
	}
}
