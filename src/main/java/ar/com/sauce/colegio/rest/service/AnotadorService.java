/**
 * 
 */
package ar.com.sauce.colegio.rest.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ar.com.sauce.colegio.rest.model.Anotador;
import ar.com.sauce.colegio.rest.repository.IAnotadorRepository;

/**
 * @author daniel
 *
 */
@Service
public class AnotadorService {
	@Autowired
	private IAnotadorRepository repository;

	public List<Anotador> findAll() {
		return repository.findAll();
	}

	public List<Anotador> findAllByAlumnoId(Long alumnoId) {
		return repository.findAllByAlumnoId(alumnoId, Sort.by("anotadorId").descending());
	}

	public Anotador add(Anotador anotador) {
		// se pone Denver para compensar la hora
		anotador.setFecha(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Denver"))));
		repository.save(anotador);
		return anotador;
	}
}
