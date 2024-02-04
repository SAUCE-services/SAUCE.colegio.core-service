/**
 * 
 */
package ar.com.sauce.colegio.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.sauce.colegio.rest.model.Anotador;
import ar.com.sauce.colegio.rest.service.AnotadorService;

/**
 * @author daniel
 *
 */
@RestController
@RequestMapping("/anotador")
public class AnotadorController {
	@Autowired
	private AnotadorService service;
	
	@GetMapping("/")
	public ResponseEntity<List<Anotador>> findAll() {
		return new ResponseEntity<List<Anotador>>(service.findAll(), HttpStatus.OK);
	}

	@GetMapping("/alumno/{alumnoId}")
	public ResponseEntity<List<Anotador>> findAllByAlumnoId(@PathVariable Long alumnoId) {
		return new ResponseEntity<List<Anotador>>(service.findAllByAlumnoId(alumnoId), HttpStatus.OK);
	}
	
	@PostMapping("/")
	public ResponseEntity<Anotador> add(@RequestBody Anotador anotador) {
		return new ResponseEntity<Anotador>(service.add(anotador), HttpStatus.OK);
	}
	@GetMapping("/add")
	public ResponseEntity<Anotador> addByGet(@RequestParam Long alumnoId, @RequestParam String anotacion, @RequestParam Long transaccionId) {
		return add(new Anotador(alumnoId, anotacion, transaccionId));
	}
}
