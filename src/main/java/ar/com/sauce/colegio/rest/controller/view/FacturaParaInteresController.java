/**
 * 
 */
package ar.com.sauce.colegio.rest.controller.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.sauce.colegio.rest.model.view.FacturaParaInteres;
import ar.com.sauce.colegio.rest.service.view.FacturaParaInteresService;

/**
 * @author daniel
 *
 */
@RestController
@RequestMapping("/facturaparainteres")
public class FacturaParaInteresController {
	@Autowired
	private FacturaParaInteresService service;
	
	@GetMapping("/alumno/{alumnoId}")
	public ResponseEntity<List<FacturaParaInteres>> findAllByAlumnoId(@PathVariable Long alumnoId) {
		return new ResponseEntity<List<FacturaParaInteres>>(service.findAllByAlumnoId(alumnoId), HttpStatus.OK);
	}
}
